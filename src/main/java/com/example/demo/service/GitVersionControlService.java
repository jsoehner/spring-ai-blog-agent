package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class GitVersionControlService implements VersionControlService {

    private static final Pattern SAFE_STRING = Pattern.compile("^[a-zA-Z0-9\\s\\-_\\.]+$\");

    @Override
    public void createBranch(String branchName) throws Exception {
        validateString(branchName);
        runCommand(List.of("git", "checkout", "-b", branchName));
    }

    @Override
    public void commit(String message) throws Exception {
        validateString(message);
        runCommand(List.of("git", "commit", "-m", message));
    }

    @Override
    public void push(String branchName) throws Exception {
        validateString(branchName);
        runCommand(List.of("git", "push", "-u", "origin", branchName));
    }

    @Override
    public void createPullRequest(String title, String body) throws Exception {
        validateString(title);
        // Body is usually longer and might contain more characters, but we still want to be careful.
        // For now, we allow more characters but strip shell metacharacters.
        String safeBody = body.replaceAll("[;&|><\\$\\\]\\*\\?\\!]", "");
        runCommand(List.of("gh", "pr", "create", "--title", title, "--body", safeBody));
    }

    @Override
    public void addFiles(List<String> filePaths) throws Exception {
        for (String filePath : filePaths) {
            // Paths are generally safe as they are used as individual arguments, but we should ensure they don't contain spaces/newlines if possible.
            runCommand(List.of("git", "add", "-f", filePath));
        }
    }

    private void validateString(String input) {
        if (!SAFE_STRING.matcher(input).matches()) {
            throw new IllegalArgumentException("Invalid input: " + input + ". Only alphanumeric, spaces, hyphens, underscores, and dots are allowed.");
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
            // Use a safer way to log the command without joining strings into a single command string.
            throw new RuntimeException("Command failed with exit code " + exitCode + ". Command arguments: " + cmd);
        }
    }
}
