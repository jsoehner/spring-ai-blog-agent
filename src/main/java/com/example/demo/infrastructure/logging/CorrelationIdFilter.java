package com.example.demo.infrastructure.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                           jakarta.servlet.ServletResponse response,
                           FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            String correlationId = ((HttpServletRequest) request).getHeader("correlationId");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put("correlationId", correlationId);
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove("correlationId");
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
