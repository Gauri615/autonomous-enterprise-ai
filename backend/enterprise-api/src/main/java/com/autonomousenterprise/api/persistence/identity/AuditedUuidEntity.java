package com.autonomousenterprise.api.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@MappedSuperclass
public abstract class AuditedUuidEntity {

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
	private Instant updatedAt;

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@PrePersist
	protected void initializeTimestamps() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	protected void updateTimestamp() {
		updatedAt = Instant.now();
	}

	@PreRemove
	protected void rejectHardDelete() {
		throw new IllegalStateException("Hard deletion is not permitted for identity foundation records.");
	}
}
