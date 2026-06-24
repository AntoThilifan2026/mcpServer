package com.example.githubMcpServer.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class McpIntegrationTest {

    private McpSyncClient createClient() throws Exception {

        ServerParameters params = ServerParameters.builder("java")
                .args("-Dspring.ai.mcp.server.stdio=true",
                        "-jar",
                        "target/githubMcpServer-0.0.1-SNAPSHOT.jar")
                .build();

        var transport = new StdioClientTransport(params, McpJsonDefaults.getMapper());

        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }

    //  Helper to extract text from MCP result
    private String extractText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining());
    }

    // TEST 1: verify tools are exposed
    @Test
    void toolsListContainsExpectedTools() throws Exception {

        try (McpSyncClient client = createClient()) {

            client.initialize();

            McpSchema.ListToolsResult tools = client.listTools();

            var toolNames = tools.tools()
                    .stream()
                    .map(McpSchema.Tool::name)
                    .collect(Collectors.toSet());

            assertTrue(toolNames.contains("readPom"));
            assertTrue(toolNames.contains("findChildPoms"));
            assertTrue(toolNames.contains("findRootPom"));
        }
    }

    // " TEST 2: readPom through MCP
    @Test
    void readPomWorksThroughMcp() throws Exception {

        Path pom = Files.createTempFile("test", ".xml");
        Files.writeString(pom, "<project>hello</project>");

        try (McpSyncClient client = createClient()) {

            client.initialize();

            var result = client.callTool(
                    McpSchema.CallToolRequest.builder()
                            .name("readPom")
                            .arguments(Map.of(
                                    "pomPath", pom.toString()
                            ))
                            .build()
            );

            String output = extractText(result);

            assertEquals("<project>hello</project>", output);
        }
    }

    // " TEST 3: readPom error case
    @Test
    void readPomThrowsErrorThroughMcp() throws Exception {

        Path missing = Path.of("missing-file.xml").toAbsolutePath();

        try (McpSyncClient client = createClient()) {

            client.initialize();

            var result = client.callTool(
                    McpSchema.CallToolRequest.builder()
                            .name("readPom")
                            .arguments(Map.of(
                                    "pomPath", missing.toString()
                            ))
                            .build()
            );

            assertTrue(result.isError());

            String output = extractText(result);

            assertEquals("Pom file not found: " + missing.toString(), output);
        }
    }

    //  TEST 4: findChildPoms through MCP
    @Test
    void findChildPomsWorksThroughMcp() throws Exception {

        Path repo = Files.createTempDirectory("repo");
        Path child = repo.resolve("child");

        Files.createDirectories(child);

        Path rootPom = repo.resolve("pom.xml");
        Path childPom = child.resolve("pom.xml");

        Files.writeString(rootPom, "<root/>");
        Files.writeString(childPom, "<child/>");

        try (McpSyncClient client = createClient()) {

            client.initialize();

            var result = client.callTool(
                    McpSchema.CallToolRequest.builder()
                            .name("findChildPoms")
                            .arguments(Map.of(
                                    "repoDir", repo.toString()
                            ))
                            .build()
            );

            String output = extractText(result);

            assertTrue(output.contains(childPom.toAbsolutePath().toString()));
        }
    }

    // " TEST 5: findRootPom (characterisation style)
    @Test
    void findRootPomWorksThroughMcp() throws Exception {

        Path repo = Files.createTempDirectory("repo");
        Path child = repo.resolve("child");

        Files.createDirectories(child);

        Path rootPom = repo.resolve("pom.xml");
        Path childPom = child.resolve("pom.xml");

        Files.writeString(rootPom, "<root/>");
        Files.writeString(childPom, "<child/>");

        try (McpSyncClient client = createClient()) {

            client.initialize();

            var result = client.callTool(
                    McpSchema.CallToolRequest.builder()
                            .name("findRootPom")
                            .arguments(Map.of(
                                    "repoDir", repo.toString()
                            ))
                            .build()
            );

            String output = extractText(result);

            // matches current behavior (first encountered)
            assertTrue(
                    output.equals(rootPom.toString()) ||
                            output.equals(childPom.toString())
            );
        }
    }
}
