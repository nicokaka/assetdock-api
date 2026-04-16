package com.assetdock.api.config;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.assignment.domain.AssetAssignment;
import com.assetdock.api.assignment.domain.AssetAssignmentRepository;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.catalog.domain.Category;
import com.assetdock.api.catalog.domain.CategoryRepository;
import com.assetdock.api.catalog.domain.Location;
import com.assetdock.api.catalog.domain.LocationRepository;
import com.assetdock.api.catalog.domain.Manufacturer;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the database with realistic demonstration data for portfolio screenshots.
 * Runs only when PORTFOLIO_SEED_ENABLED=true is set in environment variables.
 * Is idempotent: checks whether assets already exist before inserting anything.
 */
@Component
@Order(10)
public class PortfolioSeedRunner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioSeedRunner.class);

	private final boolean enabled;
	private final JdbcTemplate jdbcTemplate;
	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final LocationRepository locationRepository;
	private final AssetRepository assetRepository;
	private final AssetAssignmentRepository assetAssignmentRepository;
	private final AuditLogRepository auditLogRepository;
	private final Clock clock;

	public PortfolioSeedRunner(
		@Value("${app.portfolio-seed.enabled:false}") boolean enabled,
		JdbcTemplate jdbcTemplate,
		UserRepository userRepository,
		CategoryRepository categoryRepository,
		ManufacturerRepository manufacturerRepository,
		LocationRepository locationRepository,
		AssetRepository assetRepository,
		AssetAssignmentRepository assetAssignmentRepository,
		AuditLogRepository auditLogRepository,
		Clock clock
	) {
		this.enabled = enabled;
		this.jdbcTemplate = jdbcTemplate;
		this.userRepository = userRepository;
		this.categoryRepository = categoryRepository;
		this.manufacturerRepository = manufacturerRepository;
		this.locationRepository = locationRepository;
		this.assetRepository = assetRepository;
		this.assetAssignmentRepository = assetAssignmentRepository;
		this.auditLogRepository = auditLogRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled) {
			LOGGER.info("portfolio_seed status=disabled");
			return;
		}

		var orgId = jdbcTemplate.query(
			"SELECT id FROM organizations LIMIT 1",
			(rs, rowNum) -> UUID.fromString(rs.getString("id"))
		).stream().findFirst().orElse(null);

		if (orgId == null) {
			LOGGER.warn("portfolio_seed status=skipped reason=no_organization_found");
			return;
		}
		var existingAssets = assetRepository.findAllPaginated(orgId, 1, 0, null, null);
		if (!existingAssets.isEmpty()) {
			LOGGER.info("portfolio_seed status=skipped reason=assets_already_exist org_id={}", orgId);
			return;
		}

		var admin = userRepository.findAllPaginated(orgId, 1, 0, null).stream().findFirst().orElse(null);
		if (admin == null) {
			LOGGER.warn("portfolio_seed status=skipped reason=no_admin_user_found org_id={}", orgId);
			return;
		}

		LOGGER.info("portfolio_seed status=starting org_id={}", orgId);
		var adminId = admin.id();
		var now = Instant.now(clock);

		// Categories
		var macCat = saveCategory(orgId, "Laptop (Mac)", "Apple MacBooks", now);
		var pcCat = saveCategory(orgId, "Laptop (PC)", "Windows/Linux Laptops", now);
		var monitorCat = saveCategory(orgId, "Monitor", "External Displays", now);
		var phoneCat = saveCategory(orgId, "Mobile Phone", "Corporate Phones", now);
		var badgeCat = saveCategory(orgId, "Access Badge", "Physical NFC Badges", now);

		// Manufacturers
		var apple = saveManufacturer(orgId, "Apple", "Apple Inc.", now);
		var dell = saveManufacturer(orgId, "Dell", "Dell Technologies", now);

		// Locations
		var hq = saveLocation(orgId, "Auckland HQ - Floor 3", "Main Office", now);
		var remote = saveLocation(orgId, "Remote (WFH)", "Work from Home assignments", now);

		// Users
		var user1 = saveUser(orgId, "amanda.silva@assetdock.local", "Amanda Silva", now.minus(30, ChronoUnit.DAYS));
		var user2 = saveUser(orgId, "chloe.chen@assetdock.local", "Chloe Chen", now.minus(15, ChronoUnit.DAYS));
		var user3 = saveUser(orgId, "thomas.williams@assetdock.local", "Thomas Williams", now.minus(10, ChronoUnit.DAYS));
		var user4 = saveUser(orgId, "alex.murphy@assetdock.local", "Alex Murphy", now.minus(5, ChronoUnit.DAYS));

		// Assets
		var asset1 = saveAsset(orgId, "AST-MBP-001", "C02F239XMD6T", "nz-eng-mac-01",
			"MacBook Pro 16\" M3 Max", "Engineering Team Primary",
			macCat.id(), apple.id(), remote.id(), user1.id(),
			AssetStatus.ASSIGNED, LocalDate.of(2023, 11, 15), now.minus(20, ChronoUnit.DAYS));

		var asset2 = saveAsset(orgId, "AST-MBP-002", "C02G338YME7U", "nz-eng-mac-02",
			"MacBook Pro 14\" M3 Pro", "Engineering Team Secondary",
			macCat.id(), apple.id(), hq.id(), user2.id(),
			AssetStatus.ASSIGNED, LocalDate.of(2023, 11, 20), now.minus(18, ChronoUnit.DAYS));

		saveAsset(orgId, "AST-MON-001", "CN-0P1Y0K-74261", null,
			"Dell UltraSharp 27\" 4K", "Hotdesk monitor #12",
			monitorCat.id(), dell.id(), hq.id(), null,
			AssetStatus.IN_STOCK, LocalDate.of(2022, 5, 10), now.minus(25, ChronoUnit.DAYS));

		var asset4 = saveAsset(orgId, "AST-PHN-001", "DNPG319XN732", "iPhone-Chloe",
			"iPhone 15 Pro", "Sales Department",
			phoneCat.id(), apple.id(), remote.id(), user2.id(),
			AssetStatus.ASSIGNED, LocalDate.of(2023, 9, 25), now.minus(15, ChronoUnit.DAYS));

		saveAsset(orgId, "AST-BDG-102", "NFC-993-12", null,
			"HQ Access Badge", "Card ID: 109283",
			badgeCat.id(), null, hq.id(), null,
			AssetStatus.LOST, null, now.minus(60, ChronoUnit.DAYS));

		var asset6 = saveAsset(orgId, "AST-MBP-003", "C02H412ZMF8V", "nz-dev-mac-03",
			"MacBook Air 15\" M2", "Office loaner",
			macCat.id(), apple.id(), hq.id(), null,
			AssetStatus.IN_MAINTENANCE, LocalDate.of(2023, 8, 1), now.minus(10, ChronoUnit.DAYS));

		var asset7 = saveAsset(orgId, "AST-PC-001", "5P1X8Y2", "nz-fin-dell-01",
			"Dell XPS 15", "Finance Team",
			pcCat.id(), dell.id(), hq.id(), user4.id(),
			AssetStatus.RETIRED, LocalDate.of(2019, 3, 15), now.minus(40, ChronoUnit.DAYS));

		// Assignments
		saveAssignment(orgId, asset1.id(), user1.id(), adminId, now.minus(19, ChronoUnit.DAYS), null, remote.id(), "Initial provision for Engineering.");
		saveAssignment(orgId, asset2.id(), user2.id(), adminId, now.minus(17, ChronoUnit.DAYS), null, hq.id(), "Assigned internally.");
		saveAssignment(orgId, asset4.id(), user2.id(), adminId, now.minus(14, ChronoUnit.DAYS), null, remote.id(), "Corporate phone provision.");
		saveAssignment(orgId, asset6.id(), user3.id(), adminId, now.minus(20, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), hq.id(), "Loaner returned damaged, sent to repair.");
		saveAssignment(orgId, asset7.id(), user4.id(), adminId, now.minus(30, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS), hq.id(), "User terminated. Laptop too old, retiring asset.");

		// Audit logs
		saveAudit(orgId, adminId, AuditEventType.USER_CREATED, "USER", user1.id(), Map.of("email", "amanda.silva@assetdock.local"), now.minus(30, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.ASSET_CREATED, "ASSET", asset1.id(), Map.of("asset_tag", "AST-MBP-001"), now.minus(20, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.ASSET_ASSIGNED, "ASSET", asset1.id(), Map.of("assigned_to_user_id", user1.id().toString()), now.minus(19, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.ASSET_UPDATED, "ASSET", asset6.id(), Map.of("old_status", "IN_STOCK", "new_status", "IN_MAINTENANCE"), now.minus(2, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.ASSET_UNASSIGNED, "ASSET", asset7.id(), Map.of(), now.minus(5, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.USER_UPDATED, "USER", user4.id(), Map.of("old_status", "ACTIVE", "new_status", "INACTIVE"), now.minus(4, ChronoUnit.DAYS));
		saveAudit(orgId, adminId, AuditEventType.LOGIN_SUCCESS, "USER", adminId, Map.of(), now.minus(120, ChronoUnit.MINUTES));

		LOGGER.info("portfolio_seed status=completed org_id={}", orgId);
	}

	private Category saveCategory(UUID orgId, String name, String description, Instant now) {
		return categoryRepository.save(new Category(UUID.randomUUID(), orgId, name, description, true, now, now));
	}

	private Manufacturer saveManufacturer(UUID orgId, String name, String description, Instant now) {
		return manufacturerRepository.save(new Manufacturer(UUID.randomUUID(), orgId, name, description, null, true, now, now));
	}

	private Location saveLocation(UUID orgId, String name, String description, Instant now) {
		return locationRepository.save(new Location(UUID.randomUUID(), orgId, name, description, true, now, now));
	}

	private User saveUser(UUID orgId, String email, String fullName, Instant createdAt) {
		var user = new User(UUID.randomUUID(), orgId, email, fullName, "dummy-not-login", UserStatus.ACTIVE, Set.of(UserRole.VIEWER), 0, null, createdAt, createdAt);
		return userRepository.save(user);
	}

	private Asset saveAsset(
		UUID orgId, String tag, String serial, String hostname,
		String displayName, String description,
		UUID categoryId, UUID manufacturerId, UUID locationId, UUID assignedUserId,
		AssetStatus status, LocalDate purchaseDate, Instant createdAt
	) {
		return assetRepository.save(new Asset(
			UUID.randomUUID(), orgId, tag, serial, hostname,
			displayName, description,
			categoryId, manufacturerId, locationId, assignedUserId,
			status, purchaseDate, null, null, createdAt, createdAt
		));
	}

	private void saveAssignment(
		UUID orgId, UUID assetId, UUID userId, UUID assignedBy,
		Instant assignedAt, Instant unassignedAt, UUID locationId, String notes
	) {
		assetAssignmentRepository.save(new AssetAssignment(
			UUID.randomUUID(), orgId, assetId, userId, locationId,
			assignedAt, unassignedAt, assignedBy, notes, assignedAt
		));
	}

	private void saveAudit(
		UUID orgId, UUID actorId, AuditEventType eventType,
		String resourceType, UUID resourceId, Map<String, Object> details, Instant occurredAt
	) {
		auditLogRepository.save(new AuditLogEntry(
			UUID.randomUUID(), orgId, actorId, eventType,
			resourceType, resourceId, "SUCCESS",
			"203.0.113.1", "AssetDock/PortfolioSeed", null,
			details, occurredAt
		));
	}
}
