package com.ridefast.ride_fast_backend.service.driveraccess.impl;

import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.Wallet;
import com.ridefast.ride_fast_backend.model.driveraccess.DriverDailyActivity;
import com.ridefast.ride_fast_backend.model.driveraccess.DriverFeeConfiguration;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.WalletRepository;
import com.ridefast.ride_fast_backend.repository.driveraccess.DriverDailyActivityRepository;
import com.ridefast.ride_fast_backend.repository.driveraccess.DriverFeeConfigurationRepository;
import com.ridefast.ride_fast_backend.service.driveraccess.DriverAccessRulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverAccessRulesServiceImpl implements DriverAccessRulesService {

    private final DriverRepository driverRepository;
    private final DriverFeeConfigurationRepository configRepository;
    private final DriverDailyActivityRepository activityRepository;
    private final WalletRepository walletRepository;

    @Override
    public Map<String, Object> getFeeConfigurations() {
        List<DriverFeeConfiguration> all = configRepository.findAll();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", all);
        res.put("message_en", "Every Day is Free Access — If You Earn More!");
        res.put("message_te", "ప్రతి రోజు ఫ్రీ యాక్సెస్ – మీరు సంపాదిస్తే!");
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> todayStatus(Long driverId, String vehicleType) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElseThrow();
        LocalDate today = LocalDate.now();
        DriverDailyActivity a = activityRepository.findByDriverAndActivityDate(d, today).orElseGet(() -> {
            DriverDailyActivity na = DriverDailyActivity.builder()
                    .driver(d)
                    .activityDate(today)
                    .vehicleType(vehicleType)
                    .targetTrips(cfg.getDailyTargetTrips())
                    .dailyFee(cfg.getDailyFee())
                    .daysSinceJoining(0)
                    .isWelcomePeriod(Boolean.TRUE)
                    .build();
            return activityRepository.save(na);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("date", today.toString());
        data.put("vehicle_type", vehicleType);
        data.put("days_since_joining", a.getDaysSinceJoining());
        data.put("is_welcome_period", a.getIsWelcomePeriod());
        data.put("completed_trips", a.getCompletedTrips());
        data.put("counted_trips", a.getCompletedTrips() + a.getCustomerCancelledAfterStart());
        data.put("target_trips", a.getTargetTrips());
        int tripsRemaining = Math.max(0, a.getTargetTrips() - (a.getCompletedTrips() + a.getCustomerCancelledAfterStart()));
        data.put("trips_remaining", tripsRemaining);
        boolean free = (a.getCompletedTrips() + a.getCustomerCancelledAfterStart()) >= a.getTargetTrips();
        data.put("free_access_achieved", free);
        data.put("daily_fee", a.getDailyFee());
        data.put("status", free ? "free" : "in_progress");
        data.put("message_en", free ? "Congratulations! Free access achieved today!" : tripsRemaining + " more trips needed for free access");
        data.put("message_te", free ? "అభినందనలు! ఈ రోజు ఫ్రీ యాక్సెస్ పొందారు" : "ఫ్రీ యాక్సెస్ కోసం మరో " + tripsRemaining + " ట్రిప్స్ అవసరం");

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", data);
        return res;
    }

    @Override
    public Map<String, Object> canAcceptTrips(Long driverId, String vehicleType) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElseThrow();
        Optional<Wallet> w = walletRepository.findByOwnerTypeAndOwnerId(WalletOwnerType.DRIVER, String.valueOf(driverId));
        BigDecimal balance = w.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        boolean ok = balance.compareTo(cfg.getMinimumWalletBalance()) >= 0;
        Map<String, Object> data = new HashMap<>();
        data.put("can_accept", ok);
        if (!ok) {
            BigDecimal needed = cfg.getMinimumWalletBalance().subtract(balance).max(BigDecimal.ZERO);
            data.put("reason", "Insufficient wallet balance. Minimum required: \u20B9" + cfg.getMinimumWalletBalance());
            data.put("reason_te", "వాలెట్ బ్యాలెన్స్ తక్కువగా ఉంది. కనీసం \u20B9" + cfg.getMinimumWalletBalance() + " అవసరం");
            data.put("current_balance", balance);
            data.put("required_balance", cfg.getMinimumWalletBalance());
            data.put("top_up_needed", needed);
        } else {
            data.put("reason", "Driver can accept trips");
            data.put("reason_te", "డ్రైవర్ ట్రిప్స్ అంగీకరించవచ్చు");
            data.put("current_balance", balance);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", data);
        return res;
    }

    @Override
    @Transactional
    public void recordTripCompleted(Long driverId, Long rideId, String vehicleType) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElseThrow();
        LocalDate today = LocalDate.now();
        DriverDailyActivity a = activityRepository.findByDriverAndActivityDate(d, today)
                .orElseGet(() -> activityRepository.save(DriverDailyActivity.builder()
                        .driver(d)
                        .activityDate(today)
                        .vehicleType(vehicleType)
                        .targetTrips(cfg.getDailyTargetTrips())
                        .dailyFee(cfg.getDailyFee())
                        .isWelcomePeriod(Boolean.FALSE)
                        .build()));
        a.setCompletedTrips(a.getCompletedTrips() + 1);
        activityRepository.save(a);
    }

    @Override
    @Transactional
    public void recordCustomerCancelledAfterStart(Long driverId, Long rideId, String vehicleType) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElseThrow();
        LocalDate today = LocalDate.now();
        DriverDailyActivity a = activityRepository.findByDriverAndActivityDate(d, today)
                .orElseGet(() -> activityRepository.save(DriverDailyActivity.builder()
                        .driver(d)
                        .activityDate(today)
                        .vehicleType(vehicleType)
                        .targetTrips(cfg.getDailyTargetTrips())
                        .dailyFee(cfg.getDailyFee())
                        .isWelcomePeriod(Boolean.FALSE)
                        .build()));
        a.setCustomerCancelledAfterStart(a.getCustomerCancelledAfterStart() + 1);
        activityRepository.save(a);
    }

    @Override
    @Transactional
    public void recordDriverCancellation(Long driverId, Long rideId, String vehicleType) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElseThrow();
        LocalDate today = LocalDate.now();
        DriverDailyActivity a = activityRepository.findByDriverAndActivityDate(d, today)
                .orElseGet(() -> activityRepository.save(DriverDailyActivity.builder()
                        .driver(d)
                        .activityDate(today)
                        .vehicleType(vehicleType)
                        .targetTrips(cfg.getDailyTargetTrips())
                        .dailyFee(cfg.getDailyFee())
                        .isWelcomePeriod(Boolean.FALSE)
                        .build()));
        a.setDriverCancelled(a.getDriverCancelled() + 1);
        activityRepository.save(a);
    }

    @Override
    public Map<String, Object> statistics(Long driverId, String start, String end) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        List<DriverDailyActivity> list = activityRepository.findByDriverAndActivityDateBetweenOrderByActivityDateAsc(d, s, e);
        int totalCompleted = list.stream().mapToInt(DriverDailyActivity::getCompletedTrips).sum();
        int days = (int) (e.toEpochDay() - s.toEpochDay() + 1);
        Map<String, Object> data = new HashMap<>();
        data.put("period", Map.of("start", start, "end", end, "days", days));
        data.put("trips", Map.of("total_completed", totalCompleted, "average_per_day", days > 0 ? (double) totalCompleted / days : 0.0));
        long freeDays = list.stream().filter(DriverDailyActivity::getFreeAccessAchieved).count();
        long paidDays = list.stream().filter(DriverDailyActivity::getFeeDeducted).count();
        long welcomeDays = list.stream().filter(DriverDailyActivity::getIsWelcomePeriod).count();
        data.put("access", Map.of("free_days", freeDays, "paid_days", paidDays, "welcome_days", welcomeDays));
        BigDecimal totalDeducted = list.stream().map(a -> a.getFeeAmountDeducted() == null ? BigDecimal.ZERO : a.getFeeAmountDeducted())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("fees", Map.of("total_deducted", totalDeducted));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("data", data);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> processDailyFees(LocalDate date) {
        List<DriverDailyActivity> list = activityRepository.findAll();
        int feesDeducted = 0;
        for (DriverDailyActivity a : list) {
            if (a.getActivityDate() == null || !a.getActivityDate().equals(date)) continue;
            int counted = a.getCompletedTrips() + a.getCustomerCancelledAfterStart();
            if (Boolean.TRUE.equals(a.getIsWelcomePeriod())) continue;
            if (counted == 0) continue;
            DriverFeeConfiguration cfg = configRepository.findByVehicleTypeIgnoreCase(a.getVehicleType()).orElse(null);
            if (cfg == null) continue;
            if (counted >= cfg.getDailyTargetTrips()) {
                a.setFreeAccessAchieved(true);
                activityRepository.save(a);
                continue;
            }
            if (Boolean.TRUE.equals(a.getFeeDeducted())) continue;
            a.setFeeDeducted(true);
            a.setFeeAmountDeducted(cfg.getDailyFee());
            a.setFeeDeductedAt(java.time.LocalDateTime.now());
            activityRepository.save(a);
            feesDeducted++;
        }
        return Map.of("success", true, "fees_deducted", feesDeducted);
    }
}


