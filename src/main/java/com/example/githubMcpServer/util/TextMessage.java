package com.example.githubMcpServer.util;

public record TextMessage(
        Role role,
        String content)
        implements Message {
}