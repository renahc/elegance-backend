package com.elegance.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceFlowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  String body(int price, int duration, boolean active) {
    return "{\"name\":\"Corte\",\"category\":\"cabello\",\"durationMinutes\":"
        + duration
        + ",\"basePrice\":"
        + price
        + ",\"active\":"
        + active
        + "}";
  }

  @Test
  void rejectsNegativePrice() throws Exception {
    mvc.perform(
            post("/api/v1/services")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(-1, 60, true)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsInvalidDuration() throws Exception {
    mvc.perform(
            post("/api/v1/services")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(25000, 0, true)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void availabilityTogglePersists() throws Exception {
    var created =
        mvc.perform(
                post("/api/v1/services")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(25000, 60, true)))
            .andExpect(status().isCreated())
            .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    mvc.perform(
            put("/api/v1/services/" + id)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(25000, 60, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(get("/api/v1/services/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }
}
