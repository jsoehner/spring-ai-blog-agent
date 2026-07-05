package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@Profile("image-agent")
public class ImageAgentController {

    private final ChatClient chatClient;

    public ImageAgentController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build().mutate()
                .defaultSystem("You are an expert image prompt generator. Given a blog post topic or content, generate a highly descriptive visual prompt for an image that represents it. Output ONLY the short image prompt, nothing else.")
                .build();
    }

    @PostMapping(value = "/image", produces = MediaType.TEXT_PLAIN_VALUE)
    public String generateImageUrls(@RequestBody Map<String, String> request) {
        String topic = request.getOrDefault("topic", "Technology");
        String content = request.getOrDefault("content", topic);

        // Generate a visual prompt for the header image
        String headerPromptStr = chatClient.prompt()
                .user("Generate a short, descriptive visual prompt for a header image for a blog post about: " + topic)
                .call()
                .content();

        // Generate a visual prompt for a generic inline image based on content
        String inlinePromptStr = chatClient.prompt()
                .user("Generate a short, descriptive visual prompt for a secondary inline image related to this content snippet: " + 
                      (content.length() > 500 ? content.substring(0, 500) : content))
                .call()
                .content();

        // Build pollinations URLs
        String encodedHeader = URLEncoder.encode(headerPromptStr.trim(), StandardCharsets.UTF_8);
        String encodedInline = URLEncoder.encode(inlinePromptStr.trim(), StandardCharsets.UTF_8);

        String headerImageUrl = "https://image.pollinations.ai/prompt/" + encodedHeader + "?width=1200&height=600&nologo=true";
        String inlineImageUrl = "https://image.pollinations.ai/prompt/" + encodedInline + "?width=800&height=600&nologo=true";

        // Return the two URLs separated by a newline
        return headerImageUrl + "\n" + inlineImageUrl;
    }
}
