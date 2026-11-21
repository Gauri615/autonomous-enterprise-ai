package com.autonomousenterprise.api.persistence.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityPrincipalRepository extends JpaRepository<IdentityPrincipal, UUID> {
}
