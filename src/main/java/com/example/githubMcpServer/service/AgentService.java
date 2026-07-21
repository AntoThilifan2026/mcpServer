package com.example.githubMcpServer.service;

import com.example.githubMcpServer.mcp.McpTools;
import com.example.githubMcpServer.util.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentService {

    private final GeminiService gemini;
    private final McpTools tools;
    private final ConversationMemory memory;
    private final SessionContext session;
    private final AgentCardLoader cardLoader;

    public AgentService(
            GeminiService gemini,
            McpTools tools,
            ConversationMemory memory,
            SessionContext session,
            AgentCardLoader cardLoader) {

        this.gemini = gemini;
        this.tools = tools;
        this.memory = memory;
        this.session = session;
        this.cardLoader = cardLoader;
    }

    public String process(String userInput)
            throws Exception {

        memory.add(new TextMessage(Role.USER, userInput));

        int maxIterations = 10;

        for (int i = 0; i < maxIterations; i++) {

            List<Message> prompt = new ArrayList<>();

            prompt.add(new TextMessage(Role.SYSTEM, cardLoader.loadAgentCard()
                                    + "\n\nCurrent Session:\n"
                                    + "Repository Path: "
                                    + session.getRepoDir()
                                    + "\nPom Path: "
                                    + session.getPomPath()));
            prompt.addAll(memory.getMessages());

            Message response = gemini.chat(prompt);

            if (!(response instanceof TextMessage textResponse)) {
                throw new RuntimeException(
                        "Unexpected response type");
            }

            System.out.println("\nLLM -> " + textResponse.content());

            if (textResponse.content()
                    .startsWith("TOOL:")) {

                String toolResult = executeTool(textResponse.content());

                memory.add(
                        new ToolResult(
                                "tool-call",
                                toolResult));

                continue;
            }

            memory.add(response);
            return textResponse.content();
        }

        return "Maximum iterations reached";
    }

    private String executeTool(
            String toolCall)
            throws Exception {

        System.out.println("\nExecuting -> " + toolCall);

        if (toolCall.startsWith("TOOL:cloneRepo:")) {
            String repoUrl = toolCall.replace("TOOL:cloneRepo:", "");
            String repoDir = tools.cloneRepo(repoUrl);
            session.setRepoDir(repoDir);
            return """
                    Repository cloned successfully.

                    Repository:
                    %s

                    Local Path:
                    %s
                    """.formatted(repoUrl, repoDir);
        }

        if (toolCall.startsWith("TOOL:findRootPom")) {

            String pomPath = tools.findRootPom(session.getRepoDir());

            session.setPomPath(pomPath);

            return """
                    Root pom.xml found.

                    Path:
                    %s
                    """.formatted(pomPath);
        }

        if (toolCall.startsWith(
                "TOOL:findChildPoms")) {

            return tools.findChildPoms(session.getRepoDir()).toString();
        }

        if (toolCall.startsWith("TOOL:readPom")) {
            return tools.readPom(session.getPomPath());
        }

        if (toolCall.startsWith("TOOL:savePom:")) {
            String payload = toolCall.replace("TOOL:savePom:", "");
            String[] parts = payload.split(
                            "\\|",
                            2);
            return tools.savePom(parts[0], parts[1]);
        }

        if (toolCall.startsWith("TOOL:createBranch:")) {

            String branchName = toolCall.replace(
                            "TOOL:createBranch:",
                            "");

            session.setBranchName(branchName);

            return tools.createBranch(session.getRepoDir(), branchName);
        }

        if (toolCall.startsWith("TOOL:pushChanges:")) {

            String payload = toolCall.replace(
                            "TOOL:pushChanges:",
                            "");

            String[] parts = payload.split(
                            "\\|",
                            2);

            return tools.pushChanges(session.getRepoDir(), parts[0], parts[1]);
        }

        if (toolCall.startsWith("TOOL:createPR:")) {

            String payload = toolCall.replace(
                            "TOOL:createPR:",
                            "");

            String[] parts = payload.split("\\|");

            return tools.createPR(
                    parts[0],
                    parts[1],
                    parts[2]);
        }

        if (toolCall.startsWith("TOOL:cleanupRepository")) {

            String result = tools.cleanupRepository(
                    session.getRepoDir());

            session.setRepoDir(null);
            session.setPomPath(null);
            session.setBranchName(null);

            memory.clear();

            return result;
        }

        return "Unknown Tool : " + toolCall;
    }
}