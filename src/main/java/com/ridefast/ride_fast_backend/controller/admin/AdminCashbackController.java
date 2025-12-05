package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.dto.cashback.CashbackEntryDTO;
import com.ridefast.ride_fast_backend.dto.cashback.CashbackSettingsDTO;
import com.ridefast.ride_fast_backend.enums.CashbackStatus;
import com.ridefast.ride_fast_backend.model.cashback.CashbackEntry;
import com.ridefast.ride_fast_backend.repository.cashback.CashbackEntryRepository;
import com.ridefast.ride_fast_backend.service.cashback.CashbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for managing cashback settings and viewing statistics
 */
@RestController
@RequestMapping("/api/admin/cashback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminCashbackController {

    private final CashbackService cashbackService;
    private final CashbackEntryRepository entryRepository;

    // ==================== SETTINGS ENDPOINTS ====================

    /**
     * Get current cashback settings
     */
    @GetMapping("/settings")
    public ResponseEntity<CashbackSettingsDTO> getSettings() {
        return ResponseEntity.ok(cashbackService.getSettingsDTO());
    }

    /**
     * Update cashback settings
     */
    @PutMapping("/settings")
    public ResponseEntity<CashbackSettingsDTO> updateSettings(@RequestBody CashbackSettingsDTO dto) {
        log.info("Updating cashback settings: enabled={}, percentage={}, limit={}", 
                dto.getIsEnabled(), dto.getCashbackPercentage(), dto.getUtilisationLimit());
        return ResponseEntity.ok(cashbackService.updateSettings(dto));
    }

    /**
     * Toggle cashback ON/OFF
     */
    @PatchMapping("/settings/toggle")
    public ResponseEntity<CashbackSettingsDTO> toggleCashback(@RequestParam Boolean enabled) {
        CashbackSettingsDTO dto = new CashbackSettingsDTO();
        dto.setIsEnabled(enabled);
        log.info("Toggling cashback: enabled={}", enabled);
        return ResponseEntity.ok(cashbackService.updateSettings(dto));
    }

    /**
     * Update festival mode
     */
    @PutMapping("/settings/festival")
    public ResponseEntity<CashbackSettingsDTO> updateFestivalMode(
            @RequestParam BigDecimal extraPercentage,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        CashbackSettingsDTO dto = new CashbackSettingsDTO();
        dto.setFestivalExtraPercentage(extraPercentage);
        dto.setFestivalStartDate(LocalDateTime.parse(startDate));
        dto.setFestivalEndDate(LocalDateTime.parse(endDate));
        log.info("Updating festival mode: extra={}%, start={}, end={}", extraPercentage, startDate, endDate);
        return ResponseEntity.ok(cashbackService.updateSettings(dto));
    }

    /**
     * Clear festival mode
     */
    @DeleteMapping("/settings/festival")
    public ResponseEntity<CashbackSettingsDTO> clearFestivalMode() {
        CashbackSettingsDTO dto = new CashbackSettingsDTO();
        dto.setFestivalExtraPercentage(BigDecimal.ZERO);
        dto.setFestivalStartDate(null);
        dto.setFestivalEndDate(null);
        log.info("Clearing festival mode");
        return ResponseEntity.ok(cashbackService.updateSettings(dto));
    }

    // ==================== DASHBOARD ENDPOINTS ====================

    /**
     * Get cashback dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Settings summary
        CashbackSettingsDTO settings = cashbackService.getSettingsDTO();
        dashboard.put("settings", settings);
        
        // Stats by status
        List<Object[]> statsByStatus = entryRepository.getCashbackStatsByStatus();
        Map<String, Map<String, Object>> statusStats = new HashMap<>();
        for (Object[] row : statsByStatus) {
            CashbackStatus status = (CashbackStatus) row[0];
            Long count = (Long) row[1];
            BigDecimal total = (BigDecimal) row[2];
            statusStats.put(status.name(), Map.of("count", count, "total", total));
        }
        dashboard.put("statsByStatus", statusStats);
        
        // Today's stats
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        BigDecimal creditedToday = entryRepository.getTotalCashbackCredited(startOfDay, endOfDay);
        BigDecimal usedToday = entryRepository.getTotalCashbackUsed(startOfDay, endOfDay);
        dashboard.put("creditedToday", creditedToday);
        dashboard.put("usedToday", usedToday);
        
        // This month's stats
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
        BigDecimal creditedThisMonth = entryRepository.getTotalCashbackCredited(startOfMonth, endOfMonth);
        BigDecimal usedThisMonth = entryRepository.getTotalCashbackUsed(startOfMonth, endOfMonth);
        dashboard.put("creditedThisMonth", creditedThisMonth);
        dashboard.put("usedThisMonth", usedThisMonth);
        
        return ResponseEntity.ok(dashboard);
    }

    // ==================== ENTRY MANAGEMENT ====================

    /**
     * Get all cashback entries with pagination
     */
    @GetMapping("/entries")
    public ResponseEntity<Page<CashbackEntry>> getAllEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CashbackStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<CashbackEntry> entries;
        if (status != null) {
            entries = entryRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            entries = entryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        return ResponseEntity.ok(entries);
    }

    /**
     * Get cashback entries for a specific user
     */
    @GetMapping("/entries/user/{userId}")
    public ResponseEntity<List<CashbackEntryDTO>> getUserEntries(
            @PathVariable String userId,
            @RequestParam(required = false) CashbackStatus status) {
        return ResponseEntity.ok(cashbackService.getCashbackHistory(userId, status));
    }

    /**
     * Manually expire a cashback entry
     */
    @PostMapping("/entries/{entryId}/expire")
    public ResponseEntity<Map<String, Object>> expireEntry(@PathVariable Long entryId) {
        return entryRepository.findById(entryId)
                .map(entry -> {
                    entry.markExpired();
                    entryRepository.save(entry);
                    log.info("Manually expired cashback entry: id={}", entryId);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "success", true,
                            "message", "Entry expired successfully"
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Process expired entries manually
     */
    @PostMapping("/process-expired")
    public ResponseEntity<Map<String, Object>> processExpired() {
        cashbackService.processExpiredEntries();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Expired entries processed"
        ));
    }
}

