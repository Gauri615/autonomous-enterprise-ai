package com.autonomousenterprise.api.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "external_identities",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_external_identity_issuer_subject",
				columnNames = { "issuer", "subject" }))
public class ExternalIdentity extends AuditedUuidEntity {

	@Id
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "external_identity_id", nullable = false, updatable = false, length = 16, columnDefinition = "BINARY(16)")
	private UUID externalIdentityId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "identity_principal_id",
			nullable = false,
			updatable = false,
			columnDefinition = "BINARY(16)",
			foreignKey = @ForeignKey(name = "fk_external_identity_principal"))
	private IdentityPrincipal identityPrincipal;

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(name = "issuer", nullable = false, updatable = false, length = 480, columnDefinition = "VARBINARY(480)")
	private byte[] issuerBytes;

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(name = "subject", nullable = false, updatable = false, length = 255, columnDefinition = "VARBINARY(255)")
	private byte[] subjectBytes;

	@Column(name = "verified_at", nullable = false, columnDefinition = "DATETIME(6)")
	private Instant verifiedAt;

	@Column(name = "linked_at", nullable = false, columnDefinition = "DATETIME(6)")
	private Instant linkedAt;

	@Column(name = "unlinked_at", columnDefinition = "DATETIME(6)")
	private Instant unlinkedAt;

	protected ExternalIdentity() {
	}

	public ExternalIdentity(
			IdentityPrincipal identityPrincipal,
			OidcIssuer issuer,
			ExternalSubject subject,
			Instant verifiedAt,
			Instant linkedAt) {
		this(UUID.randomUUID(), identityPrincipal, issuer, subject, verifiedAt, linkedAt);
	}

	public ExternalIdentity(
			UUID externalIdentityId,
			IdentityPrincipal identityPrincipal,
			OidcIssuer issuer,
			ExternalSubject subject,
			Instant verifiedAt,
			Instant linkedAt) {
		this.externalIdentityId = Objects.requireNonNull(externalIdentityId, "External identity ID is required.");
		this.identityPrincipal = Objects.requireNonNull(identityPrincipal, "Identity principal is required.");
		this.issuerBytes = Objects.requireNonNull(issuer, "OIDC issuer is required.").toBytes();
		this.subjectBytes = Objects.requireNonNull(subject, "OIDC subject is required.").toBytes();
		this.verifiedAt = Objects.requireNonNull(verifiedAt, "Verification time is required.");
		this.linkedAt = Objects.requireNonNull(linkedAt, "Link time is required.");
		if (linkedAt.isBefore(verifiedAt)) {
			throw new IllegalArgumentException("Link time cannot precede identity verification.");
		}
	}

	public UUID getExternalIdentityId() {
		return externalIdentityId;
	}

	public IdentityPrincipal getIdentityPrincipal() {
		return identityPrincipal;
	}

	public OidcIssuer getIssuer() {
		return OidcIssuer.fromBytes(issuerBytes.clone());
	}

	public ExternalSubject getSubject() {
		return ExternalSubject.fromBytes(subjectBytes.clone());
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public Instant getLinkedAt() {
		return linkedAt;
	}

	public Instant getUnlinkedAt() {
		return unlinkedAt;
	}

	public boolean isLinked() {
		return unlinkedAt == null;
	}

	public void unlink(Instant at) {
		Objects.requireNonNull(at, "Unlink time is required.");
		if (!isLinked() || at.isBefore(linkedAt)) {
			throw new IllegalStateException("External identity cannot be unlinked in its current state.");
		}
		unlinkedAt = at;
	}

	public void relink(Instant verifiedAt, Instant linkedAt) {
		Objects.requireNonNull(verifiedAt, "Verification time is required.");
		Objects.requireNonNull(linkedAt, "Link time is required.");
		if (isLinked()
				|| !verifiedAt.isAfter(unlinkedAt)
				|| linkedAt.isBefore(verifiedAt)
				|| linkedAt.isBefore(unlinkedAt)) {
			throw new IllegalStateException("External identity can only be relinked after fresh verification.");
		}
		this.verifiedAt = verifiedAt;
		this.linkedAt = linkedAt;
		this.unlinkedAt = null;
	}

	@PrePersist
	private void ensureExternalIdentityId() {
		if (externalIdentityId == null) {
			externalIdentityId = UUID.randomUUID();
		}
	}
}
