package com.eventsphere.ticket.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class RabbitMQConfig {

    // Declared identically (durable topic exchange) by booking-service too —
    // RabbitMQ treats re-declaration with matching properties as a no-op.
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange("eventsphere.bookings", true, false);
    }

    // Service-specific queue name (not shared with any other consumer) so this
    // service always gets its own copy of every booking.confirmed event.
    @Bean
    public Queue ticketBookingConfirmedQueue() {
        return QueueBuilder.durable("ticket.booking.confirmed").build();
    }

    @Bean
    public Binding ticketBookingConfirmedBinding() {
        return BindingBuilder.bind(ticketBookingConfirmedQueue())
            .to(bookingExchange())
            .with("booking.confirmed");
    }
}
