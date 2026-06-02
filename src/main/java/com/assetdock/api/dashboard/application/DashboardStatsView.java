package com.assetdock.api.dashboard.application;

public record DashboardStatsView(
    int totalAssets,
    int assignedAssets,
    int inStockAssets,
    int inMaintenanceAssets,
    int retiredAssets,
    int totalUsers,
    int activeUsers,
    int activeCheckouts
) {
}

