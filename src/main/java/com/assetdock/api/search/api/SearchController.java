package com.assetdock.api.search.api;

import com.assetdock.api.search.application.GlobalSearchResult;
import com.assetdock.api.search.application.SearchQueryService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/search", "/api/v1/search"})
public class SearchController {

	private final SearchQueryService searchQueryService;

	public SearchController(SearchQueryService searchQueryService) {
		this.searchQueryService = searchQueryService;
	}

	@GetMapping
	public GlobalSearchResult search(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@RequestParam(required = false, defaultValue = "") String q,
		@RequestParam(required = false, defaultValue = "10") Integer limit
	) {
		return searchQueryService.search(principal, q, limit != null && limit <= 50 ? limit : 10);
	}
}
