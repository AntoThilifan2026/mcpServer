package com.example.mcpServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class PullRequestService {

    @Value("${github.token}")
    private String token;

    public String createPR(String owner, String repo, String branch) {

        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/pulls";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "title", "AI Fix: Dependency Upgrade",
                "head", branch,
                "base", "main",
                "body", "Automated fix using Gemini AI"
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        return response.getBody();
    }
}