package com.reza.saas.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reza.saas.common.ApiError;

import java.io.IOException;

/**
 * Runs right after bearer-token authentication. Copies the tenant claim from the
 * verified JWT into {@link TenantContext} for the duration of the request, and
 * always clears it afterwards (Lambda reuses execution environments, so a leaked
 * ThreadLocal would carry one tenant's id into the next request).
 *
 * An authenticated token WITHOUT a valid tenant claim is rejected outright.
 * Unauthenticated public routes (/health, /signup, ...) pass through with no
 * tenant bound.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    static final String TENANT_CLAIM = "custom:tenant_id";
    
    private final ObjectMapper objectMapper;
    
    public TenantContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                try {
                    TenantContext.set(jwtAuth.getToken().getClaimAsString(TENANT_CLAIM));
                } catch (IllegalArgumentException e) {
                    reject(response);
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                new ApiError(403, "Forbidden", "Token has no valid tenant claim"));
    }
}
