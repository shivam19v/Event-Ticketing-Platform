package config

import "os"

type Config struct {
	Port               string
	DatabaseURL        string
	RabbitMQURL        string
	StripeSecretKey    string
	StripeWebhookSecret string
	JWTSecret          string
}

func Load() *Config {
	return &Config{
		Port:                getEnv("PORT", "8080"),
		DatabaseURL:         getEnv("DATABASE_URL", "postgres://appuser:eventsphere_dev_pass@localhost:5432/eventsphere?sslmode=disable"),
		RabbitMQURL:         getEnv("RABBITMQ_URL", "amqp://guest:guest@localhost:5672/"),
		StripeSecretKey:     getEnv("STRIPE_SECRET_KEY", "sk_test_placeholder"),
		StripeWebhookSecret: getEnv("STRIPE_WEBHOOK_SECRET", "whsec_placeholder"),
		JWTSecret:           getEnv("JWT_SECRET", "eventsphere-super-secret-jwt-key-2024"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
