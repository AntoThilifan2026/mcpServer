package com.example.githubMcpServer.util;

import com.example.githubMcpServer.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StructuredMessageTest {

    @Test
    void conversationPreservesStructuredMessagesInOrder() {
        ConversationMemory memory = new ConversationMemory();

        memory.add(new TextMessage(Role.SYSTEM, "system"));
        memory.add(new TextMessage(Role.USER, "user"));
        memory.add(new TextMessage(Role.ASSISTANT, "assistant"));
        memory.add(new ToolCall("call-1", "readPom", "{\"pomPath\":\"pom.xml\"}"));
        memory.add(new ToolResult("call-1", "<project/>"));

        List<Message> messages = memory.getMessages();

        assertInstanceOf(TextMessage.class, messages.get(0));
        assertEquals(Role.SYSTEM, ((TextMessage) messages.get(0)).role());
        assertEquals(Role.USER, ((TextMessage) messages.get(1)).role());
        assertEquals(Role.ASSISTANT, ((TextMessage) messages.get(2)).role());
        assertInstanceOf(ToolCall.class, messages.get(3));
        assertInstanceOf(ToolResult.class, messages.get(4));
    }

    @Test
    void toolResultCanBeCorrelatedWithToolCall() {
        ToolCall call = new ToolCall("call-42", "readPom", "{}");
        ToolResult result = new ToolResult("call-42", "result");

        assertEquals(call.id(), result.toolCallId());
    }

    @Test
    void structuredMessagesRemainStructuredAtLlmApiBoundary() {
        GeminiService service = new GeminiService(
                new ThrottlingExecutor(),
                null,
                new GeminiToolTransformer());

        Object apiMessages = ReflectionTestUtils.invokeMethod(
                service,
                "buildPrompt",
                List.of(
                        new TextMessage(Role.SYSTEM, "system"),
                        new TextMessage(Role.USER, "user"),
                        new ToolCall("call-1", "readPom", "{}"),
                        new ToolResult("call-1", "result")));

        assertInstanceOf(
                List.class,
                apiMessages,
                "The LLM boundary must retain a structured list instead of flattening messages into a String");
    }
}
