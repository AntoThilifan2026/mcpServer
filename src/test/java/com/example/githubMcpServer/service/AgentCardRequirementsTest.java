package com.example.githubMcpServer.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCardRequirementsTest {

    private String loadCard() throws Exception {
        ClassPathResource resource = new ClassPathResource("github-agent.card.yml");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void agentCardDeclaresConfiguredControlLoop() throws Exception {
        assertTrue(loadCard().contains("controlLoop: mcp_with_tools"));
    }

    @Test
    void agentCardContainsStructuredMcpToolsSection() throws Exception {
        String card = loadCard();

        assertTrue(
                card.matches("(?s).*(?m)^(mcp_tools|mcpTools|mcp):\\s*$.*"),
                "Agent Card must contain a structured MCP tools section");
    }

    @Test
    void agentCardListsEveryExposedMcpTool() throws Exception {
        String card = loadCard();
        List<String> tools = List.of(
                "cloneRepo",
                "findRootPom",
                "findChildPoms",
                "readPom",
                "savePom",
                "createBranch",
                "pushChanges",
                "createPR",
                "cleanupRepository");

        assertAll(tools.stream().map(tool ->
                () -> assertTrue(card.contains(tool), "Agent Card is missing MCP tool " + tool)));
    }
}
