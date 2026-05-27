package com.resumescreener.config;

import com.resumescreener.security.PromptInjectionDetector;
import com.resumescreener.security.PiiDetector;
import com.resumescreener.security.SafetyValidationService;
import com.resumescreener.security.SecurityEventLogger;
import com.resumescreener.security.SafetyCheckInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000",
            "http://127.0.0.1:4200"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ========== AI Safety Layer Components ==========

    @Bean
    public PromptInjectionDetector promptInjectionDetector() {
        return new PromptInjectionDetector();
    }

    @Bean
    public PiiDetector piiDetector() {
        return new PiiDetector();
    }

    @Bean
    public SecurityEventLogger securityEventLogger() {
        return new SecurityEventLogger();
    }

    @Bean
    public SafetyValidationService safetyValidationService() {
        return new SafetyValidationService();
    }

    @Bean
    public SafetyCheckInterceptor safetyCheckInterceptor() {
        return new SafetyCheckInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(safetyCheckInterceptor())
                .addPathPatterns("/api/**");
    }
}
