package com.example.githubMcpServer.util;

import java.util.Map;

public record GeminiFunctionDeclaration(
        String name,
        String description,
        Map<String,Object> parameters) {
}