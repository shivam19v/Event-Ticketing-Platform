package router

import (
	"net/http"
	"time"

	"github.com/eventsphere/api-gateway/internal/config"
	mw "github.com/eventsphere/api-gateway/internal/middleware"
	"github.com/eventsphere/api-gateway/internal/proxy"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/redis/go-redis/v9"
)

func New(cfg *config.Config, rdb *redis.Client) (http.Handler, error) {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(30 * time.Second))
	r.Use(mw.CORS(cfg.CORSOrigin))

	userProxy, err := proxy.NewReverseProxy(cfg.UserServiceURL)
	if err != nil {
		return nil, err
	}
	eventProxy, err := proxy.NewReverseProxy(cfg.EventServiceURL)
	if err != nil {
		return nil, err
	}
	bookingProxy, err := proxy.NewReverseProxy(cfg.BookingServiceURL)
	if err != nil {
		return nil, err
	}
	paymentProxy, err := proxy.NewReverseProxy(cfg.PaymentServiceURL)
	if err != nil {
		return nil, err
	}
	ticketProxy, err := proxy.NewReverseProxy(cfg.TicketServiceURL)
	if err != nil {
		return nil, err
	}

	r.Get("/health", func(w http.ResponseWriter, req *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"healthy","service":"api-gateway"}`))
	})

	// ─── Public routes ──────────────────────────────────────────────────────
	r.Post("/api/v1/auth/register", userProxy.ServeHTTP)
	r.Post("/api/v1/auth/login", userProxy.ServeHTTP)
	r.Post("/api/v1/auth/refresh", userProxy.ServeHTTP)
	r.Get("/api/v1/events", eventProxy.ServeHTTP)
	r.Get("/api/v1/events/{eventId}", eventProxy.ServeHTTP)
	r.Get("/api/v1/events/{eventId}/seats", eventProxy.ServeHTTP)

	// Stripe webhook — public (signature verified inside payment-service)
	r.Post("/api/v1/payments/webhook", paymentProxy.ServeHTTP)
	r.Post("/webhooks/stripe", paymentProxy.ServeHTTP)

	// ─── Protected routes ───────────────────────────────────────────────────
	r.Group(func(r chi.Router) {
		r.Use(mw.AuthRequired(cfg.JWTSecret))

		// Auth (logout requires a valid token)
		r.Post("/api/v1/auth/logout", userProxy.ServeHTTP)

		// Users
		r.Get("/api/v1/users/me", userProxy.ServeHTTP)
		r.Get("/api/v1/users/{userId}", userProxy.ServeHTTP)
		r.Put("/api/v1/users/{userId}", userProxy.ServeHTTP)

		// Events (write ops, organizer-only — enforced downstream)
		r.Post("/api/v1/events", eventProxy.ServeHTTP)
		r.Put("/api/v1/events/{eventId}", eventProxy.ServeHTTP)
		r.Post("/api/v1/events/{eventId}/publish", eventProxy.ServeHTTP)
		r.Post("/api/v1/events/{eventId}/cancel", eventProxy.ServeHTTP)
		r.Get("/api/v1/events/my", eventProxy.ServeHTTP)

		// Bookings — rate limited (high concurrency path)
		r.With(mw.RateLimit(rdb, 100, time.Minute)).Post("/api/v1/bookings/reserve", bookingProxy.ServeHTTP)
		r.Get("/api/v1/bookings/{bookingId}", bookingProxy.ServeHTTP)
		r.Post("/api/v1/bookings/{bookingId}/confirm", bookingProxy.ServeHTTP)
		r.Post("/api/v1/bookings/{bookingId}/cancel", bookingProxy.ServeHTTP)
		r.Get("/api/v1/users/{userId}/bookings", bookingProxy.ServeHTTP)

		// Payments
		r.Post("/api/v1/payments/initiate", paymentProxy.ServeHTTP)
		r.Get("/api/v1/payments/{paymentId}/status", paymentProxy.ServeHTTP)
		r.Post("/api/v1/payments/{paymentId}/refund", paymentProxy.ServeHTTP)

		// Tickets
		r.Get("/api/v1/tickets/{ticketId}", ticketProxy.ServeHTTP)
		r.Get("/api/v1/tickets/{ticketId}/download", ticketProxy.ServeHTTP)
		r.Post("/api/v1/tickets/{ticketId}/validate", ticketProxy.ServeHTTP)
		r.Get("/api/v1/users/{userId}/tickets", ticketProxy.ServeHTTP)
	})

	return r, nil
}
