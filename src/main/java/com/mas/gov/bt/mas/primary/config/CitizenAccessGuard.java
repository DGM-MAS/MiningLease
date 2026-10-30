package com.mas.gov.bt.mas.primary.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps citizen (applicant) tokens out of staff-only workflow endpoints.
 *
 * Staff and citizens authenticate against the same JWT secret, and these services only checked
 * that a token was valid — so a self-registered citizen could call the Director / Geologist /
 * MPCD / Mining-Engineer handlers and drive an application to "issued" (PT report 2026-10-09,
 * findings 5.2, 5.3, 5.4, 5.6, 5.9). The rule table below is first-match-wins: an ALLOW entry
 * carves a citizen-facing exception out of a broader DENY, anything unmatched is allowed (the
 * endpoint's own checks still apply).
 *
 * Enforced here, in the service, rather than only at the gateway, because the backend ports are
 * reachable directly.
 */
@Configuration
public class CitizenAccessGuard implements WebMvcConfigurer {

    private record Rule(boolean deny, String method, PathPattern pattern) {
        boolean matches(String m, String path) {
            return (method == null || method.equalsIgnoreCase(m)) && pattern.matches(
                    org.springframework.http.server.PathContainer.parsePath(path));
        }
    }

    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final List<Rule> RULES = new ArrayList<>();

    private static void deny(String pattern) { RULES.add(new Rule(true, null, PARSER.parse(pattern))); }
    private static void deny(HttpMethod m, String pattern) { RULES.add(new Rule(true, m.name(), PARSER.parse(pattern))); }
    private static void allow(String pattern) { RULES.add(new Rule(false, null, PARSER.parse(pattern))); }
    private static void allow(HttpMethod m, String pattern) { RULES.add(new Rule(false, m.name(), PARSER.parse(pattern))); }

    static {
        // ---- mining lease: staff workflow namespaces ----
        deny("/api/mining-lease/director/**");
        deny("/api/mining-lease/geologist/**");
        deny("/api/mining-lease/mpcd/**");
        deny("/api/mining-lease/mining-chief/**");
        deny("/api/mining-lease/mining-engineer/**");
        deny("/api/mining-lease/tasks/**");
        deny("/api/mining-lease/renewal/tasks/**");
        deny(HttpMethod.GET, "/api/mining-lease/geologistAssigned/**");
        deny("/api/mining-lease-renewal/**");
        // ---- restoration / EC renewal / sample clearance / closure / termination ----
        deny("/api/mine-restoration/director/**");
        deny("/api/mine-restoration/mining-engineer/**");
        deny("/api/mine-restoration/regional-coordinator/**");
        deny("/api/renewal-environmental-clearance/md/**");
        deny("/api/renewal-environmental-clearance/mi/**");
        deny("/api/renewal-environmental-clearance/mpcd/**");
        deny("/api/renewal-environmental-clearance/rc/**");
        deny("/api/sample-transport-clearance/gsd-chief/**");
        deny("/api/sample-transport-clearance/gsd-focal/**");
        deny("/api/temporary-closure/mi/**");
        deny("/api/temporary-closure/rc/**");
        deny("/api/temporary-closure/tasks/**");
        deny("/api/termination/cms-head/**");
        // ---- immediate suspension is raised by inspectors; the promoter only answers via /promoter ----
        allow("/api/immediate-suspension/promoter/**");
        deny("/api/immediate-suspension/**");
        // ---- manual entry is a back-office tool ----
        deny("/api/manual-mining/**");
        // ---- surface collection auction: only the promoter bank-guarantee flow is citizen-facing ----
        allow("/api/surface-collection-auction/bg/promoter/**");
        deny("/api/surface-collection-auction/mining-engineer-auction/**");
        deny("/api/surface-collection-auction/manual-entry/**");
        deny(HttpMethod.POST, "/api/surface-collection-auction/**");
        deny(HttpMethod.PUT, "/api/surface-collection-auction/**");
        deny(HttpMethod.PATCH, "/api/surface-collection-auction/**");
        deny(HttpMethod.DELETE, "/api/surface-collection-auction/**");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws IOException {
                if (!isCitizen()) {
                    return true;
                }
                String path = request.getRequestURI();
                String ctx = request.getContextPath();
                if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
                    path = path.substring(ctx.length());
                }
                for (Rule rule : RULES) {
                    if (rule.matches(request.getMethod(), path)) {
                        if (rule.deny()) {
                            reject(response);
                            return false;
                        }
                        return true;
                    }
                }
                return true;
            }
        }).addPathPatterns("/api/**", "/mpcd/**");
    }

    private static boolean isCitizen() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        return "CITIZEN".equalsIgnoreCase(jwt.getClaimAsString("userType"));
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"errorCode\":\"403\",\"message\":\"Access denied. The user does not have "
                + "the required permissions to access this resource.\",\"status\":\"Error\"}");
    }
}
