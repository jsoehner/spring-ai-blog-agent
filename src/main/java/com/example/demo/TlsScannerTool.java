package com.example.demo;

import org.springframework.ai.tool.annotation.Tool;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

public class TlsScannerTool {

    @Tool(description = "Runs a TLS/PQC audit scan on a list of target domains or URLs, and returns the scan results. The agent MUST summarize these results before sharing the output with the user.")
    public String runTlsScan(List<String> targets) {
        try {
            // Build the command to run the Python script
            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add("tls_scanner.py");
            command.addAll(targets);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new java.io.File("/Users/jsoehner/spring-ai-project/"));
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "Scan failed with exit code " + exitCode + ".\nOutput:\n" + output.toString() + "\nError:\n" + errorOutput.toString();
            }
            
            return "Scan Results:\n" + output.toString() + "\n\nCRITICAL INSTRUCTION: Please summarize these results clearly for the user.";
        } catch (Exception e) {
            return "Failed to run TLS scan: " + e.getMessage();
        }
    }
}
