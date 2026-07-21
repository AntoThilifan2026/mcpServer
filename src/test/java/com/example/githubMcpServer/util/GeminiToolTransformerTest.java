package com.example.githubMcpServer.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiToolTransformerTest {

    @Test
    void transformsDiscoveredMcpToolsIntoGeminiFunctionDeclarations() {
        McpTool tool = new McpTool(
                "readPom",
                "Read pom.xml content",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pomPath", Map.of("type", "string")),
                        "required", List.of("pomPath")));

        List<Map<String, Object>> result =
                new GeminiToolTransformer().transform(List.of(tool));

        Map<?, ?> declarations = (Map<?, ?>) result.get(0);
        List<?> functions = (List<?>) declarations.get("functionDeclarations");
        Map<?, ?> function = (Map<?, ?>) functions.get(0);

        assertEquals("readPom", function.get("name"));
        assertEquals("Read pom.xml content", function.get("description"));
        assertEquals(tool.inputSchema(), function.get("parameters"));
    }
}
