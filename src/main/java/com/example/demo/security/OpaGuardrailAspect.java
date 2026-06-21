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
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", toolName);
        request.put("arguments", args);
        input.put("request", request);

        // Specific handling for file writes based on agent_files.rego
        if ("writeFile".equals(toolName) || "readFile".equals(toolName)) {
            input.put("resource_type", "file");
            if (args.length > 0) {
                // If it's a WriteRequest record or similar, you may need to extract the path.
                // Assuming arg[0] could be a string path or a record containing the path.
                String path = args[0].toString();
                request.put("path", path);
                request.put("action", "writeFile".equals(toolName) ? "write" : "read");
            }
        }

        boolean allowed = opaClient.evaluatePolicy(input);

        if (!allowed) {
            throw new SecurityException("Guardrail Violation: OPA denied execution for tool '" + toolName + "'");
        }

        return joinPoint.proceed();
    }
}
