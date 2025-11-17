# File Verification for CI/CD Build

## Critical Files Required for Build

These files must be committed to fix the compilation errors:

### ✅ Service Files (Must Exist)
1. `src/main/java/com/ridefast/ride_fast_backend/service/school/BusActivationService.java`
2. `src/main/java/com/ridefast/ride_fast_backend/service/school/ParentRequestService.java`
3. `src/main/java/com/ridefast/ride_fast_backend/service/school/BusAlertService.java`
4. `src/main/java/com/ridefast/ride_fast_backend/service/school/BusAlertScheduler.java`

### ✅ Repository Files (Must Exist)
1. `src/main/java/com/ridefast/ride_fast_backend/repository/school/TrackingPingRepository.java`
2. `src/main/java/com/ridefast/ride_fast_backend/repository/school/AlertLogRepository.java`
3. `src/main/java/com/ridefast/ride_fast_backend/repository/school/ParentRequestRepository.java`

### ✅ Model Files (Must Exist)
1. `src/main/java/com/ridefast/ride_fast_backend/model/school/ParentRequest.java`

### ✅ Controller Files (Must Exist)
1. `src/main/java/com/ridefast/ride_fast_backend/controller/school/ParentRequestController.java`
2. `src/main/java/com/ridefast/ride_fast_backend/controller/admin/AdminSchoolController.java`

## Quick Fix Command

Run this in the repository root:

```bash
# Verify files exist
ls -la ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/service/school/BusActivationService.java
ls -la ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/repository/school/TrackingPingRepository.java

# Add all new files
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/service/school/BusActivationService.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/service/school/ParentRequestService.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/service/school/BusAlertService.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/service/school/BusAlertScheduler.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/repository/school/TrackingPingRepository.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/repository/school/AlertLogRepository.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/repository/school/ParentRequestRepository.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/model/school/ParentRequest.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/controller/school/ParentRequestController.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/controller/admin/AdminSchoolController.java

# Add modified files
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/controller/school/BusController.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/controller/school/SchoolTrackingController.java
git add ride_fast_backend/src/main/java/com/ridefast/ride_fast_backend/controller/school/StudentController.java

# Commit
git commit -m "Fix: Add missing BusActivationService and TrackingPingRepository for Guava bus tracking"

# Push
git push
```

