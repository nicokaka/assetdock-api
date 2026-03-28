package com.assetdock.api;

import com.assetdock.api.auth.application.AuthHardeningProperties;
import com.assetdock.api.config.LocalSeedProperties;
import com.assetdock.api.security.throttle.ThrottlingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LocalSeedProperties.class, AuthHardeningProperties.class, ThrottlingProperties.class})
public class AssetdockApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssetdockApiApplication.class, args);
	}

}
