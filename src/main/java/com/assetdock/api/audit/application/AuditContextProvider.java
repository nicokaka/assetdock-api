package com.assetdock.api.audit.application;

import com.assetdock.api.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditContextProvider {

	public AuditContext current() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return new AuditContext(null, null, null);
		}

		HttpServletRequest request = attributes.getRequest();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String ipAddress = forwardedFor != null && !forwardedFor.isBlank()
			? forwardedFor.split(",")[0].trim()
			: request.getRemoteAddr();

		Object requestIdAttribute = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
		String requestId = requestIdAttribute instanceof String requestIdValue
			? requestIdValue
			: request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

		return new AuditContext(
			ipAddress,
			request.getHeader("User-Agent"),
			requestId
		);
	}
}
