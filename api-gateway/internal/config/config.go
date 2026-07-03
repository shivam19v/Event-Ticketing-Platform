package config

import "os"

type Config struct {
	Port              string
	UserServiceURL    string
	EventServiceURL   string
	BookingServiceURL string
	PaymentServiceURL string
	TicketServiceURL  string
	RedisHost         string
	RedisPort         string
	JWTSecret         string
	CORSOrigin        string
}

func Load() *Config {
	return &Config{
		Port:              getEnv("PORT", "8000"),
		UserServiceURL:    getEnv("USER_SERVICE_URL", "http://localhost:8001"),
		EventServiceURL:   getEnv("EVENT_SERVICE_URL", "http://localhost:8002"),
		BookingServiceURL: getEnv("BOOKING_SERVICE_URL", "http://localhost:8003"),
		PaymentServiceURL: getEnv("PAYMENT_SERVICE_URL", "http://localhost:8004"),
		TicketServiceURL:  getEnv("TICKET_SERVICE_URL", "http://localhost:8005"),
		RedisHost:         getEnv("REDIS_HOST", "localhost"),
		RedisPort:         getEnv("REDIS_PORT", "6379"),
		JWTSecret:         getEnv("JWT_SECRET", "eventsphere-super-secret-jwt-key-2024"),
		CORSOrigin:        getEnv("CORS_ORIGIN", "http://localhost:3000"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
