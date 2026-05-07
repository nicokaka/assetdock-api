package com.assetdock.api.setup.api;

import java.lang.management.ManagementFactory;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
public class SystemInfoController {

	private final String appVersion;

	public SystemInfoController(@Value("${info.app.version:1.1.0}") String appVersion) {
		this.appVersion = appVersion;
	}

	@GetMapping
	public Map<String, String> getSystemInfo() {
		long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
		String uptime = formatUptime(uptimeMillis);

		return Map.of(
			"version", appVersion,
			"uptime", uptime,
			"javaVersion", System.getProperty("java.version")
		);
	}

	private String formatUptime(long uptimeMillis) {
		long seconds = uptimeMillis / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (days > 0) {
			return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
		} else if (hours > 0) {
			return String.format("%dh %dm", hours, minutes % 60);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, seconds % 60);
		} else {
			return String.format("%ds", seconds);
		}
	}
}
