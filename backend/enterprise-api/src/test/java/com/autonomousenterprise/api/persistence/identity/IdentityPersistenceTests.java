package com.autonomousenterprise.api.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/identity-schema-h2.sql")
@Transactional
class IdentityPersistenceTests {

	private static final UUID FIXED_UUID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
	private static final byte[] FIXED_UUID_BYTES = {
			0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
			(byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
			(byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
	};

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private IdentityPrincipalRepository identityPrincipalRepository;

	@Autowired
	private ExternalIdentityRepository externalIdentityRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void hibernateRoundTripsCanonicalUuidBytesForPrimaryAndForeignKeys() {
		assertThat(uuidBytes(FIXED_UUID)).containsExactly(FIXED_UUID_BYTES);

		Organization organization = organizationRepository.saveAndFlush(
				new Organization(FIXED_UUID, new OrganizationKey("round-trip-org"), "Round Trip"));
		assertThat(organizationRepository.findById(FIXED_UUID)).contains(organization);
		assertThat(rawBytes("SELECT organization_id FROM organizations WHERE organization_id = ?", FIXED_UUID))
				.containsExactly(FIXED_UUID_BYTES);

		UUID principalId = UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100");
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal(principalId));
		ExternalIdentity externalIdentity = externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example/tenant-é"),
				new ExternalSubject("Case-Sensitive "),
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-02-01T00:00:01Z")));
		assertThat(externalIdentityRepository.findById(externalIdentity.getExternalIdentityId()))
				.hasValueSatisfying(found ->
						assertThat(found.getIssuer().value()).isEqualTo("https://id.example/tenant-é"));
		assertThat(rawBytes(
				"SELECT issuer FROM external_identities WHERE external_identity_id = ?",
				externalIdentity.getExternalIdentityId()))
				.containsExactly("https://id.example/tenant-é".getBytes(StandardCharsets.UTF_8));
		assertThat(rawBytes(
				"SELECT subject FROM external_identities WHERE external_identity_id = ?",
				externalIdentity.getExternalIdentityId()))
				.containsExactly("Case-Sensitive ".getBytes(StandardCharsets.US_ASCII));
		assertThat(rawBytes(
				"SELECT identity_principal_id FROM external_identities WHERE external_identity_id = ?",
				externalIdentity.getExternalIdentityId()))
				.containsExactly(uuidBytes(principalId));

		OrganizationMembership membership = membershipRepository.saveAndFlush(new OrganizationMembership(
				UUID.fromString("10213243-5465-7687-98a9-bacbdcedfe0f"),
				organization,
				principal,
				Instant.parse("2026-02-01T00:01:00Z")));
		assertThat(rawBytes(
				"SELECT organization_id FROM organization_memberships WHERE membership_id = ?",
				membership.getMembershipId()))
				.containsExactly(FIXED_UUID_BYTES);
		assertThat(rawBytes(
				"SELECT identity_principal_id FROM organization_memberships WHERE membership_id = ?",
				membership.getMembershipId()))
				.containsExactly(uuidBytes(principalId));
	}

	@Test
	void generatedMembershipKeyAllowsRevokedHistoryAndOneLiveEpisode() {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(new OrganizationKey("membership-history"), "Membership History"));
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.parse("2026-02-01T00:00:00Z");

		for (int index = 0; index < 2; index++) {
			OrganizationMembership revoked =
					new OrganizationMembership(organization, principal, at.minusSeconds(30));
			revoked.transitionTo(OrganizationMembershipStatus.REVOKED, at);
			membershipRepository.saveAndFlush(revoked);
		}
		membershipRepository.saveAndFlush(new OrganizationMembership(organization, principal, at));

		assertThat(membershipRepository
						.findAllByOrganization_OrganizationIdAndIdentityPrincipal_IdentityPrincipalId(
								organization.getOrganizationId(),
								principal.getIdentityPrincipalId()))
				.hasSize(3);
	}

	@Test
	void exactExternalIdentityPairIsUniqueButCaseAndTrailingBytesRemainDistinct() {
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.parse("2026-02-01T00:00:00Z");
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example"),
				new ExternalSubject("subject"),
				at,
				at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example"),
				new ExternalSubject("Subject"),
				at,
				at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example"),
				new ExternalSubject("subject "),
				at,
				at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://ID.example"),
				new ExternalSubject("subject"),
				at,
				at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://id.example/"),
				new ExternalSubject("subject"),
				at,
				at));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM external_identities", Integer.class))
				.isEqualTo(5);
	}

	@Test
	void databaseUniqueIndexRejectsDuplicateExternalIdentityPair() {
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.parse("2026-02-01T00:00:00Z");
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal,
				new OidcIssuer("https://unique.example"),
				new ExternalSubject("same"),
				at,
				at));
		entityManager.flush();
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
				externalIdentityRepository.saveAndFlush(new ExternalIdentity(
						principal,
						new OidcIssuer("https://unique.example"),
						new ExternalSubject("same"),
						at,
						at)));
	}

	@Test
	void databaseUniqueIndexRejectsDuplicateLiveMembership() {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(new OrganizationKey("unique-live-member"), "Unique Live Member"));
		IdentityPrincipal anotherPrincipal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.parse("2026-02-01T00:00:00Z");
		membershipRepository.saveAndFlush(new OrganizationMembership(organization, anotherPrincipal, at));
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() ->
				membershipRepository.saveAndFlush(new OrganizationMembership(organization, anotherPrincipal, at)));
	}

	@Test
	void hardDeletionIsRejectedByEntityLifecycleGuard() {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(new OrganizationKey("no-delete"), "No Delete"));
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			entityManager.remove(organization);
			entityManager.flush();
		});
	}

	private byte[] rawBytes(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, byte[].class, uuidBytes(id));
	}

	private static byte[] uuidBytes(UUID uuid) {
		return ByteBuffer.allocate(16)
				.putLong(uuid.getMostSignificantBits())
				.putLong(uuid.getLeastSignificantBits())
				.array();
	}
}
