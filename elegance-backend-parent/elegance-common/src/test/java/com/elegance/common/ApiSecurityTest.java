package com.elegance.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

@WebMvcTest(ApiSecurityTest.Endpoints.class)
@Import({ApiSecurity.class, ApiSecurityTest.Endpoints.class})
class ApiSecurityTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class App {}

  @RestController
  static class Endpoints {
    @GetMapping("/api/v1/services")
    String catalog() {
      return "ok";
    }

    @PostMapping("/api/v1/services")
    String write() {
      return "ok";
    }

    @GetMapping("/api/v1/admin/kafka/topics")
    String topics() {
      return "ok";
    }
  }

  @Autowired MockMvc mvc;
  @MockBean JwtDecoder decoder;

  @Test
  void publicCanReadCatalog() throws Exception {
    mvc.perform(get("/api/v1/services")).andExpect(status().isOk());
  }

  @Test
  void anonymousCannotWrite() throws Exception {
    mvc.perform(post("/api/v1/services")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedClientCannotWrite() throws Exception {
    mvc.perform(post("/api/v1/services").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void administratorCanWrite() throws Exception {
    mvc.perform(
            post("/api/v1/services")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
        .andExpect(status().isOk());
  }

  @Test
  void anonymousCannotManageKafka() throws Exception {
    mvc.perform(get("/api/v1/admin/kafka/topics")).andExpect(status().isUnauthorized());
  }

  @Test
  void clientCannotManageKafka() throws Exception {
    mvc.perform(get("/api/v1/admin/kafka/topics").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void invalidBearerIsRejected() throws Exception {
    org.mockito.Mockito.when(decoder.decode("invalid"))
        .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("invalid token"));
    mvc.perform(get("/api/v1/admin/kafka/topics").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
  }
}
