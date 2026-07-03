package service

import (
	"context"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/eventsphere/payment-service/internal/model"
	"github.com/eventsphere/payment-service/internal/repository"
	"github.com/google/uuid"
	"github.com/stripe/stripe-go/v76"
	"github.com/stripe/stripe-go/v76/paymentintent"
	"github.com/stripe/stripe-go/v76/refund"
	"github.com/stripe/stripe-go/v76/webhook"
)

var (
	ErrInvalidAmount = errors.New("amount must be greater than zero")
)

type PaymentService struct {
	repo            *repository.PaymentRepository
	publisher       *EventPublisher
	webhookSecret   string
	useStripeStub   bool // when true (no real Stripe key), simulate success locally
}

func NewPaymentService(repo *repository.PaymentRepository, publisher *EventPublisher, stripeSecretKey, webhookSecret string) *PaymentService {
	useStub := stripeSecretKey == "" || stripeSecretKey == "sk_test_placeholder"
	if !useStub {
		stripe.Key = stripeSecretKey
	} else {
		log.Println("[PaymentService] No real Stripe key configured — running in STUB mode (payments auto-succeed)")
	}
	return &PaymentService{repo: repo, publisher: publisher, webhookSecret: webhookSecret, useStripeStub: useStub}
}

// InitiatePayment is fully idempotent: calling it twice with the same
// idempotencyKey returns the exact same payment record, never double-charges.
func (s *PaymentService) InitiatePayment(ctx context.Context, userID string, req model.InitiatePaymentRequest) (*model.InitiatePaymentResponse, error) {
	if req.Amount <= 0 {
		return nil, ErrInvalidAmount
	}
	if req.IdempotencyKey == "" {
		req.IdempotencyKey = uuid.New().String()
	}

	// 1. Idempotency check — if we've seen this key before, return existing result
	existing, err := s.repo.FindByIdempotencyKey(ctx, req.IdempotencyKey)
	if err == nil {
		log.Printf("[PaymentService] idempotent replay for key=%s -> payment=%s", req.IdempotencyKey, existing.ID)
		return &model.InitiatePaymentResponse{
			PaymentID: existing.ID,
			Status:    existing.Status,
		}, nil
	}
	if err != repository.ErrNotFound {
		return nil, fmt.Errorf("db error checking idempotency: %w", err)
	}

	// 2. Create the payment record first (status=INITIATED)
	payment := &model.Payment{
		ID:             uuid.New().String(),
		BookingID:      req.BookingID,
		UserID:         userID,
		Amount:         req.Amount,
		Currency:       "USD",
		Status:         "INITIATED",
		PaymentMethod:  "stripe",
		IdempotencyKey: req.IdempotencyKey,
		CreatedAt:      time.Now(),
	}

	var clientSecret, redirectURL string

	if s.useStripeStub {
		// Stub mode: simulate an external payment intent ID
		payment.ExternalPaymentID = "pi_stub_" + payment.ID[:8]
		clientSecret = "stub_secret_" + payment.ID[:8]
	} else {
		params := &stripe.PaymentIntentParams{
			Amount:   stripe.Int64(int64(req.Amount * 100)),
			Currency: stripe.String("usd"),
			Metadata: map[string]string{
				"booking_id":      req.BookingID,
				"idempotency_key": req.IdempotencyKey,
				"user_id":         userID,
			},
		}
		params.IdempotencyKey = stripe.String(req.IdempotencyKey)

		intent, err := paymentintent.New(params)
		if err != nil {
			return nil, fmt.Errorf("stripe error: %w", err)
		}
		payment.ExternalPaymentID = intent.ID
		clientSecret = intent.ClientSecret
	}

	if err := s.repo.Create(ctx, payment); err != nil {
		return nil, fmt.Errorf("failed to save payment: %w", err)
	}

	s.publisher.Publish("payment.initiated", map[string]interface{}{
		"paymentId": payment.ID,
		"bookingId": payment.BookingID,
		"amount":    payment.Amount,
	})

	// In stub mode, auto-complete the payment after creation (simulates webhook)
	if s.useStripeStub {
		go s.simulateAsyncSuccess(payment.ID)
	}

	return &model.InitiatePaymentResponse{
		PaymentID:    payment.ID,
		Status:       payment.Status,
		ClientSecret: clientSecret,
		RedirectURL:  redirectURL,
	}, nil
}

// simulateAsyncSuccess mimics what a real Stripe webhook would do, for local/dev testing
// without real Stripe credentials.
func (s *PaymentService) simulateAsyncSuccess(paymentID string) {
	time.Sleep(2 * time.Second)
	ctx := context.Background()
	p, err := s.repo.FindByID(ctx, paymentID)
	if err != nil {
		log.Printf("[stub webhook] could not find payment %s: %v", paymentID, err)
		return
	}
	now := time.Now()
	if err := s.repo.UpdateStatus(ctx, p.ID, "SUCCESS", &now); err != nil {
		log.Printf("[stub webhook] failed to update payment %s: %v", paymentID, err)
		return
	}
	log.Printf("[stub webhook] payment %s marked SUCCESS (simulated)", p.ID)
	s.publisher.Publish("payment.completed", map[string]interface{}{
		"paymentId": p.ID,
		"bookingId": p.BookingID,
		"amount":    p.Amount,
		"timestamp": now.Unix(),
	})
}

