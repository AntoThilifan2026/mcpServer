package com.example.githubMcpServer.mcp;

import com.example.githubMcpServer.service.GitHubService;
import com.example.githubMcpServer.service.PullRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolsTest {

    @TempDir
    Path tempDir;

    @Mock
    private GitHubService git;

    @Mock
    private PullRequestService pr;

    @InjectMocks
    private McpTools tools;

    @BeforeEach
    void setup() {
        tools = new McpTools(git, pr);
    }

    @Test
    void readPomReturnsFileContent() throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, "<project>test</project>");

        String result = tools.readPom(pom.toString());

        assertEquals("<project>test</project>", result);
    }

    @Test
    void readPomThrowsWhenMissing() {
        Path missing = tempDir.resolve("missing.xml");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> tools.readPom(missing.toString())
        );

        assertTrue(ex.getMessage().contains("Pom file not found"));
    }

    @Test
    void savePomWritesFile() throws Exception {
        Path pom = tempDir.resolve("pom.xml");

        String result = tools.savePom(pom.toString(), "<new/>");

        assertEquals("POM updated successfully", result);
        assertEquals("<new/>", Files.readString(pom));
    }

    @Test
    void savePomAllowsWritingAnywhereCurrently() throws Exception {
        Path randomFile = tempDir.resolve("outside.xml");

        tools.savePom(randomFile.toString(), "<unsafe/>");

        assertEquals("<unsafe/>", Files.readString(randomFile));
    }


    @Test
    void findRootPomReturnsAnyPomFound() throws Exception {
        Path rootPom = tempDir.resolve("pom.xml");
        Path childDir = tempDir.resolve("child");

        Files.createDirectories(childDir);
        Files.writeString(rootPom, "<root/>");
        Files.writeString(childDir.resolve("pom.xml"), "<child/>");

        String result = tools.findRootPom(tempDir.toString());

        assertTrue(
                result.equals(rootPom.toString()) ||
                        result.equals(childDir.resolve("pom.xml").toString())
        );
    }

    @Test
    void findRootPomThrowsIfMissing() {
        assertThrows(RuntimeException.class,
                () -> tools.findRootPom(tempDir.toString()));
    }

    // ✅ findChildPoms
    @Test
    void findChildPomsReturnsOnlyChildPoms() throws Exception {
        Path rootPom = tempDir.resolve("pom.xml");
        Path module = tempDir.resolve("module");

        Files.createDirectories(module);
        Files.writeString(rootPom, "<root/>");
        Files.writeString(module.resolve("pom.xml"), "<child/>");

        List<String> result = tools.findChildPoms(tempDir.toString());

        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("module"));
    }

    // ✅ delegation
    @Test
    void createBranchDelegatesToGitService() throws Exception {
        tools.createBranch(tempDir.toString(), "feature/test");

        verify(git).createBranch(new File(tempDir.toString()), "feature/test");
    }

    @Test
    void pushChangesDelegates() throws Exception {
        tools.pushChanges(tempDir.toString(), "feature/test", "msg");

        verify(git).commitAndPush(
                new File(tempDir.toString()),
                "feature/test",
                "msg"
        );
    }

    @Test
    void createPRDelegates() {
        when(pr.createPR("owner", "repo", "branch"))
                .thenReturn("ok");

        String result = tools.createPR("owner", "repo", "branch");

        assertEquals("ok", result);
    }

    // ✅ cleanup
    @Test
    void cleanupDeletesDirectory() throws Exception {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "data");

        tools.cleanupRepository(tempDir.toString());

        assertFalse(Files.exists(tempDir));
    }

    // 🔥 dangerous behaviour test
    @Test
    void cleanupCanDeleteAnyFolderCurrently() throws Exception {
        Path dir = Files.createTempDirectory("danger");
        Files.writeString(dir.resolve("file.txt"), "data");

        tools.cleanupRepository(dir.toString());

        assertFalse(Files.exists(dir));
    }
}