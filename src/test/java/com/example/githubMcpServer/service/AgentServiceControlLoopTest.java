package com.example.githubMcpServer.service;

import com.example.githubMcpServer.mcp.McpTools;
import com.example.githubMcpServer.util.ConversationMemory;
import com.example.githubMcpServer.util.GeminiToolTransformer;
import com.example.githubMcpServer.util.Message;
import com.example.githubMcpServer.util.Role;
import com.example.githubMcpServer.util.SessionContext;
import com.example.githubMcpServer.util.TextMessage;
import com.example.githubMcpServer.util.ThrottlingExecutor;
import com.example.githubMcpServer.util.ToolCall;
import com.example.githubMcpServer.util.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentServiceControlLoopTest {

    @Test
    void successfulTextToolCallContinuesToFinalAssistantResponse() throws Exception {
        RecordingGemini gemini = new RecordingGemini(
                new TextMessage(Role.ASSISTANT, "TOOL:readPom"),
                new TextMessage(Role.ASSISTANT, "analysis complete"));
        RecordingTools tools = new RecordingTools();
        ConversationMemory memory = new ConversationMemory();
        SessionContext session = new SessionContext();
        session.setPomPath("pom.xml");

        String response = service(gemini, tools, memory, session).process("analyze");

        assertEquals("analysis complete", response);
        assertEquals(1, tools.readPomCalls);
        assertEquals(2, gemini.calls);
        assertInstanceOf(ToolResult.class, memory.getMessages().get(1));
        assertEquals(Role.ASSISTANT,
                ((TextMessage) memory.getMessages().get(2)).role());
    }

    @Test
    void nativeToolCallIsExecutedAndCorrelatedBeforeFinalResponse() {
        RecordingGemini gemini = new RecordingGemini(
                new ToolCall("call-42", "readPom", "{\"pomPath\":\"pom.xml\"}"),
                new TextMessage(Role.ASSISTANT, "analysis complete"));
        RecordingTools tools = new RecordingTools();
        ConversationMemory memory = new ConversationMemory();

        String response = assertDoesNotThrow(() ->
                service(gemini, tools, memory, new SessionContext()).process("analyze"));

        assertEquals("analysis complete", response);
        ToolResult result = memory.getMessages().stream()
                .filter(ToolResult.class::isInstance)
                .map(ToolResult.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("call-42", result.toolCallId());
    }

    @Test
    void invalidToolNameIsNotAcceptedByPrefixMatching() throws Exception {
        RecordingGemini gemini = new RecordingGemini(
                new TextMessage(Role.ASSISTANT, "TOOL:readPomUnexpected"),
                new TextMessage(Role.ASSISTANT, "handled invalid tool"));
        RecordingTools tools = new RecordingTools();

        service(gemini, tools, new ConversationMemory(), new SessionContext())
                .process("analyze");

        assertEquals(0, tools.readPomCalls,
                "An invalid tool name must not invoke readPom through prefix matching");
    }

    @Test
    void malformedToolParametersProduceAToolErrorInsteadOfEscapingTheLoop() {
        RecordingGemini gemini = new RecordingGemini(
                new TextMessage(Role.ASSISTANT, "TOOL:savePom:only-a-path"),
                new TextMessage(Role.ASSISTANT, "parameter error reported"));

        String response = assertDoesNotThrow(() ->
                service(gemini, new RecordingTools(), new ConversationMemory(), new SessionContext())
                        .process("save"));

        assertEquals("parameter error reported", response);
    }

    @Test
    void toolExecutionFailureIsReturnedToLlmAsCorrelatedToolResult() {
        RecordingGemini gemini = new RecordingGemini(
                new TextMessage(Role.ASSISTANT, "TOOL:readPom"),
                new TextMessage(Role.ASSISTANT, "tool failure reported"));
        RecordingTools tools = new RecordingTools();
        tools.readFailure = new IllegalStateException("read failed");

        String response = assertDoesNotThrow(() ->
                service(gemini, tools, new ConversationMemory(), new SessionContext())
                        .process("analyze"));

        assertEquals("tool failure reported", response);
        assertTrue(gemini.prompts.get(1).stream().anyMatch(message ->
                message instanceof ToolResult result && result.result().contains("read failed")));
    }

    @Test
    void textualToolRequestDoesNotBypassConfiguredMcpTransport() throws Exception {
        RecordingGemini gemini = new RecordingGemini(
                new TextMessage(Role.ASSISTANT, "TOOL:readPom"),
                new TextMessage(Role.ASSISTANT, "done"));
        RecordingTools tools = new RecordingTools();

        service(gemini, tools, new ConversationMemory(), new SessionContext())
                .process("analyze");

        assertEquals(0, tools.readPomCalls,
                "The control loop must invoke the tool through the MCP client/transport, not the local bean");
    }

    @Test
    void maximumControlLoopIterationsTerminate() throws Exception {
        Message[] responses = new Message[10];
        Arrays.fill(responses, new TextMessage(Role.ASSISTANT, "TOOL:notAvailable"));
        RecordingGemini gemini = new RecordingGemini(responses);
        ConversationMemory memory = new ConversationMemory();

        String response = service(
                gemini,
                new RecordingTools(),
                memory,
                new SessionContext()).process("analyze");

        assertEquals("Maximum iterations reached", response);
        assertEquals(10, gemini.calls);
        assertEquals(11, memory.getMessages().size());
    }

    @Test
    void unexpectedResponseTypeIsRejected() {
        RecordingGemini gemini = new RecordingGemini(
                new ToolResult("call-1", "unexpected"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service(
                        gemini,
                        new RecordingTools(),
                        new ConversationMemory(),
                        new SessionContext()).process("analyze"));

        assertEquals("Unexpected response type", exception.getMessage());
    }

    private AgentService service(
            RecordingGemini gemini,
            RecordingTools tools,
            ConversationMemory memory,
            SessionContext session) {

        return new AgentService(
                gemini,
                tools,
                memory,
                session,
                new StubAgentCardLoader());
    }

    private static class RecordingGemini extends GeminiService {

        private final Deque<Message> responses;
        private final List<List<Message>> prompts = new ArrayList<>();
        private int calls;

        RecordingGemini(Message... responses) {
            super(new ThrottlingExecutor(), null, new GeminiToolTransformer());
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public Message chat(List<Message> messages) {
            calls++;
            prompts.add(new ArrayList<>(messages));
            return responses.removeFirst();
        }
    }

    private static class RecordingTools extends McpTools {

        private int readPomCalls;
        private RuntimeException readFailure;

        RecordingTools() {
            super(null, null);
        }

        @Override
        public String readPom(String pomPath) {
            readPomCalls++;
            if (readFailure != null) {
                throw readFailure;
            }
            return "<project/>";
        }

        @Override
        public String savePom(String pomPath, String content) {
            return "POM updated successfully";
        }
    }

    private static class StubAgentCardLoader extends AgentCardLoader {
        @Override
        public String loadAgentCard() {
            return "system";
        }
    }
}
