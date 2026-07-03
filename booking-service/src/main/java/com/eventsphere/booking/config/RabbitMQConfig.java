package com.eventsphere.booking.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitMQConfig {
    @Bean public TopicExchange bookingExchange() { return new TopicExchange("eventsphere.bookings", true, false); }
    @Bean public DirectExchange dlxExchange() { return new DirectExchange("eventsphere.dlx", true, false); }
    @Bean public Queue bookingCreatedQueue() {
        return QueueBuilder.durable("booking.created")
            .withArgument("x-dead-letter-exchange", "eventsphere.dlx")
            .withArgument("x-dead-letter-routing-key", "dlq").build();
    }
    @Bean public Queue bookingConfirmedQueue() { return QueueBuilder.durable("booking.confirmed").build(); }
    @Bean public Queue bookingCancelledQueue() { return QueueBuilder.durable("booking.cancelled").build(); }
    @Bean public Queue deadLetterQueue() { return QueueBuilder.durable("eventsphere.dlq").build(); }
    @Bean public Binding bookingCreatedBinding() { return BindingBuilder.bind(bookingCreatedQueue()).to(bookingExchange()).with("booking.created"); }
    @Bean public Binding bookingConfirmedBinding() { return BindingBuilder.bind(bookingConfirmedQueue()).to(bookingExchange()).with("booking.confirmed"); }
    @Bean public Binding bookingCancelledBinding() { return BindingBuilder.bind(bookingCancelledQueue()).to(bookingExchange()).with("booking.cancelled"); }
    @Bean public Binding dlqBinding() { return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("dlq"); }
    @Bean public MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf); t.setMessageConverter(messageConverter()); return t;
    }
}
