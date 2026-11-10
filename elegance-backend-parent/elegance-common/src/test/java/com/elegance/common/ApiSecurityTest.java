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
import org.springframework.http.HttpHeaders;
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
  void preflightDoesNotRequireBearerToken() throws Exception {
    mvc.perform(
            options("/api/v1/services")
                .header(HttpHeaders.ORIGIN, "https://98.93.83.155")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
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
