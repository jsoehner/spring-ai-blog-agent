package com.example.demo.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class OpaGuardrailAspect {

    private final OpaClient opaClient;

    public OpaGuardrailAspect(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object enforceGuardrails(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Build generic OPA input (customize this mapping based on the specific policy needs)
        Map<String, Object> input = new HashMap<>();
        input.put("resource_type", "tool");
        input.put("topic", request.getOrDefault("topic", "default_topic"));
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", toolName);
        request.put("tool_name", toolName);
        request.put("arguments", args);
        input.put("request", request);

        // Specific handling for file writes based on agent_files.rego
        if ("writeFile".equals(toolName) || "readFile".equals(toolName) || "scanImageMetadata".equals(toolName) || "moveImages".equals(toolName)) {
            input.put("resource_type", "file");
            if (args.length > 0) {
                String path;
                if (args[0] instanceof com.example.demo.CodeTools.WriteRequest writeRequest) {
                    path = writeRequest.absolutePath();
                } else if (args[0] instanceof com.example.demo.ImageTools.MoveRequest moveRequest) {
                    path = moveRequest.sourceDirectory();
                } else {
                    path = args[0].toString();
                }
                
                // Securely normalize the path before sending it to OPA to prevent path traversal bypass
                try {
                    String normalizedPath = java.nio.file.Paths.get(path).toAbsolutePath().normalize().toString();
                    request.put("path", normalizedPath);
                } catch (Exception e) {
                    throw new SecurityException("Failed to normalize path: " + path);
                }
                
                request.put("action", ("writeFile".equals(toolName) || "moveImages".equals(toolName)) ? "write" : "read");
            }
        }

        boolean allowed = opaClient.evaluatePolicy(input);

        if (!allowed) {
            throw new SecurityException("Guardrail Violation: OPA denied execution for tool '" + toolName + "'");
        }

        return joinPoint.proceed();
    }
}
