package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GitVersionControlService implements VersionControlService {

    @Override
    public void createBranch(String branchName) throws Exception {
        runCommand(List.of("git", "checkout", "-b", branchName));
    }

    @Override
    public void commit(String message) throws Exception {
        runCommand(List.of("git", "commit", "-m", message));
    }

    @Override
    public void push(String branchName) throws Exception {
        runCommand(List.of("git", "push", "-u", "origin", branchName));
    }

    @Override
    public void createPullRequest(String title, String body) throws Exception {
        runCommand(List.of("gh", "pr", "create", "--title", title, "--body", body));
    }

    @Override
    public void addFiles(List<String> filePaths) throws Exception {
        for (String filePath : filePaths) {
            runCommand(List.of("git", "add", "-f", filePath));
        }
    }

    private void runCommand(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Git/GH Output: " + line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + String.join(" ", cmd));
        }
    }
}