// HandleStripeWebhook verifies the signature and processes payment_intent events.
// Idempotent: the same Stripe event ID will never be processed twice.
func (s *PaymentService) HandleStripeWebhook(ctx context.Context, payload []byte, signature string) error {
	event, err := webhook.ConstructEvent(payload, signature, s.webhookSecret)
	if err != nil {
		return fmt.Errorf("webhook signature verification failed: %w", err)
	}

	already, err := s.repo.IsWebhookEventProcessed(ctx, event.ID)
	if err != nil {
		return fmt.Errorf("db error: %w", err)
	}
	if already {
		log.Printf("[Webhook] event %s already processed, skipping (idempotent)", event.ID)
		return nil
	}

	switch event.Type {
	case "payment_intent.succeeded":
		var intent stripe.PaymentIntent
		if err := event.Data.UnmarshalJSON(stripeRaw(event)); err != nil {
			return err
		}
		if err := s.handlePaymentSuccess(ctx, intent.ID); err != nil {
			return err
		}
	case "payment_intent.payment_failed":
		var intent stripe.PaymentIntent
		if err := event.Data.UnmarshalJSON(stripeRaw(event)); err != nil {
			return err
		}
		if err := s.handlePaymentFailure(ctx, intent.ID); err != nil {
			return err
		}
	default:
		log.Printf("[Webhook] unhandled event type: %s", event.Type)
	}

	return s.repo.MarkWebhookEventProcessed(ctx, event.ID)
}

func stripeRaw(event stripe.Event) []byte { return event.Data.Raw }

func (s *PaymentService) handlePaymentSuccess(ctx context.Context, externalID string) error {
	payment, err := s.repo.FindByExternalID(ctx, externalID)
	if err != nil {
		return err
	}
	if payment.Status == "SUCCESS" {
		return nil // already success, idempotent no-op
	}
	now := time.Now()
	if err := s.repo.UpdateStatus(ctx, payment.ID, "SUCCESS", &now); err != nil {
		return err
	}
	s.publisher.Publish("payment.completed", map[string]interface{}{
		"paymentId": payment.ID,
		"bookingId": payment.BookingID,
		"amount":    payment.Amount,
		"timestamp": now.Unix(),
	})
	return nil
}

func (s *PaymentService) handlePaymentFailure(ctx context.Context, externalID string) error {
	payment, err := s.repo.FindByExternalID(ctx, externalID)
	if err != nil {
		return err
	}
	now := time.Now()
	if err := s.repo.UpdateStatus(ctx, payment.ID, "FAILED", &now); err != nil {
		return err
	}
	s.publisher.Publish("payment.failed", map[string]interface{}{
		"paymentId": payment.ID,
		"bookingId": payment.BookingID,
	})
	return nil
}

func (s *PaymentService) GetPaymentStatus(ctx context.Context, paymentID string) (*model.PaymentStatusResponse, error) {
	p, err := s.repo.FindByID(ctx, paymentID)
	if err != nil {
		return nil, err
	}
	return &model.PaymentStatusResponse{
		ID: p.ID, Status: p.Status, Amount: p.Amount, Currency: p.Currency,
		CreatedAt: p.CreatedAt, ProcessedAt: p.ProcessedAt,
	}, nil
}

func (s *PaymentService) ProcessRefund(ctx context.Context, paymentID, reason string) error {
	payment, err := s.repo.FindByID(ctx, paymentID)
	if err != nil {
		return err
	}
	if payment.Status != "SUCCESS" {
		return fmt.Errorf("cannot refund payment in status %s", payment.Status)
	}

	refundID := "re_stub_" + uuid.New().String()[:8]
	if !s.useStripeStub {
		params := &stripe.RefundParams{PaymentIntent: stripe.String(payment.ExternalPaymentID)}
		r, err := refund.New(params)
		if err != nil {
			return fmt.Errorf("stripe refund error: %w", err)
		}
		refundID = r.ID
	}

	now := time.Now()
	ref := &model.Refund{
		ID: uuid.New().String(), PaymentID: payment.ID, Amount: payment.Amount,
		Reason: reason, Status: "PROCESSED", ExternalRefundID: refundID, CreatedAt: now,
	}
	if err := s.repo.CreateRefund(ctx, ref); err != nil {
		return err
	}
	if err := s.repo.UpdateStatus(ctx, payment.ID, "REFUNDED", &now); err != nil {
		return err
	}

	s.publisher.Publish("refund.processed", map[string]interface{}{
		"paymentId": payment.ID, "bookingId": payment.BookingID, "amount": payment.Amount,
	})
	return nil
}
