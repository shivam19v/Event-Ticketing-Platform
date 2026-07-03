package repository

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	_ "github.com/lib/pq"
)

func NewDB(dsn string) (*sql.DB, error) {
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open db: %w", err)
	}

	db.SetMaxOpenConns(20)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(30 * time.Minute)

	// Retry connection (postgres container may still be starting)
	var pingErr error
	for i := 0; i < 10; i++ {
		pingErr = db.Ping()
		if pingErr == nil {
			break
		}
		log.Printf("waiting for database... (%d/10): %v", i+1, pingErr)
		time.Sleep(3 * time.Second)
	}
	if pingErr != nil {
		return nil, fmt.Errorf("could not connect to db: %w", pingErr)
	}

	if err := runMigrations(db); err != nil {
		return nil, fmt.Errorf("migration failed: %w", err)
	}

	return db, nil
}

func runMigrations(db *sql.DB) error {
	schema := `
	CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

	CREATE TABLE IF NOT EXISTS payments (
		id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
		booking_id           UUID NOT NULL,
		user_id              UUID NOT NULL,
		amount               DECIMAL(10,2) NOT NULL,
		currency             VARCHAR(3) NOT NULL DEFAULT 'USD',
		status               VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
		payment_method       VARCHAR(50) NOT NULL DEFAULT 'stripe',
		external_payment_id  VARCHAR(255) UNIQUE,
		idempotency_key      VARCHAR(255) UNIQUE NOT NULL,
		created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		processed_at         TIMESTAMP
	);
	CREATE INDEX IF NOT EXISTS idx_payments_status     ON payments(status);
	CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments(booking_id);
	CREATE INDEX IF NOT EXISTS idx_payments_idempotency ON payments(idempotency_key);

	CREATE TABLE IF NOT EXISTS refunds (
		id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
		payment_id          UUID NOT NULL REFERENCES payments(id),
		amount              DECIMAL(10,2) NOT NULL,
		reason              VARCHAR(255),
		status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
		external_refund_id  VARCHAR(255),
		created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		processed_at        TIMESTAMP
	);
	CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);

	CREATE TABLE IF NOT EXISTS processed_webhook_events (
		stripe_event_id  VARCHAR(255) PRIMARY KEY,
		processed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err := db.Exec(schema)
	return err
}
