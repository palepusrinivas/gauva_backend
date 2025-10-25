-- Flyway migration: initial schema for vehicles, categories, brands, models, zones, settings, trip_fares

-- vehicle_categories
create table if not exists vehicle_categories (
    id char(36) primary key,
    name varchar(255) unique not null,
    description text not null,
    image varchar(255) not null,
    type varchar(255) not null,
    is_active tinyint(1) default 0,
    deleted_at timestamp,
    created_at timestamp,
    updated_at timestamp
);

-- vehicle_brands
create table if not exists vehicle_brands (
    id char(36) primary key,
    name varchar(255) unique not null,
    is_active tinyint(1) default 0,
    deleted_at timestamp,
    created_at timestamp,
    updated_at timestamp
);

-- vehicle_models
create table if not exists vehicle_models (
    id char(36) primary key,
    brand_id char(36) not null,
    name varchar(255) not null,
    is_active tinyint(1) default 0,
    deleted_at timestamp,
    created_at timestamp,
    updated_at timestamp,
    constraint fk_vehicle_models_brand
      foreign key (brand_id) references vehicle_brands(id)
);

-- zones
create table if not exists zones (
    id char(36) primary key,
    name varchar(255) unique not null,
    coordinates POLYGON,
    is_active tinyint(1) default 1,
    deleted_at timestamp,
    created_at timestamp,
    updated_at timestamp
);

-- settings
create table if not exists settings (
    id char(36) primary key,
    key_name varchar(191),
    live_values text,
    test_values text,
    settings_type varchar(255),
    mode varchar(20) default 'live',
    is_active tinyint(1) default 1,
    created_at timestamp,
    updated_at timestamp
);

-- trip_fares
create table if not exists trip_fares (
    id char(36) primary key,
    zone_id char(36) not null,
    vehicle_category_id char(36) not null,
    base_fare decimal(8,2) not null,
    base_fare_per_km decimal(8,2) not null,
    waiting_fee_per_min decimal(8,2) not null,
    cancellation_fee_percent decimal(8,2) not null,
    min_cancellation_fee decimal(8,2) not null,
    idle_fee_per_min decimal(8,2) not null,
    trip_delay_fee_per_min decimal(8,2) not null,
    penalty_fee_for_cancel decimal(8,2) not null,
    fee_add_to_next decimal(8,2) not null,
    created_at timestamp,
    updated_at timestamp,
    constraint fk_trip_fares_zone
      foreign key (zone_id) references zones(id),
    constraint fk_trip_fares_vehicle_category
      foreign key (vehicle_category_id) references vehicle_categories(id)
);

-- vehicles
create table if not exists vehicles (
    id char(36) primary key,
    ref_id varchar(20) not null,
    brand_id char(36) not null,
    model_id char(36) not null,
    category_id char(36) not null,
    licence_plate_number varchar(255) not null,
    licence_expire_date date not null,
    vin_number varchar(255) not null,
    transmission varchar(255) not null,
    fuel_type varchar(255) not null,
    ownership varchar(255) not null,
    driver_id char(36) not null,
    documents TEXT,
    is_active tinyint(1) default 0,
    deleted_at timestamp,
    created_at timestamp,
    updated_at timestamp,
    constraint fk_vehicles_brand
      foreign key (brand_id) references vehicle_brands(id),
    constraint fk_vehicles_model
      foreign key (model_id) references vehicle_models(id),
    constraint fk_vehicles_category
      foreign key (category_id) references vehicle_categories(id),
    constraint fk_vehicles_driver
      foreign key (driver_id) references users(id)
);
