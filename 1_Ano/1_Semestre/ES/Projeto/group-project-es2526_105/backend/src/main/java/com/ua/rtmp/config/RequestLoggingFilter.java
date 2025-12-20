package com.ua.rtmp.config;

import com.ua.rtmp.util.LoggingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestId = LoggingUtil.generateRequestId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        try {
            LoggingUtil.setRequestContext(requestId, authentication);
            
            log.info("Incoming request: method={}, uri={}, remoteAddr={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
            
            filterChain.doFilter(request, response);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed: method={}, uri={}, status={}, duration={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Request failed: method={}, uri={}, duration={}ms, error={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    e.getMessage(),
                    e);
            throw e;
        } finally {
            LoggingUtil.clearAllContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || 
               path.startsWith("/actuator/info") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
