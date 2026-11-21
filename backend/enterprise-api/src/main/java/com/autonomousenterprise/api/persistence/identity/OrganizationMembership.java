package com.autonomousenterprise.api.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "organization_memberships",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_membership_org_membership",
				columnNames = { "organization_id", "membership_id" }))
public class OrganizationMembership extends AuditedUuidEntity {

	@Id
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "membership_id", nullable = false, updatable = false, length = 16, columnDefinition = "BINARY(16)")
	private UUID membershipId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "organization_id",
			nullable = false,
			updatable = false,
			columnDefinition = "BINARY(16)",
			foreignKey = @ForeignKey(name = "fk_membership_organization"))
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "identity_principal_id",
			nullable = false,
			updatable = false,
			columnDefinition = "BINARY(16)",
			foreignKey = @ForeignKey(name = "fk_membership_principal"))
	private IdentityPrincipal identityPrincipal;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
	private OrganizationMembershipStatus status;

	@Column(name = "invited_at", columnDefinition = "DATETIME(6)")
	private Instant invitedAt;

	@Column(name = "activated_at", columnDefinition = "DATETIME(6)")
	private Instant activatedAt;

	@Column(name = "revoked_at", columnDefinition = "DATETIME(6)")
	private Instant revokedAt;

	protected OrganizationMembership() {
	}

	public OrganizationMembership(Organization organization, IdentityPrincipal identityPrincipal, Instant invitedAt) {
		this(UUID.randomUUID(), organization, identityPrincipal, invitedAt);
	}

	public OrganizationMembership(
			UUID membershipId,
			Organization organization,
			IdentityPrincipal identityPrincipal,
			Instant invitedAt) {
		this.membershipId = Objects.requireNonNull(membershipId, "Membership ID is required.");
		this.organization = Objects.requireNonNull(organization, "Organization is required.");
		this.identityPrincipal = Objects.requireNonNull(identityPrincipal, "Identity principal is required.");
		this.invitedAt = invitedAt;
		this.status = OrganizationMembershipStatus.PENDING;
	}

	public UUID getMembershipId() {
		return membershipId;
	}

	public Organization getOrganization() {
		return organization;
	}

	public IdentityPrincipal getIdentityPrincipal() {
		return identityPrincipal;
	}

	public OrganizationMembershipStatus getStatus() {
		return status;
	}

	public Instant getInvitedAt() {
		return invitedAt;
	}

	public Instant getActivatedAt() {
		return activatedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void transitionTo(OrganizationMembershipStatus target, Instant at) {
		Objects.requireNonNull(target, "Target status is required.");
		Objects.requireNonNull(at, "Transition time is required.");
		boolean allowed = (status == OrganizationMembershipStatus.PENDING
				&& target == OrganizationMembershipStatus.ACTIVE)
				|| (status == OrganizationMembershipStatus.PENDING
						&& target == OrganizationMembershipStatus.REVOKED)
				|| (status == OrganizationMembershipStatus.ACTIVE
						&& (target == OrganizationMembershipStatus.SUSPENDED || target == OrganizationMembershipStatus.REVOKED))
				|| (status == OrganizationMembershipStatus.SUSPENDED
						&& (target == OrganizationMembershipStatus.ACTIVE || target == OrganizationMembershipStatus.REVOKED));
		if (!allowed || (invitedAt != null && at.isBefore(invitedAt))) {
			throw new IllegalStateException("Organization membership status transition is not permitted.");
		}
		status = target;
		if (target == OrganizationMembershipStatus.ACTIVE && activatedAt == null) {
			activatedAt = at;
		}
		if (target == OrganizationMembershipStatus.REVOKED) {
			revokedAt = at;
		}
	}

	@PrePersist
	private void ensureMembershipId() {
		if (membershipId == null) {
			membershipId = UUID.randomUUID();
		}
	}
}
