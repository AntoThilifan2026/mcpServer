package com.example.githubMcpServer.service;

import com.example.githubMcpServer.mcp.McpTools;
import com.example.githubMcpServer.util.ConversationMemory;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final GeminiService gemini;
    private final McpTools tools;
    private final ConversationMemory memory;

    public AgentService(GeminiService gemini, McpTools tools, ConversationMemory memory) {

        this.gemini = gemini;
        this.tools = tools;
        this.memory = memory;
    }

    public String process(
            String userMessage)
            throws Exception {

        memory.add(
                "USER",
                userMessage);

        String prompt = """
                %s

                Conversation:

                %s
                """
                .formatted(
                        loadAgentCard(),
                        memory.getConversation());

        String response =
                gemini.chat(prompt);

        memory.add(
                "ASSISTANT",
                response);

        return response;
    }

    private String loadAgentCard() {

        return """
                You are GitHubAgent.

                Available Tools:

                TOOL:cloneRepo:<repoUrl>
                TOOL:findRootPom:<repoDir>
                TOOL:readPom:<pomPath>

                Use tools whenever needed.
                """;
    }
}