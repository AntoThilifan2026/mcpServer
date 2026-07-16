package com.example.githubMcpServer.service;

import com.example.githubMcpServer.util.ThrottlingExecutor;
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

    public GeminiService(
            ThrottlingExecutor throttlingExecutor) {

        this.throttlingExecutor = throttlingExecutor;
    }

    public String chat(String prompt) {

        return throttlingExecutor.execute(() -> invokeGemini(prompt));
    }

    private String invokeGemini(String prompt) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                        + apiKey;

        Map<String, Object> body =
                Map.of("contents", new Object[]{
                        Map.of("parts", new Object[]{Map.of("text", prompt)})
                        });

        RestTemplate rest = new RestTemplate();

        Map response =
                rest.postForObject(
                        url,
                        body,
                        Map.class);

        List<Map> candidates =
                (List<Map>) response.get("candidates");

        Map contentMap =
                (Map) candidates.get(0)
                        .get("content");

        List<Map> parts =
                (List<Map>) contentMap.get("parts");

        return parts.get(0)
                .get("text")
                .toString();
    }
}