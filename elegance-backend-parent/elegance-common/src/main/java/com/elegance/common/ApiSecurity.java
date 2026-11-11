package com.elegance.common;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class ApiSecurity {
  @Bean
  SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return http.cors(c -> {})
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/services",
                        "/api/v1/services/*",
                        "/api/v1/stylists",
                        "/api/v1/stylists/*")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/clients/me", "/api/v1/appointments", "/api/v1/appointments/**")
                    .authenticated()
                    .anyRequest()
                    .hasRole("Admin"))
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${AZURE_AD_ISSUER_URI:https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}")
          String issuer,
      @Value("${AZURE_AD_CLIENT_ID:304d54f7-d485-478a-a1ea-0c2f874b0c1f}") String audience,
      @Value(
              "${AZURE_AD_JWK_SET_URI:https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/discovery/v2.0/keys}")
          String jwks) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> aud =
        jwt ->
            jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), aud));
    return decoder;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${CORS_ALLOWED_ORIGINS:*}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origins.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  public static boolean isAdmin(org.springframework.security.core.Authentication auth) {
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
  }

  @Bean
  public org.springframework.web.filter.OncePerRequestFilter correlationFilter() {
    return new org.springframework.web.filter.OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          jakarta.servlet.http.HttpServletRequest request,
          jakarta.servlet.http.HttpServletResponse response,
          jakarta.servlet.FilterChain chain)
          throws jakarta.servlet.ServletException, java.io.IOException {
        String id = request.getHeader("X-Correlation-ID");
        if (id == null || !id.matches("[a-zA-Z0-9-]{1,64}"))
          id = java.util.UUID.randomUUID().toString();
        org.slf4j.MDC.put("correlationId", id);
        response.setHeader("X-Correlation-ID", id);
        try {
          chain.doFilter(request, response);
        } finally {
          org.slf4j.MDC.remove("correlationId");
        }
      }
    };
  }
}
