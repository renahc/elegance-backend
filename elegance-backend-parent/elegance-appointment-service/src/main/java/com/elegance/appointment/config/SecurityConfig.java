package com.elegance.appointment.config;

import com.elegance.common.ApiSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ApiSecurity.class)
public class SecurityConfig {}
