CREATE TABLE sent_notifications (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID,
    template     VARCHAR(100) NOT NULL,
    channel      VARCHAR(50) NOT NULL DEFAULT 'email',
    recipient    VARCHAR(255) NOT NULL,
    subject      VARCHAR(255),
    status       VARCHAR(50) NOT NULL DEFAULT 'SENT',
    error_message TEXT,
    sent_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user   ON sent_notifications(user_id);
CREATE INDEX idx_notifications_status ON sent_notifications(status);

CREATE TABLE processed_events (
    event_key    VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
