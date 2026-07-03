package middleware

import (
	"fmt"
	"net/http"
	"time"

	"github.com/redis/go-redis/v9"
)

func RateLimit(rdb *redis.Client, limit int, window time.Duration) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Prefer authenticated user ID, fall back to remote IP
			key := r.RemoteAddr
			if uid, ok := r.Context().Value(CtxUserID).(string); ok && uid != "" {
				key = uid
			}
			redisKey := fmt.Sprintf("ratelimit:%s", key)

			ctx := r.Context()
			count, err := rdb.Incr(ctx, redisKey).Result()
			if err != nil {
				// Redis unavailable: fail open (don't block traffic)
				next.ServeHTTP(w, r)
				return
			}
			if count == 1 {
				rdb.Expire(ctx, redisKey, window)
			}

			w.Header().Set("X-RateLimit-Limit", fmt.Sprintf("%d", limit))
			remaining := limit - int(count)
			if remaining < 0 {
				remaining = 0
			}
			w.Header().Set("X-RateLimit-Remaining", fmt.Sprintf("%d", remaining))

			if count > int64(limit) {
				http.Error(w, `{"error":"RateLimitError","message":"Rate limit exceeded"}`, http.StatusTooManyRequests)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
