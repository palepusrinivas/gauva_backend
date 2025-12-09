-- Fix PostgreSQL boolean column types
-- MySQL uses BIT/TINYINT for booleans, PostgreSQL uses BOOLEAN
-- Run this script after migrating from MySQL to PostgreSQL

-- Fix zone table
-- Convert 'active' column from bit to boolean
ALTER TABLE zone 
ALTER COLUMN active TYPE BOOLEAN USING CASE WHEN active::int = 1 THEN TRUE ELSE FALSE END;

-- If the column doesn't exist yet, it will be created as BOOLEAN by Hibernate
-- If it exists as bit, the above will convert it

-- Fix other common boolean columns (add more as needed)
-- Check and fix is_active columns (if they exist as bit)
DO $$
BEGIN
    -- Fix zone.is_active if it exists as bit
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'zone' 
        AND column_name = 'is_active' 
        AND data_type = 'bit'
    ) THEN
        ALTER TABLE zone 
        ALTER COLUMN is_active TYPE BOOLEAN USING CASE WHEN is_active::int = 1 THEN TRUE ELSE FALSE END;
    END IF;
END $$;

-- Fix coordinates column type (polygon -> TEXT for PostgreSQL)
-- PostgreSQL polygon type is different from MySQL
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'zone' 
        AND column_name = 'coordinates' 
        AND udt_name != 'text'
    ) THEN
        ALTER TABLE zone 
        ALTER COLUMN coordinates TYPE TEXT;
    END IF;
END $$;

-- Verify changes
SELECT column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_name = 'zone'
AND column_name IN ('active', 'is_active', 'coordinates');

