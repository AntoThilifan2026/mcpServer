package com.example.githubMcpServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String analyzeAndFix(String content) {

        String prompt = "Fix vulnerabilities in this pom.xml:\n" + content;

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        RestTemplate rest = new RestTemplate();

        Map response = rest.postForObject(url, body, Map.class);

        var candidates = (java.util.List<Map>) response.get("candidates");
        var contentMap = (Map) candidates.get(0).get("content");
        var parts = (java.util.List<Map>) contentMap.get("parts");

        return parts.get(0).get("text").toString();
    }
}
