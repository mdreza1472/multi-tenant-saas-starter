package com.reza.saas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reza.saas.common.ApiError;
import com.reza.saas.tenant.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Second line of defence behind the API Gateway JWT authorizer: the app
 * re-validates every token itself (signature, expiry, issuer), so it stays safe
 * even if the gateway routing is ever misconfigured or the app is run behind
 * something else.
 *
 * Public routes are deliberately few: /health, /signup (tenant onboarding, no
 * tenant exists yet) and /webhooks/stripe (authenticated by Stripe's signature,
 * not a JWT).
 *
 * 401/403 responses are rendered here as ApiError bodies to match
 * GlobalExceptionHandler's shape - Spring Security rejects bad/missing tokens
 * and failed @PreAuthorize checks BEFORE a request reaches any controller, so
 * GlobalExceptionHandler (a @RestControllerAdvice) never sees these failures
 * unless this class renders them itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies/sessions
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/signup", "/webhooks/stripe").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
                // Instantiated here (not a @Component) so Boot doesn't ALSO register it
                // as a plain servlet filter and run it twice.
                .addFilterAfter(new TenantContextFilter(objectMapper), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /** Missing/invalid/expired token -> 401, same ApiError shape as every other error. */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeError(response, HttpStatus.UNAUTHORIZED);
    }

    /** Valid token, wrong role (e.g. @PreAuthorize("hasRole('TENANT_ADMIN')") failing) -> 403. */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeError(response, HttpStatus.FORBIDDEN);
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                new ApiError(status.value(), status.getReasonPhrase(), status.getReasonPhrase()));
    }

    /**
     * Maps Cognito groups (the "cognito:groups" claim) to Spring roles, so
     * @PreAuthorize("hasRole('TENANT_ADMIN')") works. In this starter a user belongs
     * to exactly one tenant, so a pool-level group doubles as their role within it.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("cognito:groups");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}