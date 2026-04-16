package com.assetdock.api.dashboard.application;

import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {

	private final AssetRepository assetRepository;
	private final UserRepository userRepository;

	public DashboardQueryService(AssetRepository assetRepository, UserRepository userRepository) {
		this.assetRepository = assetRepository;
		this.userRepository = userRepository;
	}

	public DashboardStatsView getStats(AuthenticatedUserPrincipal principal) {
		Map<AssetStatus, Integer> assetCounts = assetRepository.countByStatusForOrganization(principal.organizationId());
		
		int totalAssets = assetCounts.values().stream().mapToInt(Integer::intValue).sum();
		int assignedAssets = assetCounts.getOrDefault(AssetStatus.ASSIGNED, 0);
		int inStockAssets = assetCounts.getOrDefault(AssetStatus.IN_STOCK, 0);
		int inMaintenanceAssets = assetCounts.getOrDefault(AssetStatus.IN_MAINTENANCE, 0);
		int retiredAssets = assetCounts.getOrDefault(AssetStatus.RETIRED, 0);
		int lostAssets = assetCounts.getOrDefault(AssetStatus.LOST, 0);
		
		int totalUsers = userRepository.countTotalUsersForOrganization(principal.organizationId());
		int activeUsers = userRepository.countActiveUsersForOrganization(principal.organizationId());
		
		return new DashboardStatsView(
			totalAssets,
			assignedAssets,
			inStockAssets,
			inMaintenanceAssets,
			retiredAssets,
			lostAssets,
			totalUsers,
			activeUsers
		);
	}
}
