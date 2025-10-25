-- Add time_rate_per_min_override to trip_fares for time component overrides (MySQL)
alter table trip_fares
  add column if not exists time_rate_per_min_override decimal(8,2);
