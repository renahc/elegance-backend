package com.elegance.salon.controller;

import com.elegance.salon.dto.UserInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación Azure AD", description = "Endpoints de verificación de identidad del token JWT de Azure AD (MSAL / Entra ID)")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "Obtener información del usuario autenticado vía JWT Azure AD")
    public ResponseEntity<UserInfoDto> getAuthenticatedUserInfo(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            UserInfoDto anon = new UserInfoDto();
            anon.setName("Usuario Anónimo");
            anon.setAuthenticated(false);
            anon.setRoles(Collections.emptyList());
            return ResponseEntity.ok(anon);
        }

        String name = jwt.getClaimAsString("name");
        if (name == null) name = jwt.getClaimAsString("preferred_username");

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getClaimAsString("preferred_username");

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = Collections.emptyList();

        UserInfoDto userInfo = new UserInfoDto(
                jwt.getSubject(),
                name != null ? name : "Usuario Azure AD",
                email,
                jwt.getClaimAsString("preferred_username"),
                jwt.getIssuer() != null ? jwt.getIssuer().toString() : "Azure AD IDaaS",
                roles,
                true
        );

        return ResponseEntity.ok(userInfo);
    }
}
