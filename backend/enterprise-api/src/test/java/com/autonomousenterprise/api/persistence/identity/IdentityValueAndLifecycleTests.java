package com.autonomousenterprise.api.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IdentityValueAndLifecycleTests {

	@Test
	void organizationKeysRequireCanonicalLowercaseAsciiSlugs() {
		assertThat(new OrganizationKey("enterprise-1").toBytes())
				.containsExactly("enterprise-1".getBytes(StandardCharsets.US_ASCII));
		assertThat(OrganizationKey.fromBytes(new OrganizationKey("a".repeat(100)).toBytes()).value())
				.hasSize(100);

		assertThatIllegalArgumentException().isThrownBy(() -> new OrganizationKey(""));
		assertThatIllegalArgumentException().isThrownBy(() -> new OrganizationKey("Enterprise"));
		assertThatIllegalArgumentException().isThrownBy(() -> new OrganizationKey("enterprise-"));
		assertThatIllegalArgumentException().isThrownBy(() -> new OrganizationKey("a".repeat(101)));
	}

	@Test
	void issuerUsesStrictUtf8BytesWithoutNormalization() {
		String issuerValue = "https://id.example/tenant-é";
		OidcIssuer issuer = new OidcIssuer(issuerValue);
		assertThat(issuer.toBytes()).containsExactly(issuerValue.getBytes(StandardCharsets.UTF_8));
		assertThat(OidcIssuer.fromBytes(issuer.toBytes()).value()).isEqualTo(issuerValue);
		assertThat(new OidcIssuer("https://ID.example/issuer").value()).isEqualTo("https://ID.example/issuer");

		String prefix = "https://id.example/";
		assertThat(new OidcIssuer(prefix + "a".repeat(480 - prefix.length())).toBytes()).hasSize(480);
		assertThatIllegalArgumentException().isThrownBy(
				() -> new OidcIssuer(prefix + "a".repeat(481 - prefix.length())));
		assertThatIllegalArgumentException().isThrownBy(() -> new OidcIssuer(" https://id.example"));
		assertThatIllegalArgumentException().isThrownBy(() -> new OidcIssuer("https://id.example/path?query"));
		assertThatIllegalArgumentException().isThrownBy(() -> new OidcIssuer("https://id.example/path#fragment"));
		assertThatIllegalArgumentException().isThrownBy(() -> new OidcIssuer("https://id.example/\uD800"));
	}

	@Test
	void subjectsAreExactAsciiIdentifiersAndPreserveTrailingBytes() {
		assertThat(new ExternalSubject("subject ").toBytes())
				.containsExactly("subject ".getBytes(StandardCharsets.US_ASCII));
		assertThat(ExternalSubject.fromBytes("subject ".getBytes(StandardCharsets.US_ASCII)).value())
				.isEqualTo("subject ");
		assertThat(new ExternalSubject("a".repeat(255)).toBytes()).hasSize(255);

		assertThatIllegalArgumentException().isThrownBy(() -> new ExternalSubject(""));
		assertThatIllegalArgumentException().isThrownBy(() -> new ExternalSubject("é"));
		assertThatIllegalArgumentException().isThrownBy(() -> new ExternalSubject("a".repeat(256)));
		assertThatIllegalArgumentException().isThrownBy(() -> ExternalSubject.fromBytes(new byte[] { (byte) 0x80 }));
	}

	@Test
	void lifecycleTransitionsFollowTheCanonicalStateMachines() {
		Organization organization = new Organization(new OrganizationKey("tenant"), "Tenant");
		organization.transitionTo(OrganizationStatus.SUSPENDED);
		organization.transitionTo(OrganizationStatus.ACTIVE);
		organization.transitionTo(OrganizationStatus.CLOSED);
		assertThatIllegalStateException().isThrownBy(() -> organization.transitionTo(OrganizationStatus.ACTIVE));

		IdentityPrincipal principal = new IdentityPrincipal();
		Instant disabledAt = Instant.parse("2026-01-01T00:00:00Z");
		principal.transitionTo(IdentityPrincipalStatus.DISABLED, disabledAt);
		assertThat(principal.getDisabledAt()).isEqualTo(disabledAt);
		principal.transitionTo(IdentityPrincipalStatus.ACTIVE, disabledAt.plusSeconds(1));
		assertThat(principal.getDisabledAt()).isNull();

		OrganizationMembership membership = new OrganizationMembership(
				organization, principal, Instant.parse("2026-01-01T00:00:00Z"));
		membership.transitionTo(OrganizationMembershipStatus.ACTIVE, Instant.parse("2026-01-01T00:01:00Z"));
		membership.transitionTo(OrganizationMembershipStatus.SUSPENDED, Instant.parse("2026-01-01T00:02:00Z"));
		membership.transitionTo(OrganizationMembershipStatus.ACTIVE, Instant.parse("2026-01-01T00:03:00Z"));
		membership.transitionTo(OrganizationMembershipStatus.REVOKED, Instant.parse("2026-01-01T00:04:00Z"));
		assertThat(membership.getRevokedAt()).isEqualTo(Instant.parse("2026-01-01T00:04:00Z"));
		assertThatIllegalStateException().isThrownBy(
				() -> membership.transitionTo(OrganizationMembershipStatus.ACTIVE, Instant.now()));
		assertThatIllegalStateException().isThrownBy(
				() -> new OrganizationMembership(organization, principal, null)
						.transitionTo(OrganizationMembershipStatus.SUSPENDED, Instant.now()));

		ExternalIdentity externalIdentity = new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example"),
				new ExternalSubject("subject"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:01Z"));
		externalIdentity.unlink(Instant.parse("2026-01-01T00:01:00Z"));
		externalIdentity.relink(
				Instant.parse("2026-01-01T00:02:00Z"),
				Instant.parse("2026-01-01T00:02:01Z"));
		assertThat(externalIdentity.isLinked()).isTrue();
		assertThatIllegalStateException().isThrownBy(
				() -> externalIdentity.relink(Instant.now(), Instant.now()));
	}
}
