-- Fix intercity_bookings table: Increase column sizes for enum fields
-- Run this SQL script on your database to fix the "Data truncated" error

-- Fix status column (longest value: "IN_PROGRESS" = 11 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Fix booking_type column (longest value: "SHARE_POOL" = 10 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
MODIFY COLUMN booking_type VARCHAR(20) NOT NULL;

-- Fix payment_status column (longest value: "COMPLETED" = 9 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
MODIFY COLUMN payment_status VARCHAR(20) NOT NULL;

-- Fix payment_method column (longest value: "ONLINE" = 6 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
MODIFY COLUMN payment_method VARCHAR(20);

-- Verify the changes
DESCRIBE intercity_bookings;

