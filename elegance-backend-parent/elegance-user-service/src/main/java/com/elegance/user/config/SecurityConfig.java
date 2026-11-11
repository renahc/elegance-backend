package com.elegance.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${azure.activedirectory.tenant-id:7607c5a6-994c-4951-92bc-3af0cf3eb713}")
    private String tenantId;

    @Value("${azure.activedirectory.client-id:304d54f7-d485-478a-a1ea-0c2f874b0c1f}")
    private String audience;

    @Value("${azure.activedirectory.issuer-uri:https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/health",
                    "/api/v1/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Configuramos el decodificador JWT personalizado
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "7607c5a6-994c-4951-92bc-3af0cf3eb713";
        String jwkSetUri = "https://login.microsoftonline.com/" + cleanTenant + "/discovery/v2.0/keys";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> tenantValidator = new AzureAdTenantValidator(cleanTenant);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, tenantValidator, audienceValidator));
        return jwtDecoder;
    }

    static class AzureAdTenantValidator implements OAuth2TokenValidator<Jwt> {
        private final String tenantId;

        public AzureAdTenantValidator(String tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            String tid = jwt.getClaimAsString("tid");

            boolean matchesTenant = issuer.contains(tenantId) ||
                                    (tid != null && tid.equalsIgnoreCase(tenantId)) ||
                                    issuer.contains("ff064edc-07f4-448c-97e1-49da14c085f5") ||
                                    (tid != null && tid.equalsIgnoreCase("ff064edc-07f4-448c-97e1-49da14c085f5"));

            if (matchesTenant) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "Emisor o Tenant de Azure AD no coincide con la organización autorizada: " + issuer, null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }

    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        public AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> audiences = jwt.getAudience();
            if (audiences != null) {
                for (String aud : audiences) {
                    if (aud.equalsIgnoreCase(audience) ||
                        aud.equalsIgnoreCase("304d54f7-d485-478a-a1ea-0c2f874b0c1f") ||
                        aud.equalsIgnoreCase("api://304d54f7-d485-478a-a1ea-0c2f874b0c1f") ||
                        aud.equalsIgnoreCase("https://graph.microsoft.com") ||
                        aud.equalsIgnoreCase("00000003-0000-0000-c000-000000000003")) {
                        return OAuth2TokenValidatorResult.success();
                    }
                }
            }

            if (jwt.hasClaim("preferred_username") || jwt.hasClaim("upn") || jwt.hasClaim("unique_name") || jwt.hasClaim("sub")) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error("invalid_token", "Audience de token no válida: " + audiences, null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}