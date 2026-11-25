-- Monitoring database (schema is created by Hibernate at first start).
-- Regenerate this file with real data after running the stack:  ./docs/export-db-dumps.sh
CREATE TABLE device_refs (
    device_id uuid PRIMARY KEY,
    user_id uuid,
    name varchar(255),
    max_consumption double precision NOT NULL
);

CREATE TABLE measurements (
    id uuid PRIMARY KEY,
    device_id uuid NOT NULL,
    measured_at timestamp with time zone NOT NULL,
    value_kwh double precision NOT NULL,
    CONSTRAINT uk_measurement UNIQUE (device_id, measured_at)
);

CREATE TABLE hourly_consumption (
    id uuid PRIMARY KEY,
    device_id uuid NOT NULL,
    hour_start timestamp with time zone NOT NULL,
    total_kwh double precision NOT NULL,
    CONSTRAINT uk_hourly UNIQUE (device_id, hour_start)
);
