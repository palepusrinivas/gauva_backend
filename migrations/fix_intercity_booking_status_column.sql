-- Fix intercity_bookings table: Increase column sizes for enum fields
-- PostgreSQL version
-- Run this SQL script on your database to fix the "Data truncated" error

-- Fix status column (longest value: "IN_PROGRESS" = 11 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE intercity_bookings 
ALTER COLUMN status SET NOT NULL;

-- Fix booking_type column (longest value: "SHARE_POOL" = 10 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
ALTER COLUMN booking_type TYPE VARCHAR(20);

ALTER TABLE intercity_bookings 
ALTER COLUMN booking_type SET NOT NULL;

-- Fix payment_status column (longest value: "COMPLETED" = 9 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
ALTER COLUMN payment_status TYPE VARCHAR(20);

ALTER TABLE intercity_bookings 
ALTER COLUMN payment_status SET NOT NULL;

-- Fix payment_method column (longest value: "ONLINE" = 6 chars, using 20 for safety)
ALTER TABLE intercity_bookings 
ALTER COLUMN payment_method TYPE VARCHAR(20);

-- Verify the changes (PostgreSQL)
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'intercity_bookings'
AND column_name IN ('status', 'booking_type', 'payment_status', 'payment_method');

