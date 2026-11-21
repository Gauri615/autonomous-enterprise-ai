package com.autonomousenterprise.api.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "organizations",
		uniqueConstraints = @UniqueConstraint(name = "uq_organizations_key", columnNames = "organization_key"))
public class Organization extends AuditedUuidEntity {

	@Id
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "organization_id", nullable = false, updatable = false, length = 16, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(name = "organization_key", nullable = false, updatable = false, length = 100, columnDefinition = "VARBINARY(100)")
	private byte[] organizationKeyBytes;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
	private OrganizationStatus status;

	protected Organization() {
	}

	public Organization(OrganizationKey organizationKey, String displayName) {
		this(UUID.randomUUID(), organizationKey, displayName);
	}

	public Organization(UUID organizationId, OrganizationKey organizationKey, String displayName) {
		this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required.");
		this.organizationKeyBytes = Objects.requireNonNull(organizationKey, "Organization key is required.").toBytes();
		this.displayName = validateDisplayName(displayName);
		this.status = OrganizationStatus.ACTIVE;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public OrganizationKey getOrganizationKey() {
		return OrganizationKey.fromBytes(organizationKeyBytes.clone());
	}

	public String getDisplayName() {
		return displayName;
	}

	public OrganizationStatus getStatus() {
		return status;
	}

	public void transitionTo(OrganizationStatus target) {
		Objects.requireNonNull(target, "Target status is required.");
		boolean allowed = (status == OrganizationStatus.ACTIVE
				&& (target == OrganizationStatus.SUSPENDED || target == OrganizationStatus.CLOSED))
				|| (status == OrganizationStatus.SUSPENDED
						&& (target == OrganizationStatus.ACTIVE || target == OrganizationStatus.CLOSED));
		if (!allowed) {
			throw new IllegalStateException("Organization status transition is not permitted.");
		}
		status = target;
	}

	@PrePersist
	private void ensureOrganizationId() {
		if (organizationId == null) {
			organizationId = UUID.randomUUID();
		}
	}

	private static String validateDisplayName(String displayName) {
		Objects.requireNonNull(displayName, "Organization display name is required.");
		if (displayName.isBlank() || displayName.length() > 255) {
			throw new IllegalArgumentException("Organization display name must contain 1-255 characters.");
		}
		return displayName;
	}
}
