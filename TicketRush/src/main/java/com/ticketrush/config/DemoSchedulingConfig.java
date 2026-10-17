package com.ticketrush.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling exists only in the public demo, where the periodic stock reset runs. */
@Configuration
@EnableScheduling
@Profile("demo")
public class DemoSchedulingConfig {
}
