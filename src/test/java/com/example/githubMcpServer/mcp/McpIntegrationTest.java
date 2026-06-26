package com.example.githubMcpServer.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpIntegrationTest {

    private McpSyncClient createClient() {

        ServerParameters params =
                ServerParameters.builder("java")
                        .args(
                                "-Dspring.ai.mcp.server.stdio=true",
                                "-jar",
                                "target/githubMcpServer-0.0.1-SNAPSHOT.jar")
                        .build();

        return McpClient.sync(
                        new StdioClientTransport(
                                params,
                                McpJsonDefaults.getMapper()))
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }
    private String extractText(McpSchema.CallToolResult result) {

        return result.content()
                .stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining())
                .trim()
                .replace("\"", "")
                .replace("\\\\", "\\");
    }

    @Test
    void toolsAreAvailable() {

        try (var client = createClient()) {

            client.initialize();

            Set<String> actual = client.listTools()
                    .tools()
                    .stream()
                    .map(McpSchema.Tool::name)
                    .collect(Collectors.toSet());

            Set<String> expected = Set.of(
                    "cloneRepo",
                    "findRootPom",
                    "findChildPoms",
                    "readPom",
                    "savePom",
                    "createBranch",
                    "pushChanges",
                    "createPR",
                    "cleanupRepository"
            );

            assertEquals(expected, actual);
        }
    }
    @Test
    void readPomWorks() throws Exception {

        Path pom = Files.createTempFile("test", ".xml");

        Files.writeString(
                pom,
                "<project>hello</project>");

        try (var client = createClient()) {

            client.initialize();

            var result =
                    client.callTool(
                            McpSchema.CallToolRequest.builder()
                                    .name("readPom")
                                    .arguments(
                                            Map.of(
                                                    "pomPath",
                                                    pom.toString()))
                                    .build());

            String text = extractText(result);

            assertEquals("<project>hello</project>", text);
        }
    }

    @Test
    void readPomMissingFile() {

        try (var client = createClient()) {

            client.initialize();

            var result =
                    client.callTool(
                            McpSchema.CallToolRequest.builder()
                                    .name("readPom")
                                    .arguments(
                                            Map.of(
                                                    "pomPath",
                                                    "missing.xml"))
                                    .build());

            assertTrue(result.isError());

            String text = extractText(result);

            assertEquals("Pom file not found: missing.xml", text);
        }
    }

    @Test
    void findChildPomWorks() throws Exception {

        Path repo =
                Files.createTempDirectory("repo");

        Files.writeString(
                repo.resolve("pom.xml"),
                "<root/>");

        Path child =
                Files.createDirectories(
                        repo.resolve("module"));

        Files.writeString(
                child.resolve("pom.xml"),
                "<child/>");

        try (var client = createClient()) {

            client.initialize();

            var result =
                    client.callTool(
                            McpSchema.CallToolRequest.builder()
                                    .name("findChildPoms")
                                    .arguments(
                                            Map.of(
                                                    "repoDir",
                                                    repo.toString()))
                                    .build());

            String output =
                    extractText(result);

            assertEquals(
                    List.of(child.resolve("pom.xml").toAbsolutePath().toString()).toString(),
                    output
            );        }
    }


    @Test
    void findRootPomWorks() throws Exception {

        Path repo =
                Files.createTempDirectory("repo");

        Path root =
                repo.resolve("pom.xml");

        Files.writeString(root, "<root/>");

        try (var client = createClient()) {
            client.initialize();
            var result =
                    client.callTool(
                            McpSchema.CallToolRequest.builder()
                                    .name("findRootPom")
                                    .arguments(
                                            Map.of(
                                                    "repoDir",
                                                    repo.toString()))
                                    .build());

            String output = extractText(result);
            assertEquals(root.toAbsolutePath().toString(), output);
        }
    }

    @Test
    void savePomWorks() throws Exception {

        Path pom = Files.createTempFile("pom",".xml");

        Files.writeString(pom,"old");

        try(var client=createClient()) {

            client.initialize();

            client.callTool(

                    McpSchema.CallToolRequest.builder()

                            .name("savePom")

                            .arguments(Map.of(

                                    "pomPath",pom.toString(),

                                    "content","new"))

                            .build());

        }

        assertEquals("new", Files.readString(pom));
    }

}