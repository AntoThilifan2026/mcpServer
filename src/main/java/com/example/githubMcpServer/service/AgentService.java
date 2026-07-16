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

        memory.add("USER", userInput);

        int maxIterations = 10;

        for (int i = 0; i < maxIterations; i++) {

            String prompt = cardLoader.loadAgentCard()
                    + "\n\nCurrent Session:\n"
                    + "Repository Path: "
                    + session.getRepoDir()
                    + "\nPom Path: "
                    + session.getPomPath()
                    + "\n\nConversation:\n"
                    + memory.getConversation();

            String response = gemini.chat(prompt);

            System.out.println("\nLLM -> " + response);

            if (response.startsWith("TOOL:")) {
                String toolResult = executeTool(response);
                memory.add("TOOL", toolResult);
                continue;
            }

            memory.add("ASSISTANT", response);

            return response;
        }

        return "Maximum iterations reached";
    }

    private String executeTool(String toolCall)
            throws Exception {

        System.out.println("\nExecuting -> " + toolCall);

        // Clone Repository
        if (toolCall.startsWith("TOOL:cloneRepo:")) {

            String repoUrl =
                    toolCall.replace(
                            "TOOL:cloneRepo:",
                            "");

            String repoDir =
                    tools.cloneRepo(repoUrl);

            session.setRepoDir(repoDir);
            return
             """
                    Repository cloned successfully.
                    
                    Repository:
                    %s
                    
                    Local Path:
                    %s
                    """
                    .formatted(repoUrl, repoDir);
        }

        // Find Root Pom
        if (toolCall.startsWith("TOOL:findRootPom")) {

            String pomPath =
                    tools.findRootPom(
                            session.getRepoDir());

            session.setPomPath(pomPath);

            return """
                    Root pom.xml found.
                    
                    Path:
                    %s
                    """
                    .formatted(pomPath);
        }

        // Find Child Poms
        if (toolCall.startsWith("TOOL:findChildPoms")) {

            return tools.findChildPoms(
                            session.getRepoDir())
                    .toString();
        }

        // Read Pom
        if (toolCall.startsWith("TOOL:readPom")) {

            return tools.readPom(
                    session.getPomPath());
        }

        // Save Pom
        if (toolCall.startsWith("TOOL:savePom:")) {

            String payload = toolCall.replace(
                            "TOOL:savePom:",
                            "");

            String[] parts = payload.split("\\|", 2);

            String pomPath = parts[0];

            String content = parts[1];

            return tools.savePom(
                    pomPath,
                    content);
        }

        // Create Branch
        if (toolCall.startsWith("TOOL:createBranch:")) {

            String branchName =
                    toolCall.replace(
                            "TOOL:createBranch:",
                            "");

            session.setBranchName(
                    branchName);

            return tools.createBranch(
                    session.getRepoDir(),
                    branchName);
        }

        // Push Changes
        if (toolCall.startsWith("TOOL:pushChanges:")) {

            String payload =
                    toolCall.replace(
                            "TOOL:pushChanges:",
                            "");

            String[] parts =
                    payload.split("\\|", 2);

            String branchName =
                    parts[0];

            String commitMessage =
                    parts[1];

            return tools.pushChanges(
                    session.getRepoDir(),
                    branchName,
                    commitMessage);
        }

        // Create PR
        if (toolCall.startsWith("TOOL:createPR:")) {

            String payload =
                    toolCall.replace(
                            "TOOL:createPR:",
                            "");

            String[] parts =
                    payload.split("\\|");

            String owner =
                    parts[0];

            String repo =
                    parts[1];

            String branchName =
                    parts[2];

            return tools.createPR(
                    owner,
                    repo,
                    branchName);
        }

        // Cleanup Repository
        if (toolCall.startsWith("TOOL:cleanupRepository")) {

            String result =
                    tools.cleanupRepository(
                            session.getRepoDir());

            session.setRepoDir(null);
            session.setPomPath(null);
            session.setBranchName(null);
            return result;
        }
        return "Unknown Tool : " + toolCall;
    }
}