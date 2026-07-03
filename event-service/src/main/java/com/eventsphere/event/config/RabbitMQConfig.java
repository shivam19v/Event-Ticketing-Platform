package com.eventsphere.event.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.*;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitMQConfig {
    @Bean public TopicExchange eventExchange() { return new TopicExchange("eventsphere.events", true, false); }
    @Bean public Queue eventPublishedQueue() { return QueueBuilder.durable("event.published").build(); }
    @Bean public Queue eventCancelledQueue() { return QueueBuilder.durable("event.cancelled").build(); }
    @Bean public Binding eventPublishedBinding() { return BindingBuilder.bind(eventPublishedQueue()).to(eventExchange()).with("event.published"); }
    @Bean public Binding eventCancelledBinding() { return BindingBuilder.bind(eventCancelledQueue()).to(eventExchange()).with("event.cancelled"); }
    @Bean public MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf); t.setMessageConverter(messageConverter()); return t;
    }
}
