package com.assetdock.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtcClockConfig {

	@Bean
	Clock systemUtcClock() {
		return Clock.systemUTC();
	}
}
