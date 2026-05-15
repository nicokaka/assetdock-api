package com.assetdock.api.checkout.infrastructure;

import com.assetdock.api.checkout.domain.AssetCheckout;
import com.assetdock.api.checkout.domain.AssetCheckoutRepository;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAssetCheckoutRepository implements AssetCheckoutRepository {

    private final JdbcClient jdbcClient;

    public JdbcAssetCheckoutRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public AssetCheckout save(AssetCheckout checkout) {
        jdbcClient.sql("""
            INSERT INTO asset_checkouts (
                id,
                organization_id,
                asset_id,
                user_id,
                checked_out_at,
                expected_return_date,
                checked_in_at,
                checked_out_by,
                checked_in_by,
                notes,
                created_at
            ) VALUES (
                :id,
                :organizationId,
                :assetId,
                :userId,
                :checkedOutAt,
                :expectedReturnDate,
                :checkedInAt,
                :checkedOutBy,
                :checkedInBy,
                :notes,
                :createdAt
            )
            """)
            .param("id", checkout.id())
            .param("organizationId", checkout.organizationId())
            .param("assetId", checkout.assetId())
            .param("userId", checkout.userId())
            .param("checkedOutAt", checkout.checkedOutAt())
            .param("expectedReturnDate", checkout.expectedReturnDate())
            .param("checkedInAt", checkout.checkedInAt())
            .param("checkedOutBy", checkout.checkedOutBy())
            .param("checkedInBy", checkout.checkedInBy())
            .param("notes", checkout.notes())
            .param("createdAt", checkout.createdAt())
            .update();

        return checkout;
    }

    @Override
    public AssetCheckout update(AssetCheckout checkout) {
        jdbcClient.sql("""
            UPDATE asset_checkouts SET
                checked_in_at = :checkedInAt,
                checked_in_by = :checkedInBy,
                notes = :notes
            WHERE id = :id
            """)
            .param("id", checkout.id())
            .param("checkedInAt", checkout.checkedInAt())
            .param("checkedInBy", checkout.checkedInBy())
            .param("notes", checkout.notes())
            .update();

        return checkout;
    }

    @Override
    public List<AssetCheckout> findByAssetIdAndOrganizationIdOrderByCheckedOutAtDesc(UUID assetId, UUID organizationId) {
        return jdbcClient.sql("""
            SELECT * FROM asset_checkouts 
            WHERE asset_id = :assetId AND organization_id = :organizationId
            ORDER BY checked_out_at DESC
            """)
            .param("assetId", assetId)
            .param("organizationId", organizationId)
            .query(this::mapRow)
            .list();
    }

    @Override
    public Optional<AssetCheckout> findActiveByAssetIdAndOrganizationIdForUpdate(UUID assetId, UUID organizationId) {
        return jdbcClient.sql("""
            SELECT * FROM asset_checkouts
            WHERE asset_id = :assetId
              AND organization_id = :organizationId
              AND checked_in_at IS NULL
            FOR UPDATE
            """)
            .param("assetId", assetId)
            .param("organizationId", organizationId)
            .query(this::mapRow)
            .optional();
    }

    @Override
    public List<AssetCheckout> findByUserIdAndOrganizationIdOrderByCheckedOutAtDesc(UUID userId, UUID organizationId) {
        return jdbcClient.sql("""
            SELECT * FROM asset_checkouts 
            WHERE user_id = :userId AND organization_id = :organizationId
            ORDER BY checked_out_at DESC
            """)
            .param("userId", userId)
            .param("organizationId", organizationId)
            .query(this::mapRow)
            .list();
    }

    @Override
    public int countActiveCheckoutsForOrganization(UUID organizationId) {
        Integer count = jdbcClient.sql("""
            SELECT COUNT(*) FROM asset_checkouts
            WHERE organization_id = :organizationId
              AND checked_in_at IS NULL
            """)
            .param("organizationId", organizationId)
            .query(Integer.class)
            .single();
        return count != null ? count : 0;
    }

    private AssetCheckout mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AssetCheckout(
            rs.getObject("id", UUID.class),
            rs.getObject("organization_id", UUID.class),
            rs.getObject("asset_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            JdbcColumnReaders.getInstant(rs, "checked_out_at"),
            JdbcColumnReaders.getInstant(rs, "expected_return_date"),
            JdbcColumnReaders.getInstant(rs, "checked_in_at"),
            rs.getObject("checked_out_by", UUID.class),
            rs.getObject("checked_in_by", UUID.class),
            rs.getString("notes"),
            JdbcColumnReaders.getInstant(rs, "created_at")
        );
    }
}
