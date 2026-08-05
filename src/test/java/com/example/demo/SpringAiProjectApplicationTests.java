package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.chat.model.ChatModel;

@SpringBootTest
class SpringAiProjectApplicationTests {

	@MockitoBean
	private ChatModel chatModel;

	@Test
	void contextLoads() {
	}

}
