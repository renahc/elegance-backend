package com.elegance.kafkaadmin;

import com.elegance.common.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ApiSecurity.class)
public class KafkaAdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(KafkaAdminApplication.class, args);
  }
}
