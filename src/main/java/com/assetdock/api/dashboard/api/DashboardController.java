package com.assetdock.api.dashboard.api;

import com.assetdock.api.dashboard.application.DashboardQueryService;
import com.assetdock.api.dashboard.application.DashboardStatsView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

	private final DashboardQueryService dashboardQueryService;

	public DashboardController(DashboardQueryService dashboardQueryService) {
		this.dashboardQueryService = dashboardQueryService;
	}

	@GetMapping("/stats")
	public DashboardStatsView getStats(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return dashboardQueryService.getStats(principal);
	}
}
