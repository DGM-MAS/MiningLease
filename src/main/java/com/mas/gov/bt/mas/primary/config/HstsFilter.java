package com.mas.gov.bt.mas.primary.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class HstsFilter implements Filter {

    /**
     * What every API response gets, Spring's HTML error pages included. These services render no UI
     * of their own, so 'unsafe-inline' was never needed here - the old policy was copied from the
     * frontend's, where it is. ZAP only ever caught this on backend-ndi, because that is the one
     * service whose 4xx bodies are text/html; the other eight carried the identical weak policy
     * unreported behind JSON error bodies.
     *
     * form-action, frame-ancestors and base-uri are spelled out because none of them falls back to
     * default-src - omitting them is the same as allowing anything, which is the second thing ZAP
     * reported.
     */
    private static final String CSP_API =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'; sandbox";

    /**
     * springdoc is on the classpath here and only the prod profile switches it off - and only in
     * some of these services - so Swagger UI may well be live. It is a real HTML page that injects
     * inline styles and calls back to /v3/api-docs, so CSP_API would break it. Scoped to its own
     * paths so the strict policy still covers the entire API surface.
     */
    private static final String CSP_SWAGGER =
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
                    + "form-action 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'";

    private static boolean isSwaggerUi(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            // Not an HTTP request, so there is no path to match and nothing that renders.
            // Fall through to the strict policy.
            return false;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        String context = httpRequest.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/");
    }

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
                    isSwaggerUi(request) ? CSP_SWAGGER : CSP_API);
            // Remove server information headers
            httpResponse.setHeader("Server", "Unknown");
            // Add Cache-Control for sensitive endpoints
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");

        }
        chain.doFilter(request, response);
    }
}
