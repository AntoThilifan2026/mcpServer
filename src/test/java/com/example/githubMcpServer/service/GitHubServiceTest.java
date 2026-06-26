package com.example.githubMcpServer.service;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createBranchWorks() throws Exception {
        Path repo = tempDir.resolve("repoTemporary");

        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            Files.writeString(repo.resolve("file.txt"), "data");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").call();
        }

        GitHubService service = new GitHubService();
        service.createBranch(repo.toFile(), "test-branch");

        try (Git git = Git.open(repo.toFile())) {
            assertEquals("test-branch", git.getRepository().getBranch());
        }
    }

    @Test
    void commitAndPushCreatesCommitLocally() throws Exception {

        Path repo = tempDir.resolve("repo");
        Path remote = tempDir.resolve("remote.git");

        try (Git ignored = Git.init()
                .setDirectory(remote.toFile())
                .setBare(true)
                .call()) {
        }

        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {

            git.remoteAdd()
                    .setName("origin")
                    .setUri(new org.eclipse.jgit.transport.URIish(remote.toUri().toString()))
                    .call();

            Files.writeString(repo.resolve("file.txt"), "data");

            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").call();

            git.checkout()
                    .setCreateBranch(true)
                    .setName("branch")
                    .call();

            Files.writeString(repo.resolve("file.txt"), "change");
        }

        GitHubService service = new GitHubService();
        ReflectionTestUtils.setField(service, "token", "dummy");

        service.commitAndPush(repo.toFile(), "branch", "update");

        try (Git git = Git.open(repo.toFile())) {
            String msg = git.log().call().iterator().next().getFullMessage();
            assertEquals("update", msg);
        }
    }
}