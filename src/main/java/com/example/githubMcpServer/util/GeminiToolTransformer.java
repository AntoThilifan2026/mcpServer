package com.example.githubMcpServer.util;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiToolTransformer {

    public List<Map<String,Object>> transform(
            List<McpTool> mcpTools) {

        List<Map<String,Object>> declarations = new ArrayList<>();

        for (McpTool tool : mcpTools) {

            declarations.add(
                    Map.of(
                            "name",
                            tool.name(),

                            "description",
                            tool.description(),

                            "parameters",
                            tool.inputSchema()
                    ));
        }

        return List.of(
                Map.of("functionDeclarations", declarations
                ));
    }
}