package com.eventsphere.notification.config;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitMQConfig {
    // Booking exchange (declared by booking-service, but declared here too for idempotent setup)
    @Bean public TopicExchange bookingExchange() { return new TopicExchange("eventsphere.bookings", true, false); }
    @Bean public Queue notifyBookingCreatedQueue()   { return QueueBuilder.durable("notify.booking.created").build(); }
    @Bean public Queue notifyBookingConfirmedQueue() { return QueueBuilder.durable("notify.booking.confirmed").build(); }
    @Bean public Queue notifyBookingCancelledQueue() { return QueueBuilder.durable("notify.booking.cancelled").build(); }
    @Bean public Binding bindBookingCreated()   { return BindingBuilder.bind(notifyBookingCreatedQueue()).to(bookingExchange()).with("booking.created"); }
    @Bean public Binding bindBookingConfirmed() { return BindingBuilder.bind(notifyBookingConfirmedQueue()).to(bookingExchange()).with("booking.confirmed"); }
    @Bean public Binding bindBookingCancelled() { return BindingBuilder.bind(notifyBookingCancelledQueue()).to(bookingExchange()).with("booking.cancelled"); }

    // Event-service exchange
    @Bean public TopicExchange eventExchange() { return new TopicExchange("eventsphere.events", true, false); }
    @Bean public Queue notifyEventCancelledQueue() { return QueueBuilder.durable("notify.event.cancelled").build(); }
    @Bean public Binding bindEventCancelled() { return BindingBuilder.bind(notifyEventCancelledQueue()).to(eventExchange()).with("event.cancelled"); }

    // Payment-service exchange
    @Bean public TopicExchange paymentExchange() { return new TopicExchange("eventsphere.payments", true, false); }
    @Bean public Queue notifyRefundProcessedQueue() { return QueueBuilder.durable("notify.refund.processed").build(); }
    @Bean public Binding bindRefundProcessed() { return BindingBuilder.bind(notifyRefundProcessedQueue()).to(paymentExchange()).with("refund.processed"); }
}
