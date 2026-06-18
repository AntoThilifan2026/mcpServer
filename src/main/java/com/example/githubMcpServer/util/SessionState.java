package com.example.githubMcpServer.util;

import java.io.File;
import java.nio.file.Path;

class SessionState {
    File repoDir;
    Path pomPath;
    String content;
    String fixedContent;
    String branch;
}