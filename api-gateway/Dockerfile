FROM golang:1.21-alpine AS build
WORKDIR /app
RUN apk add --no-cache git
ENV GOPROXY=direct
ENV GOSUMDB=off
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o api-gateway ./cmd/main.go

FROM alpine:3.19
RUN apk add --no-cache ca-certificates
WORKDIR /app
COPY --from=build /app/api-gateway .
EXPOSE 8000
ENTRYPOINT ["./api-gateway"]
