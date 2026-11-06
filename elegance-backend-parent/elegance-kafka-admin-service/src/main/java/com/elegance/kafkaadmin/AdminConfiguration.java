package com.elegance.kafkaadmin;

import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class AdminConfiguration {
  @Bean(destroyMethod = "close")
  AdminClient adminClient(@Value("${spring.kafka.bootstrap-servers}") String servers) {
    return AdminClient.create(
        Map.of(
            "bootstrap.servers",
            servers,
            "request.timeout.ms",
            5000,
            "default.api.timeout.ms",
            10000));
  }
}
