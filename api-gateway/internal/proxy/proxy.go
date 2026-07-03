package proxy

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
)

// NewReverseProxy creates a reverse proxy to a downstream service,
// rewriting the path according to pathRewrite (e.g. strip /api/v1/users -> /api/v1/users on backend,
// since our internal services use the same /api/v1/* prefix).
func NewReverseProxy(targetBase string) (http.Handler, error) {
	target, err := url.Parse(targetBase)
	if err != nil {
		return nil, err
	}

	proxy := httputil.NewSingleHostReverseProxy(target)

	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		originalDirector(req)
		req.Host = target.Host
	}

	proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		log.Printf("[proxy] error forwarding to %s%s: %v", targetBase, r.URL.Path, err)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusServiceUnavailable)
		w.Write([]byte(`{"error":"ServiceUnavailable","message":"Upstream service is unavailable"}`))
	}

	return proxy, nil
}
