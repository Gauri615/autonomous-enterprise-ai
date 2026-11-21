package com.autonomousenterprise.api.persistence.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {

	Optional<ExternalIdentity> findByIssuerBytesAndSubjectBytes(byte[] issuerBytes, byte[] subjectBytes);

	default Optional<ExternalIdentity> findByIssuerAndSubject(OidcIssuer issuer, ExternalSubject subject) {
		return findByIssuerBytesAndSubjectBytes(issuer.toBytes(), subject.toBytes());
	}
}
