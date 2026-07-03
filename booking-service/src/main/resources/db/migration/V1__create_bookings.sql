CREATE TABLE reservations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    event_id        UUID NOT NULL,
    ticket_type_id  UUID NOT NULL,
    quantity        INT NOT NULL,
    seat_ids        TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_price     DECIMAL(10,2) NOT NULL,
    reserved_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP NOT NULL,
    confirmed_at    TIMESTAMP,
    cancelled_at    TIMESTAMP
);
CREATE INDEX idx_reservations_user_event   ON reservations(user_id, event_id);
CREATE INDEX idx_reservations_status_exp   ON reservations(status, expires_at);
CREATE INDEX idx_reservations_event_status ON reservations(event_id, status);

CREATE TABLE bookings (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reservation_id   UUID NOT NULL REFERENCES reservations(id),
    user_id          UUID NOT NULL,
    event_id         UUID NOT NULL,
    ticket_type_id   UUID NOT NULL,
    quantity         INT NOT NULL,
    total_price      DECIMAL(10,2) NOT NULL,
    booking_status   VARCHAR(50) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    payment_id       UUID,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP
);
CREATE INDEX idx_bookings_user_status   ON bookings(user_id, booking_status);
CREATE INDEX idx_bookings_event_status  ON bookings(event_id, booking_status);
CREATE INDEX idx_bookings_reservation   ON bookings(reservation_id);
