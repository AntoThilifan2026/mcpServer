package com.example.githubMcpServer.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;

@Service
public class GitHubService {

    @Value("${github.token}")
    private String token;

    private final String USERNAME = "AntoThilifan2026";

    public void cloneRepo(String repoUrl, File dir) throws Exception {

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(dir)
                .setBranch("main")
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(USERNAME, token)
                )
                .call();
    }

    public void createBranch(File dir, String branch) throws Exception {

        Git git = Git.open(dir);

        git.checkout()
                .setCreateBranch(true)
                .setName(branch)
                .call();
    }

    public void commitAndPush(File dir, String branch, String message) throws Exception {

        Git git = Git.open(dir);

        git.add().addFilepattern(".").call();

        git.commit().setMessage(message).call();

        git.push()
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(USERNAME, token)
                )
                .call();
    }
}