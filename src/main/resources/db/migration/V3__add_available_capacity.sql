ALTER TABLE travel_packages
    ADD COLUMN available_capacity INTEGER;

UPDATE travel_packages
SET available_capacity = capacity
WHERE available_capacity IS NULL;

ALTER TABLE travel_packages
    ALTER COLUMN available_capacity SET NOT NULL;

ALTER TABLE travel_packages
    ADD CONSTRAINT ck_travel_packages_available_capacity_non_negative
    CHECK (available_capacity >= 0);

ALTER TABLE travel_packages
    ADD CONSTRAINT ck_travel_packages_available_capacity_max
    CHECK (available_capacity <= capacity);
