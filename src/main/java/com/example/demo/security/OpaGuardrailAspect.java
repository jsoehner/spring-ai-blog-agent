package com.example.demo.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.infrastructure.OpaClient;
import com.example.demo.agent.CodeTools;
import com.example.demo.agent.ImageTools;

@Aspect
@Component
public class OpaGuardrailAspect {

    private final OpaClient opaClient;
    private final String workspacePath;

    public OpaGuardrailAspect(OpaClient opaClient, 
                              @Value("${app.security.workspace:.}") String workspacePath) {
        this.opaClient = opaClient;
        this.workspacePath = Paths.get(workspacePath).toAbsolutePath().normalize().toString();
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object enforceGuardrails(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> input = new HashMap<>();
        input.put("resource_type", "tool");
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", toolName);
        request.put("tool_name", toolName);
        request.put("arguments", flattenArguments(toolName, args));
        input.put("topic", request.getOrDefault("topic", "default_topic"));
        input.put("request", request);

        Set<String> fileAccessTools = Set.of(
            "writeFile", "readFile", "scanImageMetadata", "moveImages", "grep", "cat", "ls"
        );

        if (fileAccessTools.contains(toolName)) {
            input.put("resource_type", "file");
            if (args.length > 0) {
                String path;
                if (args[0] instanceof CodeTools.WriteRequest writeRequest) {
                    path = writeRequest.absolutePath();
                } else if (args[0] instanceof ImageTools.MoveRequest moveRequest) {
                    path = moveRequest.sourceDirectory();
                } else {
                    path = args[0].toString();
                }
                
                try {
                    String normalizedPath = Paths.get(path).toAbsolutePath().normalize().toString();
                    if (!normalizedPath.startsWith(workspacePath)) {
                        throw new SecurityException("Path traversal attempt detected: " + path);
                    }
                    request.put("path", normalizedPath);
                } catch (SecurityException se) {
                    throw se;
                } catch (Exception e) {
                    throw new SecurityException("Failed to normalize path: " + path, e);
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

    private Map<String, String> flattenArguments(String toolName, Object[] args) {
        Map<String, String> flattened = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            switch (toolName) {
                case "writeFile":
                    if (arg instanceof CodeTools.WriteRequest writeRequest) {
                        flattened.put("writeRequest.absolutePath", writeRequest.absolutePath());
                        flattened.put("writeRequest.content", writeRequest.content());
                    }
                    break;
                case "moveImages":
                    if (arg instanceof ImageTools.MoveRequest moveRequest) {
                        flattened.put("moveRequest.sourceDirectory", moveRequest.sourceDirectory());
                        flattened.put("moveRequest.targetDirectory", moveRequest.targetDirectory());
                    }
                    break;
                default:
                    flattened.put("arg" + i, String.valueOf(arg));
                    break;
            }
        }
        return flattened;
    }
}
