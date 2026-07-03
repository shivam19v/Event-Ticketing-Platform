package repository

import (
	"context"
	"database/sql"
	"errors"
	"time"

	"github.com/eventsphere/payment-service/internal/model"
)

var ErrNotFound = errors.New("payment not found")

type PaymentRepository struct {
	db *sql.DB
}

func NewPaymentRepository(db *sql.DB) *PaymentRepository {
	return &PaymentRepository{db: db}
}

func (r *PaymentRepository) FindByIdempotencyKey(ctx context.Context, key string) (*model.Payment, error) {
	p := &model.Payment{}
	err := r.db.QueryRowContext(ctx,
		`SELECT id, booking_id, user_id, amount, currency, status, payment_method,
		        COALESCE(external_payment_id, ''), idempotency_key, created_at, processed_at
		 FROM payments WHERE idempotency_key = $1`, key,
	).Scan(&p.ID, &p.BookingID, &p.UserID, &p.Amount, &p.Currency, &p.Status,
		&p.PaymentMethod, &p.ExternalPaymentID, &p.IdempotencyKey, &p.CreatedAt, &p.ProcessedAt)

	if err == sql.ErrNoRows {
		return nil, ErrNotFound
	}
	return p, err
}

func (r *PaymentRepository) Create(ctx context.Context, p *model.Payment) error {
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO payments (id, booking_id, user_id, amount, currency, status, payment_method, external_payment_id, idempotency_key, created_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
		p.ID, p.BookingID, p.UserID, p.Amount, p.Currency, p.Status, p.PaymentMethod,
		p.ExternalPaymentID, p.IdempotencyKey, p.CreatedAt,
	)
	return err
}

func (r *PaymentRepository) FindByID(ctx context.Context, id string) (*model.Payment, error) {
	p := &model.Payment{}
	err := r.db.QueryRowContext(ctx,
		`SELECT id, booking_id, user_id, amount, currency, status, payment_method,
		        COALESCE(external_payment_id,''), idempotency_key, created_at, processed_at
		 FROM payments WHERE id = $1`, id,
	).Scan(&p.ID, &p.BookingID, &p.UserID, &p.Amount, &p.Currency, &p.Status,
		&p.PaymentMethod, &p.ExternalPaymentID, &p.IdempotencyKey, &p.CreatedAt, &p.ProcessedAt)
	if err == sql.ErrNoRows {
		return nil, ErrNotFound
	}
	return p, err
}

func (r *PaymentRepository) FindByExternalID(ctx context.Context, externalID string) (*model.Payment, error) {
	p := &model.Payment{}
	err := r.db.QueryRowContext(ctx,
		`SELECT id, booking_id, user_id, amount, currency, status, payment_method,
		        COALESCE(external_payment_id,''), idempotency_key, created_at, processed_at
		 FROM payments WHERE external_payment_id = $1`, externalID,
	).Scan(&p.ID, &p.BookingID, &p.UserID, &p.Amount, &p.Currency, &p.Status,
		&p.PaymentMethod, &p.ExternalPaymentID, &p.IdempotencyKey, &p.CreatedAt, &p.ProcessedAt)
	if err == sql.ErrNoRows {
		return nil, ErrNotFound
	}
	return p, err
}

func (r *PaymentRepository) UpdateStatus(ctx context.Context, id, status string, processedAt *time.Time) error {
	_, err := r.db.ExecContext(ctx,
		`UPDATE payments SET status = $1, processed_at = $2 WHERE id = $3`,
		status, processedAt, id)
	return err
}

func (r *PaymentRepository) UpdateExternalID(ctx context.Context, id, externalID string) error {
	_, err := r.db.ExecContext(ctx,
		`UPDATE payments SET external_payment_id = $1 WHERE id = $2`, externalID, id)
	return err
}

// Webhook idempotency: returns true if event was already processed
func (r *PaymentRepository) IsWebhookEventProcessed(ctx context.Context, stripeEventID string) (bool, error) {
	var exists bool
	err := r.db.QueryRowContext(ctx,
		`SELECT EXISTS(SELECT 1 FROM processed_webhook_events WHERE stripe_event_id = $1)`,
		stripeEventID).Scan(&exists)
	return exists, err
}

func (r *PaymentRepository) MarkWebhookEventProcessed(ctx context.Context, stripeEventID string) error {
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO processed_webhook_events (stripe_event_id) VALUES ($1) ON CONFLICT DO NOTHING`,
		stripeEventID)
	return err
}

func (r *PaymentRepository) CreateRefund(ctx context.Context, ref *model.Refund) error {
	_, err := r.db.ExecContext(ctx,
		`INSERT INTO refunds (id, payment_id, amount, reason, status, external_refund_id, created_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7)`,
		ref.ID, ref.PaymentID, ref.Amount, ref.Reason, ref.Status, ref.ExternalRefundID, ref.CreatedAt)
	return err
}
