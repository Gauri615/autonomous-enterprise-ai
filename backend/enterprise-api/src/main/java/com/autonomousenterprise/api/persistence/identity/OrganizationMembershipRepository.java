package com.autonomousenterprise.api.persistence.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

	List<OrganizationMembership> findAllByOrganization_OrganizationIdAndIdentityPrincipal_IdentityPrincipalId(
			UUID organizationId,
			UUID identityPrincipalId);
}
