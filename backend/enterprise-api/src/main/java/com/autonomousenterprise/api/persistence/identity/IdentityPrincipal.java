package com.autonomousenterprise.api.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "identity_principals")
public class IdentityPrincipal extends AuditedUuidEntity {

	@Id
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "identity_principal_id", nullable = false, updatable = false, length = 16, columnDefinition = "BINARY(16)")
	private UUID identityPrincipalId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
	private IdentityPrincipalStatus status;

	@Column(name = "disabled_at", columnDefinition = "DATETIME(6)")
	private Instant disabledAt;

	public IdentityPrincipal() {
		this(UUID.randomUUID());
	}

	public IdentityPrincipal(UUID identityPrincipalId) {
		this.identityPrincipalId = Objects.requireNonNull(identityPrincipalId, "Identity principal ID is required.");
		this.status = IdentityPrincipalStatus.ACTIVE;
	}

	public UUID getIdentityPrincipalId() {
		return identityPrincipalId;
	}

	public IdentityPrincipalStatus getStatus() {
		return status;
	}

	public Instant getDisabledAt() {
		return disabledAt;
	}

	public void transitionTo(IdentityPrincipalStatus target) {
		transitionTo(target, Instant.now());
	}

	public void transitionTo(IdentityPrincipalStatus target, Instant at) {
		Objects.requireNonNull(target, "Target status is required.");
		Objects.requireNonNull(at, "Transition time is required.");
		if (status == target || (status == IdentityPrincipalStatus.DISABLED && target != IdentityPrincipalStatus.ACTIVE)) {
			throw new IllegalStateException("Identity principal status transition is not permitted.");
		}
		status = target;
		disabledAt = target == IdentityPrincipalStatus.DISABLED ? at : null;
	}

	@PrePersist
	private void ensureIdentityPrincipalId() {
		if (identityPrincipalId == null) {
			identityPrincipalId = UUID.randomUUID();
		}
	}
}
