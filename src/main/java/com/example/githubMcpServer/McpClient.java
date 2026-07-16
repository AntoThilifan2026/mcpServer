package com.example.githubMcpServer;


import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Map;
import java.util.Scanner;

public class McpClient {

    public static void main(String[] args) {

        try {
            ServerParameters params = ServerParameters.builder("java")
                    .args(
                            "-Dspring.ai.mcp.server.stdio=true",
                            "-jar",
                            "target/githubMcpServer-0.0.1-SNAPSHOT.jar"
                    )
                    .build();

            //  Correct transport
            var transport = new StdioClientTransport(
                    params,
                    McpJsonDefaults.getMapper()
            );

            //  Correct client
            McpSyncClient client = io.modelcontextprotocol.client.McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();

            //  Initialize
            client.initialize();

            System.out.println("✅ Connected to MCP Server");

            //  List tools
            System.out.println(client.listTools());

            Scanner sc = new Scanner(System.in);

            while (true) {

                System.out.print("\n> ");
                String input = sc.nextLine();

                if (input.equalsIgnoreCase("exit")) break;

                handle(client, input);
            }

            client.closeGracefully();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handle(McpSyncClient client, String input) {

            try {

                String[] parts = input.split(" ", 2);

                String command = parts[0];

                switch (command) {

                    case "clone" -> {

                        String repoUrl = parts[1];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("cloneRepo")
                                        .arguments(Map.of(
                                                "repoUrl", repoUrl
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }
                    case "findRootPom" -> {
                        String repoDir = parts[1];
                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("findRootPom")
                                        .arguments(Map.of(
                                                "repoDir", repoDir
                                        ))
                                        .build()
                        );
                        System.out.println(result);
                    }

                    case "findAllPoms" -> {

                        String repoDir = parts[1];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("findAllPoms")
                                        .arguments(Map.of(
                                                "repoDir", repoDir
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    case "readPom" -> {

                        String pomPath = parts[1];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("readPom")
                                        .arguments(Map.of(
                                                "pomPath", pomPath
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    case "createBranch" -> {

                        String[] args = parts[1].split(" ");

                        String repoDir = args[0];
                        String branchName = args[1];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("createBranch")
                                        .arguments(Map.of(
                                                "repoDir", repoDir,
                                                "branchName", branchName
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    case "pushChanges" -> {

                        String[] args = parts[1].split(" ", 3);

                        String repoDir = args[0];
                        String branchName = args[1];
                        String commitMessage = args[2];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("pushChanges")
                                        .arguments(Map.of(
                                                "repoDir", repoDir,
                                                "branchName", branchName,
                                                "commitMessage", commitMessage
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    case "createPR" -> {

                        String[] args = parts[1].split(" ");

                        String owner = args[0];
                        String repo = args[1];
                        String branchName = args[2];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("createPR")
                                        .arguments(Map.of(
                                                "owner", owner,
                                                "repo", repo,
                                                "branchName", branchName
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    case "cleanupRepository" -> {

                        String repoDir = parts[1];

                        var result = client.callTool(
                                McpSchema.CallToolRequest.builder()
                                        .name("cleanupRepository")
                                        .arguments(Map.of(
                                                "repoDir", repoDir
                                        ))
                                        .build()
                        );

                        System.out.println(result);
                    }

                    default -> System.out.println("Unknown command");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    private static String extractUrl(String input) {
        for (String word : input.split(" ")) {
            if (word.startsWith("http")) return word;
        }
        throw new RuntimeException("Repo URL missing");
    }
}