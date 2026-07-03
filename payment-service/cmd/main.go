package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/eventsphere/payment-service/internal/config"
	"github.com/eventsphere/payment-service/internal/handler"
	"github.com/eventsphere/payment-service/internal/repository"
	"github.com/eventsphere/payment-service/internal/service"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

func main() {
	cfg := config.Load()

	db, err := repository.NewDB(cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("failed to connect to database: %v", err)
	}
	defer db.Close()
	log.Println("✅ connected to database, migrations applied")

	var publisher *service.EventPublisher
	for i := 0; i < 5; i++ {
		publisher, err = service.NewEventPublisher(cfg.RabbitMQURL)
		if err == nil {
			break
		}
		log.Printf("waiting for rabbitmq... (%d/5): %v", i+1, err)
		time.Sleep(3 * time.Second)
	}
	if err != nil {
		log.Fatalf("failed to connect to rabbitmq: %v", err)
	}
	defer publisher.Close()
	log.Println("✅ connected to rabbitmq")

	paymentRepo := repository.NewPaymentRepository(db)
	paymentService := service.NewPaymentService(paymentRepo, publisher, cfg.StripeSecretKey, cfg.StripeWebhookSecret)
	paymentHandler := handler.NewPaymentHandler(paymentService)

	r := chi.NewRouter()
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(30 * time.Second))

	r.Get("/health", handler.HealthCheck)

	// Webhook must be public (Stripe calls it directly, no JWT)
	r.Post("/api/v1/payments/webhook", paymentHandler.StripeWebhook)
	r.Post("/webhooks/stripe", paymentHandler.StripeWebhook)

	r.Route("/api/v1/payments", func(r chi.Router) {
		r.Use(handler.AuthMiddleware(cfg.JWTSecret))
		r.Post("/initiate", paymentHandler.InitiatePayment)
		r.Get("/{paymentId}/status", paymentHandler.GetPaymentStatus)
		r.Post("/{paymentId}/refund", paymentHandler.RefundPayment)
	})

	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		log.Printf("🚀 payment-service listening on :%s", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt)
	<-quit
	log.Println("shutting down payment-service...")

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	srv.Shutdown(ctx)
}
