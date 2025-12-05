package com.ridefast.ride_fast_backend.service.cashback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridefast.ride_fast_backend.dto.cashback.*;
import com.ridefast.ride_fast_backend.enums.CashbackStatus;
import com.ridefast.ride_fast_backend.model.cashback.CashbackEntry;
import com.ridefast.ride_fast_backend.model.cashback.CashbackSettings;
import com.ridefast.ride_fast_backend.repository.cashback.CashbackEntryRepository;
import com.ridefast.ride_fast_backend.repository.cashback.CashbackSettingsRepository;
import com.ridefast.ride_fast_backend.service.notification.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashbackService {

    private final CashbackSettingsRepository settingsRepository;
    private final CashbackEntryRepository entryRepository;
    private final PushNotificationService notificationService;
    private final ObjectMapper objectMapper;

    // ==================== SETTINGS METHODS ====================

    /**
     * Get current cashback settings
     */
    public CashbackSettings getSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * Get settings as DTO
     */
    public CashbackSettingsDTO getSettingsDTO() {
        CashbackSettings settings = getSettings();
        return toSettingsDTO(settings);
    }

    /**
     * Update cashback settings (Admin)
     */
    @Transactional
    public CashbackSettingsDTO updateSettings(CashbackSettingsDTO dto) {
        CashbackSettings settings = getSettings();
        
        if (dto.getIsEnabled() != null) settings.setIsEnabled(dto.getIsEnabled());
        if (dto.getCashbackPercentage() != null) settings.setCashbackPercentage(dto.getCashbackPercentage());
        if (dto.getUtilisationLimit() != null) settings.setUtilisationLimit(dto.getUtilisationLimit());
        if (dto.getValidityHours() != null) settings.setValidityHours(dto.getValidityHours());
        if (dto.getMaxCreditsPerDay() != null) settings.setMaxCreditsPerDay(dto.getMaxCreditsPerDay());
        if (dto.getFestivalExtraPercentage() != null) settings.setFestivalExtraPercentage(dto.getFestivalExtraPercentage());
        if (dto.getFestivalStartDate() != null) settings.setFestivalStartDate(dto.getFestivalStartDate());
        if (dto.getFestivalEndDate() != null) settings.setFestivalEndDate(dto.getFestivalEndDate());
        
        if (dto.getEnabledCategories() != null) {
            try {
                settings.setEnabledCategories(objectMapper.writeValueAsString(dto.getEnabledCategories()));
            } catch (Exception e) {
                log.error("Failed to serialize categories", e);
            }
        }
        
        settings = settingsRepository.save(settings);
        return toSettingsDTO(settings);
    }

    // ==================== CREDIT METHODS ====================

    /**
     * Credit cashback after ride completion
     */
    @Transactional
    public CashbackCreditResponse creditCashback(String userId, Long rideId, BigDecimal rideFare, String rideCategory) {
        CashbackSettings settings = getSettings();
        
        // Check if cashback is enabled
        if (!settings.getIsEnabled()) {
            return CashbackCreditResponse.builder()
                    .success(false)
                    .message("Cashback is currently disabled")
                    .build();
        }
        
        // Check if category is enabled
        if (!isCategoryEnabled(settings, rideCategory)) {
            return CashbackCreditResponse.builder()
                    .success(false)
                    .message("Cashback not applicable for this ride category")
                    .build();
        }
        
        // Check if already credited for this ride
        if (entryRepository.existsByRideId(rideId)) {
            return CashbackCreditResponse.builder()
                    .success(false)
                    .message("Cashback already credited for this ride")
                    .build();
        }
        
        // Check daily limit
        if (settings.getMaxCreditsPerDay() > 0) {
            LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
            int creditsToday = entryRepository.countCreditsToday(userId, startOfDay);
            if (creditsToday >= settings.getMaxCreditsPerDay()) {
                return CashbackCreditResponse.builder()
                        .success(false)
                        .message("Daily cashback limit reached")
                        .build();
            }
        }
        
        // Calculate cashback amount
        BigDecimal percentage = settings.getEffectivePercentage();
        BigDecimal cashbackAmount = rideFare.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        // Create cashback entry
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(settings.getValidityHours());
        
        CashbackEntry entry = CashbackEntry.builder()
                .userId(userId)
                .rideId(rideId)
                .rideCategory(rideCategory)
                .rideFare(rideFare)
                .percentageApplied(percentage)
                .amount(cashbackAmount)
                .amountRemaining(cashbackAmount)
                .status(CashbackStatus.ACTIVE)
                .createdAt(now)
                .expiresAt(expiresAt)
                .isFestivalBonus(settings.isFestivalActive())
                .build();
        
        entry = entryRepository.save(entry);
        
        log.info("Cashback credited: userId={}, rideId={}, amount={}, expiresAt={}", 
                userId, rideId, cashbackAmount, expiresAt);
        
        // Build response with popup message
        BigDecimal utilisationLimit = settings.getUtilisationLimit();
        String popupMessage = String.format(
                "Use ₹%.0f on your next trip within %d hours.",
                utilisationLimit, settings.getValidityHours()
        );
        
        return CashbackCreditResponse.builder()
                .success(true)
                .message("Cashback credited successfully")
                .entryId(entry.getId())
                .amountCredited(cashbackAmount)
                .percentageApplied(percentage)
                .expiresAt(expiresAt)
                .popupTitle(String.format("You earned ₹%.0f Cashback!", cashbackAmount))
                .popupMessage(popupMessage)
                .utilisationLimit(utilisationLimit)
                .utilisationMessage(popupMessage)
                .build();
    }

    // ==================== USAGE METHODS ====================

    /**
     * Use cashback on a ride (FIFO - oldest first)
     */
    @Transactional
    public CashbackUsageResponse useCashback(String userId, CashbackUsageRequest request) {
        if (!request.getUseCashback()) {
            return CashbackUsageResponse.builder()
                    .success(true)
                    .message("Cashback not applied")
                    .originalFare(request.getFareAmount())
                    .cashbackUsed(BigDecimal.ZERO)
                    .finalPayable(request.getFareAmount())
                    .remainingBalance(getActiveBalance(userId))
                    .build();
        }
        
        CashbackSettings settings = getSettings();
        BigDecimal utilisationLimit = settings.getUtilisationLimit();
        
        // Get active entries (FIFO)
        List<CashbackEntry> activeEntries = entryRepository.findActiveEntriesByUser(userId, LocalDateTime.now());
        
        if (activeEntries.isEmpty()) {
            return CashbackUsageResponse.builder()
                    .success(false)
                    .message("No active cashback available")
                    .originalFare(request.getFareAmount())
                    .cashbackUsed(BigDecimal.ZERO)
                    .finalPayable(request.getFareAmount())
                    .remainingBalance(BigDecimal.ZERO)
                    .build();
        }
        
        // Calculate total available
        BigDecimal totalAvailable = activeEntries.stream()
                .map(CashbackEntry::getAmountRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAvailable.compareTo(utilisationLimit) < 0) {
            return CashbackUsageResponse.builder()
                    .success(false)
                    .message("Insufficient cashback balance")
                    .originalFare(request.getFareAmount())
                    .cashbackUsed(BigDecimal.ZERO)
                    .finalPayable(request.getFareAmount())
                    .remainingBalance(totalAvailable)
                    .build();
        }
        
        // Use cashback (FIFO) - limited to utilisationLimit
        BigDecimal amountToUse = utilisationLimit;
        BigDecimal totalUsed = BigDecimal.ZERO;
        
        for (CashbackEntry entry : activeEntries) {
            if (amountToUse.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal used = entry.use(amountToUse);
            totalUsed = totalUsed.add(used);
            amountToUse = amountToUse.subtract(used);
            
            // Update entry status if partially used
            if (entry.getAmountRemaining().compareTo(BigDecimal.ZERO) > 0 && 
                entry.getAmountUsed().compareTo(BigDecimal.ZERO) > 0) {
                entry.setStatus(CashbackStatus.PARTIALLY_USED);
            }
            
            entryRepository.save(entry);
        }
        
        BigDecimal finalPayable = request.getFareAmount().subtract(totalUsed);
        BigDecimal remainingBalance = getActiveBalance(userId);
        
        log.info("Cashback used: userId={}, rideId={}, used={}, remaining={}", 
                userId, request.getRideId(), totalUsed, remainingBalance);
        
        return CashbackUsageResponse.builder()
                .success(true)
                .message("Cashback applied successfully")
                .originalFare(request.getFareAmount())
                .cashbackUsed(totalUsed)
                .finalPayable(finalPayable)
                .remainingBalance(remainingBalance)
                .build();
    }

    /**
     * Check if user can use cashback
     */
    public boolean canUseCashback(String userId) {
        CashbackSettings settings = getSettings();
        if (!settings.getIsEnabled()) return false;
        
        BigDecimal balance = getActiveBalance(userId);
        return balance.compareTo(settings.getUtilisationLimit()) >= 0;
    }

    // ==================== WALLET METHODS ====================

    /**
     * Get user's cashback wallet
     */
    public CashbackWalletDTO getUserWallet(String userId) {
        CashbackSettings settings = getSettings();
        List<CashbackEntry> activeEntries = entryRepository.findActiveEntriesByUser(userId, LocalDateTime.now());
        
        BigDecimal totalBalance = activeEntries.stream()
                .map(CashbackEntry::getAmountRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<CashbackEntryDTO> entryDTOs = activeEntries.stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
        
        // Find soonest expiry
        Long soonestExpiry = null;
        String soonestExpiryFormatted = null;
        if (!activeEntries.isEmpty()) {
            LocalDateTime soonest = activeEntries.stream()
                    .map(CashbackEntry::getExpiresAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            if (soonest != null) {
                Duration duration = Duration.between(LocalDateTime.now(), soonest);
                soonestExpiry = duration.getSeconds();
                soonestExpiryFormatted = formatDuration(duration);
            }
        }
        
        return CashbackWalletDTO.builder()
                .totalBalance(totalBalance)
                .utilisationLimit(settings.getUtilisationLimit())
                .canUseCashback(totalBalance.compareTo(settings.getUtilisationLimit()) >= 0)
                .activeEntries(entryDTOs)
                .activeCount(activeEntries.size())
                .soonestExpirySeconds(soonestExpiry)
                .soonestExpiryFormatted(soonestExpiryFormatted)
                .build();
    }

    /**
     * Get cashback history by status
     */
    public List<CashbackEntryDTO> getCashbackHistory(String userId, CashbackStatus status) {
        List<CashbackEntry> entries;
        if (status != null) {
            entries = entryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            entries = entryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return entries.stream().map(this::toEntryDTO).collect(Collectors.toList());
    }

    /**
     * Get active balance for user
     */
    public BigDecimal getActiveBalance(String userId) {
        return entryRepository.getTotalActiveBalance(userId, LocalDateTime.now());
    }

    // ==================== EXPIRY PROCESSING ====================

    /**
     * Process expired cashback entries (scheduled job)
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void processExpiredEntries() {
        List<CashbackEntry> expiredEntries = entryRepository.findExpiredEntries(LocalDateTime.now());
        
        for (CashbackEntry entry : expiredEntries) {
            entry.markExpired();
            entryRepository.save(entry);
            
            log.info("Cashback expired: entryId={}, userId={}, amount={}", 
                    entry.getId(), entry.getUserId(), entry.getAmountRemaining());
            
            // Send expiry notification
            // notificationService.sendCashbackExpiredNotification(entry.getUserId(), entry.getAmountRemaining());
        }
        
        if (!expiredEntries.isEmpty()) {
            log.info("Processed {} expired cashback entries", expiredEntries.size());
        }
    }

    /**
     * Send reminders for expiring cashback (12 hours and 1 hour)
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void sendExpiryReminders() {
        LocalDateTime now = LocalDateTime.now();
        
        // 12-hour reminder
        List<CashbackEntry> expiring12h = entryRepository.findEntriesExpiringSoon(
                now.plusHours(11).plusMinutes(50),
                now.plusHours(12).plusMinutes(10)
        );
        for (CashbackEntry entry : expiring12h) {
            log.info("Sending 12-hour reminder for cashback: userId={}", entry.getUserId());
            // notificationService.sendCashbackReminderNotification(entry.getUserId(), 12);
        }
        
        // 1-hour reminder
        List<CashbackEntry> expiring1h = entryRepository.findEntriesExpiringSoon(
                now.plusMinutes(50),
                now.plusMinutes(70)
        );
        for (CashbackEntry entry : expiring1h) {
            log.info("Sending 1-hour reminder for cashback: userId={}", entry.getUserId());
            // notificationService.sendCashbackReminderNotification(entry.getUserId(), 1);
        }
    }

    // ==================== HELPER METHODS ====================

    private CashbackSettings createDefaultSettings() {
        CashbackSettings settings = CashbackSettings.builder()
                .isEnabled(false)
                .cashbackPercentage(new BigDecimal("10.00"))
                .utilisationLimit(new BigDecimal("15.00"))
                .validityHours(24)
                .maxCreditsPerDay(1)
                .enabledCategories("[\"CAR_PREMIUM_EXPRESS\",\"CAR_SHARE_POOLING\",\"AUTO_SHARE_POOLING\",\"TATA_MAGIC_SHARE_POOLING\"]")
                .build();
        return settingsRepository.save(settings);
    }

    private boolean isCategoryEnabled(CashbackSettings settings, String category) {
        if (settings.getEnabledCategories() == null) return false;
        try {
            List<String> categories = objectMapper.readValue(
                    settings.getEnabledCategories(), 
                    new TypeReference<List<String>>() {}
            );
            return categories.contains(category);
        } catch (Exception e) {
            log.error("Failed to parse enabled categories", e);
            return false;
        }
    }

    private CashbackSettingsDTO toSettingsDTO(CashbackSettings settings) {
        List<String> categories = new ArrayList<>();
        try {
            if (settings.getEnabledCategories() != null) {
                categories = objectMapper.readValue(
                        settings.getEnabledCategories(),
                        new TypeReference<List<String>>() {}
                );
            }
        } catch (Exception e) {
            log.error("Failed to parse categories", e);
        }
        
        return CashbackSettingsDTO.builder()
                .id(settings.getId())
                .isEnabled(settings.getIsEnabled())
                .cashbackPercentage(settings.getCashbackPercentage())
                .utilisationLimit(settings.getUtilisationLimit())
                .validityHours(settings.getValidityHours())
                .maxCreditsPerDay(settings.getMaxCreditsPerDay())
                .festivalExtraPercentage(settings.getFestivalExtraPercentage())
                .festivalStartDate(settings.getFestivalStartDate())
                .festivalEndDate(settings.getFestivalEndDate())
                .isFestivalActive(settings.isFestivalActive())
                .effectivePercentage(settings.getEffectivePercentage())
                .enabledCategories(categories)
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private CashbackEntryDTO toEntryDTO(CashbackEntry entry) {
        Duration duration = Duration.between(LocalDateTime.now(), entry.getExpiresAt());
        long seconds = Math.max(0, duration.getSeconds());
        
        return CashbackEntryDTO.builder()
                .id(entry.getId())
                .rideCategory(entry.getRideCategory())
                .rideFare(entry.getRideFare())
                .percentageApplied(entry.getPercentageApplied())
                .amount(entry.getAmount())
                .amountUsed(entry.getAmountUsed())
                .amountRemaining(entry.getAmountRemaining())
                .status(entry.getStatus())
                .createdAt(entry.getCreatedAt())
                .expiresAt(entry.getExpiresAt())
                .usedAt(entry.getUsedAt())
                .expiredAt(entry.getExpiredAt())
                .isFestivalBonus(entry.getIsFestivalBonus())
                .expiresInSeconds(seconds)
                .expiresInFormatted(formatDuration(duration))
                .isExpiringSoon(seconds < 3600)
                .build();
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}

