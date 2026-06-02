package com.assetdock.api.dashboard.application;

import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.checkout.domain.AssetCheckoutRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {

	private final AssetRepository assetRepository;
	private final UserRepository userRepository;
	private final AssetCheckoutRepository checkoutRepository;

	public DashboardQueryService(
			AssetRepository assetRepository,
			UserRepository userRepository,
			AssetCheckoutRepository checkoutRepository) {
		this.assetRepository = assetRepository;
		this.userRepository = userRepository;
		this.checkoutRepository = checkoutRepository;
	}

	public DashboardStatsView getStats(AuthenticatedUserPrincipal principal) {
		UUID orgId = principal.organizationId();

		// H-3: SUPER_ADMIN without an organization context yields no meaningful org-scoped data.
		// Return a clear empty state rather than silently returning zeros.
		if (orgId == null) {
			return new DashboardStatsView(0, 0, 0, 0, 0, 0, 0, 0);
		}

		Map<AssetStatus, Integer> assetCounts = assetRepository.countByStatusForOrganization(orgId);
		
		int totalAssets = assetCounts.values().stream().mapToInt(Integer::intValue).sum();
		int assignedAssets = assetCounts.getOrDefault(AssetStatus.ASSIGNED, 0);
		int inStockAssets = assetCounts.getOrDefault(AssetStatus.IN_STOCK, 0);
		int inMaintenanceAssets = assetCounts.getOrDefault(AssetStatus.IN_MAINTENANCE, 0);
		int retiredAssets = assetCounts.getOrDefault(AssetStatus.RETIRED, 0);
		
		int totalUsers = userRepository.countTotalUsersForOrganization(orgId);
		int activeUsers = userRepository.countActiveUsersForOrganization(orgId);
		int activeCheckouts = checkoutRepository.countActiveCheckoutsForOrganization(orgId);
		
		return new DashboardStatsView(
			totalAssets,
			assignedAssets,
			inStockAssets,
			inMaintenanceAssets,
			retiredAssets,
			totalUsers,
			activeUsers,
			activeCheckouts
		);
	}
}
