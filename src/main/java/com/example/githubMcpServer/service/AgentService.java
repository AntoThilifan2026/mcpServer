package com.example.githubMcpServer.service;

import com.example.githubMcpServer.mcp.McpTools;
import com.example.githubMcpServer.util.ConversationMemory;
import com.example.githubMcpServer.util.SessionContext;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final GeminiService gemini;
    private final McpTools tools;
    private final ConversationMemory memory;
    private final SessionContext session;

    public AgentService(
            GeminiService gemini,
            McpTools tools,
            ConversationMemory memory,
            SessionContext session) {

        this.gemini = gemini;
        this.tools = tools;
        this.memory = memory;
        this.session = session;
    }

    public String process(String userInput)
            throws Exception {

        memory.add(
                "USER",
                userInput);

        int maxIterations = 10;

        for (int i = 0; i < maxIterations; i++) {

            String prompt =
                    buildPrompt();

            String response =
                    gemini.chat(prompt);

//            System.out.println("\nLLM -> " + response);

            if (response.startsWith("TOOL:")) {

                String toolResult = executeTool(response);

                memory.add(
                        "TOOL",
                        toolResult);

                continue;
            }

            memory.add(
                    "ASSISTANT",
                    response);

            return response;
        }

        return "Maximum iterations reached";
    }

    private String buildPrompt() {

        return """
                You are GitHubAgent.

                Available Tools:

                TOOL:cloneRepo:<repoUrl>
                Clone repository.

                TOOL:findRootPom:<repoDir>
                Find root pom.xml.

                TOOL:readPom:<pomPath>
                Read pom.xml.

                Rules:

                1. Ask user for missing information.
                2. Use tools when required.
                3. Do not explain Git commands.
                4. Return tool call only when tool is needed.
                5. After receiving tool result continue reasoning.

                Current Session:

                Repo Directory:
                %s

                Pom Path:
                %s

                Conversation:

                %s
                """
                .formatted(
                        session.getRepoDir(),
                        session.getPomPath(),
                        memory.getConversation());
    }

    private String executeTool(String toolCall)
            throws Exception {

        System.out.println(
                "\nExecuting -> " + toolCall);

        if (toolCall.startsWith(
                "TOOL:cloneRepo:")) {

            String repoUrl =
                    toolCall.replace(
                            "TOOL:cloneRepo:",
                            "");

            String repoDir =
                    tools.cloneRepo(repoUrl);

            session.setRepoDir(repoDir);

            return """
                    Repository cloned successfully.

                    Repository:
                    %s

                    Local Path:
                    %s
                    """
                    .formatted(
                            repoUrl,
                            repoDir);
        }

        if (toolCall.startsWith("TOOL:findRootPom")) {

            String pomPath = tools.findRootPom(session.getRepoDir());

            session.setPomPath(pomPath);

            return """
                    Root pom.xml found.
                    Path:
                    %s
                    """.formatted(
                            pomPath);
        }

        if (toolCall.startsWith("TOOL:readPom")) {
            return tools.readPom(
                    session.getPomPath());
        }

        return "Unknown Tool";
    }
}