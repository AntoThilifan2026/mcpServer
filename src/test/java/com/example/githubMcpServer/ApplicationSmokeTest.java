package com.example.githubMcpServer;

import com.example.githubMcpServer.mcp.McpTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationSmokeTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withUserConfiguration(GithubMcpServerApplication.class)
                    .withPropertyValues(
                            "gemini.api.key=test-key",
                            "github.token=test-token",
                            "github.default-owner=test-owner",
                            "github.default-branch=main",
                            "agent.card.path=classpath:github-agent.card.yml",
                            "spring.ai.mcp.server.enabled=false");

    @Test
    void springContextStartsAndRegistersMcpTools() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(McpTools.class));
            assertNotNull(context.getBean(ToolCallbackProvider.class));
        });
    }
}
