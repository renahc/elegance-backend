package com.elegance.analytics;

import com.elegance.common.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ApiSecurity.class)
public class AnalyticsApplication {
  public static void main(String[] args) {
    SpringApplication.run(AnalyticsApplication.class, args);
  }
}
