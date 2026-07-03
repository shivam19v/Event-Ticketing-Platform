package model

import "time"

type Payment struct {
	ID                string     `json:"id" db:"id"`
	BookingID         string     `json:"bookingId" db:"booking_id"`
	UserID            string     `json:"userId" db:"user_id"`
	Amount            float64    `json:"amount" db:"amount"`
	Currency          string     `json:"currency" db:"currency"`
	Status            string     `json:"status" db:"status"`
	PaymentMethod     string     `json:"paymentMethod" db:"payment_method"`
	ExternalPaymentID string     `json:"externalPaymentId,omitempty" db:"external_payment_id"`
	IdempotencyKey    string     `json:"idempotencyKey" db:"idempotency_key"`
	CreatedAt         time.Time  `json:"createdAt" db:"created_at"`
	ProcessedAt       *time.Time `json:"processedAt,omitempty" db:"processed_at"`
}

type Refund struct {
	ID                string     `json:"id" db:"id"`
	PaymentID         string     `json:"paymentId" db:"payment_id"`
	Amount            float64    `json:"amount" db:"amount"`
	Reason            string     `json:"reason" db:"reason"`
	Status            string     `json:"status" db:"status"`
	ExternalRefundID  string     `json:"externalRefundId,omitempty" db:"external_refund_id"`
	CreatedAt         time.Time  `json:"createdAt" db:"created_at"`
	ProcessedAt       *time.Time `json:"processedAt,omitempty" db:"processed_at"`
}

// Request / response payloads

type InitiatePaymentRequest struct {
	BookingID      string  `json:"bookingId"`
	Amount         float64 `json:"amount"`
	IdempotencyKey string  `json:"idempotencyKey"`
}

type InitiatePaymentResponse struct {
	PaymentID   string `json:"paymentId"`
	Status      string `json:"status"`
	RedirectURL string `json:"redirectUrl,omitempty"`
	ClientSecret string `json:"clientSecret,omitempty"`
}

type RefundRequest struct {
	Reason string `json:"reason"`
}

type PaymentStatusResponse struct {
	ID          string     `json:"id"`
	Status      string     `json:"status"`
	Amount      float64    `json:"amount"`
	Currency    string     `json:"currency"`
	CreatedAt   time.Time  `json:"createdAt"`
	ProcessedAt *time.Time `json:"processedAt,omitempty"`
}

type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message"`
}
