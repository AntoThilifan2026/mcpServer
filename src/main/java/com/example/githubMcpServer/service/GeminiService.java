package com.example.githubMcpServer.service;

import com.example.githubMcpServer.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ThrottlingExecutor throttlingExecutor;
    private final McpToolDiscoveryService toolDiscoveryService;
    private final GeminiToolTransformer transformer;

    public GeminiService(
            ThrottlingExecutor throttlingExecutor,
            McpToolDiscoveryService toolDiscoveryService,
            GeminiToolTransformer transformer) {

        this.throttlingExecutor = throttlingExecutor;
        this.toolDiscoveryService = toolDiscoveryService;
        this.transformer = transformer;
    }

    public Message chat(
            List<Message> messages) {

        String prompt =
                buildPrompt(messages);

        String response = throttlingExecutor.execute(() -> invokeGemini(prompt));

        return new TextMessage(Role.ASSISTANT, response);
    }

    private String buildPrompt(
            List<Message> messages) {

        StringBuilder prompt = new StringBuilder();

        for (Message message : messages) {

            if (message instanceof TextMessage textMessage) {
                prompt.append("[")
                        .append(textMessage.role())
                        .append("]\n")
                        .append(textMessage.content())
                        .append("\n\n");
            }

            else if (message instanceof ToolCall toolCall) {
                prompt.append("[TOOL_CALL]\n")
                        .append("Id: ")
                        .append(toolCall.id())
                        .append("\n")
                        .append("Tool: ")
                        .append(toolCall.name())
                        .append("\n")
                        .append("Arguments: ")
                        .append(toolCall.arguments())
                        .append("\n\n");
            }

            else if (message instanceof ToolResult toolResult) {

                prompt.append("[TOOL_RESULT]\n")
                        .append("Tool Call Id: ")
                        .append(toolResult.toolCallId())
                        .append("\n")
                        .append("Result:\n")
                        .append(toolResult.result())
                        .append("\n\n");
            }
        }

        return prompt.toString();
    }

    private String invokeGemini(
            String prompt) {

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                        + apiKey;

        List<McpTool> mcpTools =
                toolDiscoveryService.discoverTools();

        List<Map<String, Object>> geminiTools =
                transformer.transform(
                        mcpTools);

        Map<String, Object> requestBody =
                Map.of(
                        "contents",
                        List.of(
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of(
                                                        "text",
                                                        prompt)
                                        )
                                )
                        ),
                        "tools",
                        geminiTools
                );


        RestTemplate restTemplate =
                new RestTemplate();

        Map<?, ?> response =
                restTemplate.postForObject(
                        url,
                        requestBody,
                        Map.class);


        if (response == null) {

            throw new RuntimeException(
                    "No response from Gemini");
        }

        if (response.containsKey("error")) {

            throw new RuntimeException(
                    "Gemini Error: "
                            + response.get("error"));
        }

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>)
                        response.get(
                                "candidates");

        if (candidates == null ||
                candidates.isEmpty()) {

            throw new RuntimeException(
                    "No candidates returned");
        }

        Map<String, Object> candidate =
                candidates.get(0);

        Map<String, Object> content =
                (Map<String, Object>)
                        candidate.get(
                                "content");

        if (content == null) {

            throw new RuntimeException(
                    "Content missing in Gemini response: "
                            + response);
        }

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>)
                        content.get(
                                "parts");

        if (parts == null ||
                parts.isEmpty()) {

            throw new RuntimeException(
                    "Parts missing in Gemini response: "
                            + response);
        }

        Object text =
                parts.get(0)
                        .get("text");

        if (text == null) {

            throw new RuntimeException(
                    "Text missing in Gemini response: "
                            + response);
        }

        return text.toString();
    }
}