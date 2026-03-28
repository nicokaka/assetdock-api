package com.assetdock.api.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class MockMvcClientIp {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private MockMvcClientIp() {
	}

	public static RequestPostProcessor uniqueClientIp() {
		int sequence = Math.floorMod(SEQUENCE.incrementAndGet(), 65025);
		int thirdOctet = sequence / 255;
		int fourthOctet = (sequence % 255) + 1;
		String ipAddress = "198.51." + thirdOctet + "." + fourthOctet;
		return request -> {
			request.setRemoteAddr(ipAddress);
			return request;
		};
	}
}
