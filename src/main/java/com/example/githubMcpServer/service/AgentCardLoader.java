package com.example.githubMcpServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AgentCardLoader {

    @Value("${agent.card.path}")
    private Resource agentCard;

    public String loadAgentCard() throws Exception {

        return new String(
                agentCard.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
