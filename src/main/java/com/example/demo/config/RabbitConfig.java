package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "agent.tasks.exchange";
    public static final String QUEUE_SUPERVISOR = "supervisor-tasks";
    public static final String ROUTING_KEY_SUPERVISOR = "task.supervisor.request";

    @Bean
    public TopicExchange taskExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue supervisorQueue() {
        return new Queue(QUEUE_SUPERVISOR, true);
    }

    @Bean
    public Binding supervisorBinding(Queue supervisorQueue, TopicExchange taskExchange) {
        return BindingBuilder.bind(supervisorQueue).to(taskExchange).with(ROUTING_KEY_SUPERVISOR);
    }
}
