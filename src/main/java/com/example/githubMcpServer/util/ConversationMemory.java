package com.example.githubMcpServer.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConversationMemory {

    private final List<String> messages =
            new ArrayList<>();

    public void add(String role, String content) {

        messages.add(
                role + ": " + content);
    }

    public String getConversation() {

        return String.join("\n", messages);
    }
}