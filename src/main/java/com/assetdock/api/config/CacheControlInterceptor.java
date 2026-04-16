package com.assetdock.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sets {@code Cache-Control: private, no-cache} on all GET responses.
 *
 * <p>This instructs browsers and CDNs to:
 * <ul>
 *   <li><b>private</b> — not cache in shared/proxy caches (data is tenant-specific).</li>
 *   <li><b>no-cache</b> — always revalidate before using cached payload (enables 304 via ETag).</li>
 * </ul>
 *
 * <p>Combined with {@link ShallowEtagHeaderFilter}, the browser will send
 * {@code If-None-Match} on repeat requests and receive {@code 304 Not Modified}
 * when the response has not changed, eliminating body transfer overhead.
 *
 * <p>Mutation methods (POST, PATCH, PUT, DELETE) are intentionally excluded.
 */
@Component
public class CacheControlInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull Object handler
	) {
		if (HttpMethod.GET.matches(request.getMethod())) {
			response.setHeader("Cache-Control", "private, no-cache");
		}
		return true;
	}
}
