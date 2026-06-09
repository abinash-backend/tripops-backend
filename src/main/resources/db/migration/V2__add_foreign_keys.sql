ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_user_id
    FOREIGN KEY (user_id)
    REFERENCES users(id);

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_package_id
    FOREIGN KEY (package_id)
    REFERENCES travel_packages(id);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_booking_id
    FOREIGN KEY (booking_id)
    REFERENCES bookings(id);
