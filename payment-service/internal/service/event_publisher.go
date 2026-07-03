package service

import (
	"encoding/json"
	"log"

	amqp "github.com/rabbitmq/amqp091-go"
)

type EventPublisher struct {
	conn    *amqp.Connection
	channel *amqp.Channel
}

func NewEventPublisher(amqpURL string) (*EventPublisher, error) {
	conn, err := amqp.Dial(amqpURL)
	if err != nil {
		return nil, err
	}
	ch, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, err
	}

	err = ch.ExchangeDeclare(
		"eventsphere.payments", // name
		"topic",                // type
		true,                   // durable
		false, false, false, nil,
	)
	if err != nil {
		return nil, err
	}

	return &EventPublisher{conn: conn, channel: ch}, nil
}

func (p *EventPublisher) Publish(routingKey string, payload map[string]interface{}) error {
	body, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	err = p.channel.Publish(
		"eventsphere.payments",
		routingKey,
		false, false,
		amqp.Publishing{
			ContentType:  "application/json",
			Body:         body,
			DeliveryMode: amqp.Persistent,
		},
	)
	if err != nil {
		log.Printf("[EventPublisher] failed to publish %s: %v", routingKey, err)
	} else {
		log.Printf("[EventPublisher] published %s", routingKey)
	}
	return err
}

func (p *EventPublisher) Close() {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		p.conn.Close()
	}
}
