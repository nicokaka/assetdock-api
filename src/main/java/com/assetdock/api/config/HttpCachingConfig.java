package com.assetdock.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * HTTP caching configuration.
 *
 * <h3>ETag support</h3>
 * Registers {@link ShallowEtagHeaderFilter} which computes an MD5-based ETag over the
 * serialized response body. On repeat requests the browser sends {@code If-None-Match};
 * if the response has not changed, Spring returns {@code 304 Not Modified} with an empty
 * body — saving bandwidth and reducing client-side re-renders.
 *
 * <h3>Cache-Control</h3>
 * Sets {@code Cache-Control: private, no-cache} on all GET responses via
 * {@link CacheControlInterceptor}. This enforces per-user caching (no shared proxies)
 * and requires revalidation on every request, enabling the 304 round-trip.
 */
@Configuration
public class HttpCachingConfig implements WebMvcConfigurer {

	private final CacheControlInterceptor cacheControlInterceptor;

	public HttpCachingConfig(CacheControlInterceptor cacheControlInterceptor) {
		this.cacheControlInterceptor = cacheControlInterceptor;
	}

	@Bean
	ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
		return new ShallowEtagHeaderFilter();
	}

	@Override
	public void addInterceptors(@NonNull InterceptorRegistry registry) {
		registry.addInterceptor(cacheControlInterceptor).addPathPatterns("/api/**");
	}
}
