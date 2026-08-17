package com.example.demo.service;

import com.example.demo.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE_NAME = RabbitConfig.EXCHANGE_NAME;

    public MessageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendResearchTask(String topic) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "task.research.request", topic);
    }

    public void sendImageTask(String topic, String content) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "task.image.request", content);
    }

    public void sendSeoTask(String topic) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "task.seo.request", topic);
    }

    public void sendSupervisorTask(String jsonPayload) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_SUPERVISOR, jsonPayload);
    }
}
