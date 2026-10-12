package com.mas.gov.bt.mas.primary.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitizenAccessGuardTest {

    static class Exposed extends InterceptorRegistry {
        List<Object> all() { return getInterceptors(); }
    }

    private int status(String userType, String method, String uri) throws Exception {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userType", userType)
                .claim("userId", 70L).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        Exposed reg = new Exposed();
        new CitizenAccessGuard().addInterceptors(reg);
        HandlerInterceptor in = (HandlerInterceptor) reg.all().get(0);
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRequestURI(uri);
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean ok = in.preHandle((HttpServletRequest) req, res, new Object());
        return ok ? 200 : res.getStatus();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void citizenBlockedOnStaffWorkflow() throws Exception {
        assertEquals(403, status("CITIZEN", "POST", "/api/mining-lease/director/assignTask"));
        assertEquals(403, status("CITIZEN", "POST", "/api/mining-lease/mining-engineer/applicationsMLA"));
        assertEquals(403, status("CITIZEN", "PUT", "/api/mining-lease/tasks/reassignME"));
        assertEquals(403, status("CITIZEN", "POST", "/api/surface-collection-auction"));
        assertEquals(403, status("CITIZEN", "PUT", "/api/surface-collection-auction/12/approve-ec"));
        assertEquals(403, status("CITIZEN", "POST", "/api/surface-collection-auction/mining-engineer-auction/review/12/issue-permit"));
        assertEquals(403, status("CITIZEN", "POST", "/api/immediate-suspension/review"));
        assertEquals(403, status("CITIZEN", "POST", "/api/mining-lease-renewal/director/review"));
    }

    @Test
    void citizenStillReachesOwnEndpoints() throws Exception {
        assertEquals(200, status("CITIZEN", "POST", "/api/mining-lease/applications"));
        assertEquals(200, status("CITIZEN", "GET", "/api/mining-lease/my-applications"));
        assertEquals(200, status("CITIZEN", "POST", "/api/mining-lease/renewal/applications"));
        assertEquals(200, status("CITIZEN", "POST", "/api/surface-collection-auction/bg/promoter/5/submit"));
        assertEquals(200, status("CITIZEN", "GET", "/api/surface-collection-auction/my-applications"));
        assertEquals(200, status("CITIZEN", "POST", "/api/immediate-suspension/promoter/review"));
    }

    @Test
    void staffTokenIsUntouched() throws Exception {
        assertEquals(200, status("Agency", "POST", "/api/mining-lease/director/assignTask"));
        assertEquals(200, status("Agency", "POST", "/api/surface-collection-auction"));
    }
}
