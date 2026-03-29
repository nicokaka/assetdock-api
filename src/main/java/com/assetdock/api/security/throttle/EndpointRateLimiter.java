package com.assetdock.api.security.throttle;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EndpointRateLimiter {

	private final EnumMap<Endpoint, Map<String, Counter>> counters = new EnumMap<>(Endpoint.class);

	public EndpointRateLimiter() {
		counters.put(Endpoint.LOGIN, new HashMap<>());
		counters.put(Endpoint.ASSET_IMPORT, new HashMap<>());
	}

	public synchronized RateLimitDecision tryAcquire(
		Endpoint endpoint,
		String origin,
		ThrottlingProperties.EndpointPolicy policy,
		Instant now
	) {
		if (!policy.enabled()) {
			return RateLimitDecision.allow();
		}

		Map<String, Counter> scopedCounters = counters.get(endpoint);
		scopedCounters.entrySet().removeIf(entry -> !entry.getValue().windowEndsAt().isAfter(now));
		if (!scopedCounters.containsKey(origin) && scopedCounters.size() >= policy.maxTrackedClients()) {
			Iterator<String> iterator = scopedCounters.keySet().iterator();
			if (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
			}
		}

		Counter counter = scopedCounters.get(origin);
		if (counter == null || !counter.windowEndsAt().isAfter(now)) {
			scopedCounters.put(origin, new Counter(1, now.plus(policy.window())));
			return RateLimitDecision.allow();
		}

		if (counter.count() >= policy.maxRequests()) {
			return RateLimitDecision.deny(Duration.between(now, counter.windowEndsAt()));
		}

		scopedCounters.put(origin, new Counter(counter.count() + 1, counter.windowEndsAt()));
		return RateLimitDecision.allow();
	}

	public enum Endpoint {
		LOGIN,
		ASSET_IMPORT
	}

	private record Counter(
		int count,
		Instant windowEndsAt
	) {
	}

	public record RateLimitDecision(
		boolean allowed,
		long retryAfterSeconds
	) {

		public static RateLimitDecision allow() {
			return new RateLimitDecision(true, 0);
		}

		public static RateLimitDecision deny(Duration retryAfter) {
			long seconds = Math.max(1, retryAfter.toSeconds());
			return new RateLimitDecision(false, seconds);
		}
	}
}
