package com.example.mcpServer.service;

import org.apache.commons.io.FileUtils;
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

    @Value("${github.repo-url}")
    private String repoUrl;

    private final String LOCAL_DIR =
            "C://Users//antothilifanr.bibia//OneDrive - HCL TECHNOLOGIES LIMITED//Desktop//mcp-server//demo/";

//    private final String LOCAL_DIR = "C:/temp/mcp-repo/";

    private final String USERNAME = "AntoThilifan2026";

    // ✅ CLONE REPOSITORY
    public File cloneRepo() throws Exception {

        File dir = new File(LOCAL_DIR);


        // ✅ Clean old repo folder
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }

        System.out.println("Cloning repository...");


        return Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(dir)
                .setBranch("main")
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider("AntoThilifan2026", token)
                )
                .call()
                .getRepository()
                .getWorkTree();


}

    // ✅ CREATE NEW BRANCH
    public void createBranch(String branchName) throws Exception {

        Git git = Git.open(new File(LOCAL_DIR));

        System.out.println("Creating branch: " + branchName);

        git.checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .call();
    }

    // ✅ UPDATE FILE CONTENT (pom.xml)
    public void updateFile(String filePath, String content) throws Exception {

        System.out.println("Updating file: " + filePath);

        Files.write(new File(filePath).toPath(), content.getBytes());
    }

    // ✅ COMMIT + PUSH
    public void commitAndPush(String message) throws Exception {

        Git git = Git.open(new File(LOCAL_DIR));

        System.out.println("Staging files...");
        git.add().addFilepattern(".").call();

        System.out.println("Committing changes...");
        git.commit()
                .setMessage(message)
                .call();

        System.out.println("Pushing to GitHub...");

        git.push()
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(USERNAME, token)
                )
                .call();
    }
}