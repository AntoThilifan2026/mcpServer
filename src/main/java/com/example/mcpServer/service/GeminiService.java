package com.example.mcpServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String analyzeAndFix(String fileContent) {

        String prompt = """
                You are a security expert.
               \s
                Find vulnerable dependencies in this pom.xml.
                Upgrade them to the latest safe version.
               \s
                Rules:
                - Keep structure same
                - Only change vulnerable versions
                - Output full updated pom.xml              \s
               \s""" + fileContent;

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key="
                        + apiKey;
        Map<String, Object> request = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        RestTemplate restTemplate = new RestTemplate();
        Map response = restTemplate.postForObject(url, request, Map.class);

        var candidates = (java.util.List<Map<String, Object>>) response.get("candidates");
        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (java.util.List<Map<String, Object>>) content.get("parts");

        return parts.get(0).get("text").toString();
    }
}