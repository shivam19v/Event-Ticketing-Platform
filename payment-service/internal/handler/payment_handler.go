package handler

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"time"

	"github.com/eventsphere/payment-service/internal/model"
	"github.com/eventsphere/payment-service/internal/repository"
	"github.com/eventsphere/payment-service/internal/service"
	"github.com/go-chi/chi/v5"
)

type PaymentHandler struct {
	service *service.PaymentService
}

func NewPaymentHandler(s *service.PaymentService) *PaymentHandler {
	return &PaymentHandler{service: s}
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, errType, message string) {
	writeJSON(w, status, model.ErrorResponse{Error: errType, Message: message})
}

func (h *PaymentHandler) InitiatePayment(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(ctxUserID).(string)
	if userID == "" {
		userID = "anonymous"
	}

	var req model.InitiatePaymentRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "ValidationError", "Invalid request body")
		return
	}
	if req.BookingID == "" {
		writeError(w, http.StatusBadRequest, "ValidationError", "bookingId is required")
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	resp, err := h.service.InitiatePayment(ctx, userID, req)
	if err != nil {
		if errors.Is(err, service.ErrInvalidAmount) {
			writeError(w, http.StatusBadRequest, "ValidationError", "Amount must be greater than zero")
			return
		}
		writeError(w, http.StatusInternalServerError, "InternalServerError", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *PaymentHandler) GetPaymentStatus(w http.ResponseWriter, r *http.Request) {
	paymentID := chi.URLParam(r, "paymentId")
	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	resp, err := h.service.GetPaymentStatus(ctx, paymentID)
	if err != nil {
		if errors.Is(err, repository.ErrNotFound) {
			writeError(w, http.StatusNotFound, "NotFoundError", "Payment not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "InternalServerError", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *PaymentHandler) RefundPayment(w http.ResponseWriter, r *http.Request) {
	paymentID := chi.URLParam(r, "paymentId")

	var req model.RefundRequest
	json.NewDecoder(r.Body).Decode(&req) // optional body

	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	if err := h.service.ProcessRefund(ctx, paymentID, req.Reason); err != nil {
		writeError(w, http.StatusBadRequest, "RefundError", err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "REFUNDED"})
}

func (h *PaymentHandler) StripeWebhook(w http.ResponseWriter, r *http.Request) {
	payload, err := io.ReadAll(r.Body)
	if err != nil {
		writeError(w, http.StatusBadRequest, "BadRequest", "Could not read body")
		return
	}
	signature := r.Header.Get("Stripe-Signature")

	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	if err := h.service.HandleStripeWebhook(ctx, payload, signature); err != nil {
		writeError(w, http.StatusBadRequest, "WebhookError", err.Error())
		return
	}
	w.WriteHeader(http.StatusOK)
}

func HealthCheck(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{
		"status": "healthy", "timestamp": time.Now().Format(time.RFC3339),
	})
}
