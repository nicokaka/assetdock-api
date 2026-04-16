package com.assetdock.api.dashboard.application;

public record DashboardStatsView(
    int totalAssets,
    int assignedAssets,
    int inStockAssets,
    int inMaintenanceAssets,
    int retiredAssets,
    int lostAssets,
    int totalUsers,
    int activeUsers
) {
}
