package com.mas.gov.bt.mas.primary.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.EnumSet;

/**
 * Registers {@link HstsFilter} explicitly. The filter is deliberately not a {@code @Component} -
 * Spring Boot would auto-register it a second time, unordered, alongside this one.
 *
 * <p>Two things need to be true here and neither is the default:
 *
 * <p><b>Order.</b> An auto-registered filter bean sits at {@code LOWEST_PRECEDENCE}, behind
 * springSecurityFilterChain (order -100). Spring Security does not call {@code chain.doFilter} on a
 * 401/403, so the headers never reached the responses an unauthenticated caller sees.
 *
 * <p><b>Dispatcher types.</b> Filter registration defaults to {@code REQUEST} only. A 404 or a 500
 * is delivered through the container's ERROR dispatch, which the filter therefore never saw, and
 * the reset preceding that dispatch discards whatever the REQUEST pass had set. Verified against
 * the running cluster: services behind Spring Security returned the headers on a 401, while a 404
 * came back bare.
 */
@Configuration
public class HstsFilterConfig {

    @Bean
    public FilterRegistrationBean<HstsFilter> hstsFilterRegistration() {
        FilterRegistrationBean<HstsFilter> registration = new FilterRegistrationBean<>(new HstsFilter());
        registration.setDispatcherTypes(EnumSet.of(
                DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
