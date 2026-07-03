CREATE TABLE tickets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id      UUID NOT NULL,
    event_id        UUID NOT NULL,
    user_id         UUID NOT NULL,
    ticket_number   VARCHAR(50) UNIQUE NOT NULL,
    qr_code_data    TEXT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'VALID',
    issued_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    first_used_at   TIMESTAMP,
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX idx_tickets_event_user    ON tickets(event_id, user_id);
CREATE INDEX idx_tickets_booking       ON tickets(booking_id);

CREATE TABLE ticket_scans (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_id    UUID NOT NULL REFERENCES tickets(id),
    event_id     UUID NOT NULL,
    scanned_by   UUID,
    scanned_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location     VARCHAR(255),
    device_id    VARCHAR(100),
    result       VARCHAR(50) NOT NULL DEFAULT 'SUCCESS'
);
CREATE INDEX idx_ticket_scans_ticket ON ticket_scans(ticket_id, scanned_at);

-- Idempotency for event consumption (avoid double-processing booking.confirmed)
CREATE TABLE processed_events (
    event_key     VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
