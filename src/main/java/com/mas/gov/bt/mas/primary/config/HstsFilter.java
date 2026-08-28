package com.mas.gov.bt.mas.primary.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Must run ahead of springSecurityFilterChain (order -100). An unordered @Component
// Filter defaults to LOWEST_PRECEDENCE, which puts it behind Spring Security — and on a
// 401/403 Spring Security never calls chain.doFilter, so these headers were never written
// on exactly the responses an unauthenticated client (or a scanner) sees.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HstsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'; " +
                            "object-src 'none'; base-uri 'self'");
            // Remove server information headers
            httpResponse.setHeader("Server", "Unknown");
            httpResponse.setHeader("X-Powered-By", "");
            // Add Cache-Control for sensitive endpoints
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");

        }
        chain.doFilter(request, response);
    }
}
