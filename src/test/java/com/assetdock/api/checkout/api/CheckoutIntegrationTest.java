package com.assetdock.api.checkout.api;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.support.AbstractIntegrationTest;
import com.assetdock.api.user.domain.UserRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ORG_1         = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_2         = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORG_ADMIN_1   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ASSET_MANAGER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID AUDITOR_1     = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VIEWER_1      = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID USER_1        = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID USER_ORG_2    = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private static final UUID CATEGORY_1      = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID MANUFACTURER_1  = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID LOCATION_1      = UUID.fromString("10000000-0000-0000-0000-000000000001");

    // IN_STOCK — eligible for checkout
    private static final UUID ASSET_IN_STOCK  = UUID.fromString("50000000-0000-0000-0000-000000000005");
    // ASSIGNED — eligible for checkin
    private static final UUID ASSET_ASSIGNED  = UUID.fromString("60000000-0000-0000-0000-000000000006");
    // Other statuses that must block checkout
    private static final UUID ASSET_RETIRED   = UUID.fromString("70000000-0000-0000-0000-000000000007");
    // Asset in other org
    private static final UUID ASSET_ORG_2     = UUID.fromString("90000000-0000-0000-0000-000000000009");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        insertOrganization(ORG_1, "org-one");
        insertOrganization(ORG_2, "org-two");

        insertUser(ORG_ADMIN_1,   ORG_1, "orgadmin1@assetdock.dev",  "ORG_ADMIN");
        insertUser(ASSET_MANAGER, ORG_1, "manager1@assetdock.dev",   "ASSET_MANAGER");
        insertUser(AUDITOR_1,     ORG_1, "auditor1@assetdock.dev",   "AUDITOR");
        insertUser(VIEWER_1,      ORG_1, "viewer1@assetdock.dev",    "VIEWER");
        insertUser(USER_1,        ORG_1, "user1@assetdock.dev",      "VIEWER");
        insertUser(USER_ORG_2,    ORG_2, "user2@assetdock.dev",      "VIEWER");

        insertCategory(CATEGORY_1, ORG_1, "Laptops");
        insertManufacturer(MANUFACTURER_1, ORG_1, "Dell");
        insertLocation(LOCATION_1, ORG_1, "Warehouse");

        insertAsset(ASSET_IN_STOCK, ORG_1, "AST-001", CATEGORY_1, MANUFACTURER_1, LOCATION_1, null,   "IN_STOCK");
        insertAsset(ASSET_ASSIGNED, ORG_1, "AST-002", CATEGORY_1, MANUFACTURER_1, LOCATION_1, USER_1, "ASSIGNED");
        insertAsset(ASSET_RETIRED,  ORG_1, "AST-003", CATEGORY_1, MANUFACTURER_1, LOCATION_1, null,   "RETIRED");
        insertAsset(ASSET_ORG_2,    ORG_2, "AST-900", null, null, null, null, "IN_STOCK");

        // Pre-existing active checkout record for ASSET_ASSIGNED
        insertActiveCheckout(UUID.fromString("c0000000-0000-0000-0000-00000000000c"),
                ORG_1, ASSET_ASSIGNED, USER_1, ORG_ADMIN_1);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    // -------------------------------------------------------------------------
    // Happy path: full checkout → checkin lifecycle
    // -------------------------------------------------------------------------

    @Test
    void orgAdminCheckoutThenCheckinFullLifecycle() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        // 1. Checkout
        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "personId": "%s",
                                  "notes": "Field visit"
                                }
                                """.formatted(USER_1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").value(ASSET_IN_STOCK.toString()))
                .andExpect(jsonPath("$.personId").value(USER_1.toString()))
                .andExpect(jsonPath("$.checkedOutAt").isNotEmpty())
                .andExpect(jsonPath("$.checkedInAt").value(nullValue()))
                .andExpect(jsonPath("$.checkedOutBy").value(ORG_ADMIN_1.toString()));

        // 2. Asset status must be ASSIGNED after checkout
        Map<String, Object> assetSnapshot = jdbcTemplate.queryForMap(
                "SELECT status, current_assigned_person_id FROM assets WHERE id = ?", ASSET_IN_STOCK);
        assertThat(assetSnapshot)
                .containsEntry("status", "ASSIGNED")
                .containsEntry("current_assigned_person_id", USER_1);

        // 3. Checkin
        mockMvc.perform(post("/assets/{id}/checkin", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "Returned in good condition"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedInAt").isNotEmpty())
                .andExpect(jsonPath("$.checkedInBy").value(ORG_ADMIN_1.toString()));

        // 4. Asset status must be IN_STOCK after checkin, user cleared
        Map<String, Object> assetAfterCheckin = jdbcTemplate.queryForMap(
                "SELECT status, current_assigned_person_id FROM assets WHERE id = ?", ASSET_IN_STOCK);
        assertThat(assetAfterCheckin)
                .containsEntry("status", "IN_STOCK")
                .containsEntry("current_assigned_person_id", null);
    }

    @Test
    void assetManagerCanCheckoutAndCheckin() throws Exception {
        String token = login("manager1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/assets/{id}/checkin", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // History endpoint
    // -------------------------------------------------------------------------

    @Test
    void anyTenantMemberCanReadCheckoutHistory() throws Exception {
        for (String email : new String[]{
                "orgadmin1@assetdock.dev", "manager1@assetdock.dev",
                "auditor1@assetdock.dev",  "viewer1@assetdock.dev"}) {

            mockMvc.perform(get("/assets/{id}/checkouts", ASSET_ASSIGNED)
                            .header(AUTHORIZATION, bearer(login(email))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].assetId").value(ASSET_ASSIGNED.toString()));
        }
    }

    // -------------------------------------------------------------------------
    // Audit trail
    // -------------------------------------------------------------------------

    @Test
    void checkoutAndCheckinPersistAuditLog() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isCreated());

        Map<String, Object> checkoutEvent = latestAuditEvent();
        assertThat(checkoutEvent)
                .containsEntry("event_type", "ASSET_CHECKED_OUT")
                .containsEntry("outcome", "SUCCESS")
                .containsEntry("organization_id", ORG_1);

        mockMvc.perform(post("/assets/{id}/checkin", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        Map<String, Object> checkinEvent = latestAuditEvent();
        assertThat(checkinEvent)
                .containsEntry("event_type", "ASSET_CHECKED_IN")
                .containsEntry("outcome", "SUCCESS")
                .containsEntry("organization_id", ORG_1);
    }

    // -------------------------------------------------------------------------
    // RBAC: viewers and auditors cannot mutate
    // -------------------------------------------------------------------------

    @Test
    void viewerAndAuditorCannotCheckoutOrCheckin() throws Exception {
        String viewerToken  = login("viewer1@assetdock.dev");
        String auditorToken = login("auditor1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(viewerToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/assets/{id}/checkin", ASSET_ASSIGNED)
                        .header(AUTHORIZATION, bearer(auditorToken))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Business rule: asset status must match the operation
    // -------------------------------------------------------------------------

    @Test
    void checkoutRequiresInStockAsset() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        // Cannot checkout an ASSIGNED asset
        mockMvc.perform(post("/assets/{id}/checkout", ASSET_ASSIGNED)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isBadRequest());

        // Cannot checkout a RETIRED asset
        mockMvc.perform(post("/assets/{id}/checkout", ASSET_RETIRED)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkinRequiresAssignedAsset() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkin", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Business rule: user must belong to same organization
    // -------------------------------------------------------------------------

    @Test
    void cannotCheckoutAssetToUserFromAnotherOrganization() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_ORG_2)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Business rule: checkout requires a valid (non-null) userId
    // -------------------------------------------------------------------------

    @Test
    void checkoutWithMissingUserIdIsRejected() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Cross-tenant isolation
    // -------------------------------------------------------------------------

    @Test
    void crossTenantCheckoutIsDenied() throws Exception {
        String token = login("orgadmin1@assetdock.dev");

        mockMvc.perform(post("/assets/{id}/checkout", ASSET_ORG_2)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantHistoryIsDenied() throws Exception {
        String token = login("auditor1@assetdock.dev");

        mockMvc.perform(get("/assets/{id}/checkouts", ASSET_ORG_2)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Unauthenticated access
    // -------------------------------------------------------------------------

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/assets/{id}/checkout", ASSET_IN_STOCK)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"personId": "%s"}
                                """.formatted(USER_1)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/assets/{id}/checkouts", ASSET_IN_STOCK))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Infrastructure helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> latestAuditEvent() {
        return jdbcTemplate.queryForMap("""
                SELECT event_type, outcome, organization_id
                FROM audit_logs
                ORDER BY occurred_at DESC
                LIMIT 1
                """);
    }

    private String login(String email) {
        return switch (email) {
            case "orgadmin1@assetdock.dev" -> issueToken(ORG_ADMIN_1, ORG_1, email, UserRole.ORG_ADMIN);
            case "manager1@assetdock.dev"  -> issueToken(ASSET_MANAGER, ORG_1, email, UserRole.ASSET_MANAGER);
            case "auditor1@assetdock.dev"  -> issueToken(AUDITOR_1, ORG_1, email, UserRole.AUDITOR);
            case "viewer1@assetdock.dev"   -> issueToken(VIEWER_1, ORG_1, email, UserRole.VIEWER);
            default -> throw new IllegalArgumentException("Unsupported test user: " + email);
        };
    }

    private String issueToken(UUID userId, UUID orgId, String email, UserRole... roles) {
        return jwtTokenService.issue(
                new AuthenticatedUserPrincipal(userId, orgId, email, java.util.Set.of(roles)),
                java.time.Instant.now()
        ).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void insertOrganization(UUID id, String slug) {
        jdbcTemplate.update(
                "INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
                id, slug, slug);
    }

    private void insertUser(UUID userId, UUID orgId, String email, String role) {
        jdbcTemplate.update("""
                INSERT INTO users (id, organization_id, email, full_name, password_hash, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE'::user_status)
                """,
                userId, orgId, email, email, passwordEncoder.encode("S3curePass!"));
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role) VALUES (?, ?::user_role)
                """,
                userId, role);
        jdbcTemplate.update("""
                INSERT INTO people (id, organization_id, full_name, email, active)
                VALUES (?, ?, ?, ?, true)
                """,
                userId, orgId, email, email);
    }

    private void insertCategory(UUID id, UUID orgId, String name) {
        jdbcTemplate.update("""
                INSERT INTO categories (id, organization_id, name, active, created_at, updated_at)
                VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, orgId, name);
    }

    private void insertManufacturer(UUID id, UUID orgId, String name) {
        jdbcTemplate.update("""
                INSERT INTO manufacturers (id, organization_id, name, active, created_at, updated_at)
                VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, orgId, name);
    }

    private void insertLocation(UUID id, UUID orgId, String name) {
        jdbcTemplate.update("""
                INSERT INTO locations (id, organization_id, name, active, created_at, updated_at)
                VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, orgId, name);
    }

    private void insertAsset(UUID id, UUID orgId, String tag,
                             UUID categoryId, UUID manufacturerId, UUID locationId,
                             UUID assignedPersonId, String status) {
        jdbcTemplate.update("""
                INSERT INTO assets (
                    id, organization_id, asset_tag, display_name,
                    category_id, manufacturer_id, current_location_id,
                    current_assigned_person_id, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS asset_status), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, orgId, tag, tag,
                categoryId, manufacturerId, locationId,
                assignedPersonId, status);
    }

    private void insertActiveCheckout(UUID id, UUID orgId, UUID assetId, UUID personId, UUID checkedOutBy) {
        jdbcTemplate.update("""
                INSERT INTO asset_checkouts (
                    id, organization_id, asset_id, person_id,
                    checked_out_at, checked_out_by, created_at
                ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)
                """,
                id, orgId, assetId, personId, checkedOutBy);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM asset_checkouts");
        jdbcTemplate.update("DELETE FROM asset_assignments");
        jdbcTemplate.update("DELETE FROM assets");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM manufacturers");
        jdbcTemplate.update("DELETE FROM locations");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM people");
        jdbcTemplate.update("DELETE FROM organizations");
    }
}
