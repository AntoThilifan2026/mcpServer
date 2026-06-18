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

            // ✅ Start MCP server
            ServerParameters params = ServerParameters.builder("java")
                    .args(
                            "-Dspring.ai.mcp.server.stdio=true",
                            "-jar",
                            "C://Users//antothilifanr.bibia//OneDrive - HCL TECHNOLOGIES LIMITED//Desktop//GithubMcpServer//githubMcpServer//target//githubMcpServer-0.0.1-SNAPSHOT.jar"
                    )
                    .build();

            // ✅ Correct transport
            var transport = new StdioClientTransport(
                    params,
                    McpJsonDefaults.getMapper()
            );

            // ✅ Correct client
            McpSyncClient client = io.modelcontextprotocol.client.McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();

            // ✅ Initialize
            client.initialize();

            System.out.println("✅ Connected to MCP Server");

            // ✅ List tools
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

            if (input.contains("clone")) {

                var result = client.callTool(
                        McpSchema.CallToolRequest
                                .builder()
                                .name("cloneRepo")
                                .arguments(Map.of(
                                        "repoUrl",
                                        extractUrl(input)
                                ))
                                .build()
                );

                System.out.println(result);
            }

            else if (input.contains("find")) {

                var result = client.callTool(
                        McpSchema.CallToolRequest.builder().name("findPom").build()
                );

                System.out.println(result);
            }

            else if (input.contains("fix")) {

                var result = client.callTool(
                        McpSchema.CallToolRequest.builder().name("fixPom").build()
                );

                System.out.println(result);
            }

            else if (input.contains("pr")) {

                var result = client.callTool(
                        McpSchema.CallToolRequest.builder().name("createPR")
                                .arguments(Map.of(
                                        "owner", "AntoThilifan2026",
                                        "repo", "mcp-demo"
                                ))
                                .build()
                );

                System.out.println(result);
            }

            else {
                System.out.println("❌ Unknown command");
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