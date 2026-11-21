package com.autonomousenterprise.api.persistence.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	Optional<Organization> findByOrganizationKeyBytes(byte[] organizationKeyBytes);

	default Optional<Organization> findByOrganizationKey(OrganizationKey organizationKey) {
		return findByOrganizationKeyBytes(organizationKey.toBytes());
	}
}
