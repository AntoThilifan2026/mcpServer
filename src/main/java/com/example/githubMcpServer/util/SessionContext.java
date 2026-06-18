package com.example.githubMcpServer.util;

import java.io.File;
import java.nio.file.Path;

public class SessionContext {

    public File repoDir;
    public Path pomPath;
    public String originalContent;
    public String fixedContent;
    public String branchName;
}
