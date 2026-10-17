package com.ticketrush.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ticketrush.cors.origins:http://localhost:5173}")
    private String[] origins;

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET" , "POST" , "PUT" , "DELETE" , "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
