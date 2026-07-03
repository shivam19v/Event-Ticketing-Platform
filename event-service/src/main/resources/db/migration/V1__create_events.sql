CREATE TABLE events (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organizer_id UUID NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    category     VARCHAR(50),
    image_url    VARCHAR(500),
    status       VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    start_time   TIMESTAMP NOT NULL,
    end_time     TIMESTAMP NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_events_status      ON events(status);
CREATE INDEX idx_events_start_time  ON events(start_time);
CREATE INDEX idx_events_organizer   ON events(organizer_id);
CREATE INDEX idx_events_category    ON events(category);
CREATE INDEX idx_events_title_trgm  ON events USING gin(title gin_trgm_ops);

CREATE TABLE venues (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id   UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(500),
    city       VARCHAR(100),
    state      VARCHAR(100),
    country    VARCHAR(100),
    latitude   DECIMAL(10,8),
    longitude  DECIMAL(11,8),
    capacity   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_venues_event_id ON venues(event_id);
CREATE INDEX idx_venues_city     ON venues(city);

CREATE TABLE ticket_types (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id        UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    quantity        INT NOT NULL,
    sold            INT NOT NULL DEFAULT 0,
    sale_start_time TIMESTAMP,
    sale_end_time   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id, name)
);
CREATE INDEX idx_ticket_types_event ON ticket_types(event_id);

CREATE TABLE seat_maps (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id       UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    ticket_type_id UUID REFERENCES ticket_types(id),
    section_name   VARCHAR(100) NOT NULL,
    row_label      VARCHAR(10),
    seat_number    INT,
    status         VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id, section_name, row_label, seat_number)
);
CREATE INDEX idx_seat_maps_event_status ON seat_maps(event_id, status);
CREATE INDEX idx_seat_maps_event        ON seat_maps(event_id);
