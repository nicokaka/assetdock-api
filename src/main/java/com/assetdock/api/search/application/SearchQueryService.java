package com.assetdock.api.search.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchQueryService {

	private final AssetRepository assetRepository;
	private final UserRepository userRepository;
	private final TenantAccessService tenantAccessService;

	public SearchQueryService(
		AssetRepository assetRepository,
		UserRepository userRepository,
		TenantAccessService tenantAccessService
	) {
		this.assetRepository = assetRepository;
		this.userRepository = userRepository;
		this.tenantAccessService = tenantAccessService;
	}

	@Transactional(readOnly = true)
	public GlobalSearchResult search(AuthenticatedUserPrincipal actor, String query, int limit) {
		if (actor.isSuperAdmin() || actor.organizationId() == null) {
			// Super admin cross-tenant search is out of scope for this API iteration
			return new GlobalSearchResult(List.of(), List.of());
		}

		UUID orgId = actor.organizationId();
		tenantAccessService.requireAssetReadAccess(actor, orgId);

		String sanitizedQuery = query != null ? query.trim() : "";
		if (sanitizedQuery.length() < 2) {
			return new GlobalSearchResult(List.of(), List.of());
		}

		List<Asset> assets = assetRepository.findAllPaginated(orgId, limit, 0, null, sanitizedQuery);
		
		boolean canReadUsers = false;
		try {
			tenantAccessService.requireUserReadAccess(actor, orgId);
			canReadUsers = true;
		} catch (Exception ignored) {
		}

		List<User> users = canReadUsers 
			? userRepository.findAllPaginated(orgId, limit, 0, sanitizedQuery) 
			: List.of();

		return new GlobalSearchResult(
			assets.stream().map(a -> new GlobalSearchResult.SearchResultItem(
				a.id().toString(),
				a.displayName() != null ? a.displayName() : "Unnamed Asset",
				a.assetTag() + (a.serialNumber() != null ? " • " + a.serialNumber() : ""),
				"/app/assets/" + a.id()
			)).toList(),
			users.stream().map(u -> new GlobalSearchResult.SearchResultItem(
				u.id().toString(),
				u.fullName(),
				u.email() + " • " + u.status(),
				"/app/users/" + u.id()
			)).toList()
		);
	}
}
