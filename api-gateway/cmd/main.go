package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/eventsphere/api-gateway/internal/config"
	"github.com/eventsphere/api-gateway/internal/router"
	"github.com/redis/go-redis/v9"
)

func main() {
	cfg := config.Load()

	rdb := redis.NewClient(&redis.Options{
		Addr: cfg.RedisHost + ":" + cfg.RedisPort,
	})

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Printf("⚠️  warning: redis not reachable at startup: %v (rate limiting will fail open)", err)
	}
	cancel()

	handler, err := router.New(cfg, rdb)
	if err != nil {
		log.Fatalf("failed to build router: %v", err)
	}

	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      handler,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		log.Printf("🚀 api-gateway listening on :%s", cfg.Port)
		log.Printf("   → user-service:    %s", cfg.UserServiceURL)
		log.Printf("   → event-service:   %s", cfg.EventServiceURL)
		log.Printf("   → booking-service: %s", cfg.BookingServiceURL)
		log.Printf("   → payment-service: %s", cfg.PaymentServiceURL)
		log.Printf("   → ticket-service:  %s", cfg.TicketServiceURL)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt)
	<-quit
	log.Println("shutting down api-gateway...")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()
	srv.Shutdown(shutdownCtx)
}
