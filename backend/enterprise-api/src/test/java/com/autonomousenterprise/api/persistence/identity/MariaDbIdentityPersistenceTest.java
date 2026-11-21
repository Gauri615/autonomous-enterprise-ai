package com.autonomousenterprise.api.persistence.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
@EnabledIfSystemProperty(named = "mariadb.integration.enabled", matches = "true")
class MariaDbIdentityPersistenceTest {

	private static final UUID FIXED_UUID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
	private static final byte[] FIXED_UUID_BYTES = {
			0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
			(byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
			(byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
	};
	private static final Pattern SAFE_TEST_URL =
			Pattern.compile("^jdbc:mariadb://([^/?#]+)/([A-Za-z0-9_]+)$");

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
	private PlatformTransactionManager transactionManager;

	@DynamicPropertySource
	static void configureDedicatedTestDatabase(DynamicPropertyRegistry registry) {
		String url = requiredEnvironmentVariable("MARIADB_IT_URL");
		Matcher matcher = SAFE_TEST_URL.matcher(url);
		if (!matcher.matches()
				|| matcher.group(1).contains("@")
				|| matcher.group(2).equalsIgnoreCase("autonomous_enterprise_ai")
				|| !matcher.group(2).endsWith("_it")) {
			throw new IllegalStateException(
					"MARIADB_IT_URL must target a dedicated MariaDB database ending in _it without URL credentials or query parameters.");
		}
		String username = requiredEnvironmentVariable("MARIADB_IT_USER");
		String password = requiredEnvironmentVariable("MARIADB_IT_PASSWORD");
		registry.add("spring.datasource.url", () -> url);
		registry.add("spring.datasource.username", () -> username);
		registry.add("spring.datasource.password", () -> password);
		registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
	}

	@Test
	void persistsFixedUuidAndIdenticalForeignKeyBytes() {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(FIXED_UUID, new OrganizationKey(uniqueSlug("uuid")), "UUID Round Trip"));
		assertThat(organizationRepository.findById(FIXED_UUID)).isPresent();
		assertThat(rawBytes("SELECT organization_id FROM organizations WHERE organization_id = ?", FIXED_UUID))
				.containsExactly(FIXED_UUID_BYTES);

		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		OrganizationMembership membership = membershipRepository.saveAndFlush(
				new OrganizationMembership(organization, principal, Instant.now()));
		assertThat(rawBytes(
				"SELECT organization_id FROM organization_memberships WHERE membership_id = ?",
				membership.getMembershipId()))
				.containsExactly(FIXED_UUID_BYTES);
		assertThat(rawBytes(
				"SELECT identity_principal_id FROM organization_memberships WHERE membership_id = ?",
				membership.getMembershipId()))
				.containsExactly(uuidBytes(principal.getIdentityPrincipalId()));
	}

	@Test
	void externalIdentityUsesExactIssuerSubjectBytesAndUniquePair() {
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.now();
		String issuer = "https://id.example/tenant-é";
		ExternalIdentity original = externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal, new OidcIssuer(issuer), new ExternalSubject("subject"), at, at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal, new OidcIssuer(issuer), new ExternalSubject("Subject"), at, at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal, new OidcIssuer(issuer), new ExternalSubject("subject "), at, at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal, new OidcIssuer("https://ID.example/tenant-é"), new ExternalSubject("subject"), at, at));
		externalIdentityRepository.saveAndFlush(new ExternalIdentity(
				principal, new OidcIssuer("https://id.example/tenant-é/"), new ExternalSubject("subject"), at, at));
		assertThat(rawBytes(
				"SELECT issuer FROM external_identities WHERE external_identity_id = ?",
				original.getExternalIdentityId()))
				.containsExactly(issuer.getBytes(StandardCharsets.UTF_8));
		assertThat(rawBytes(
				"SELECT subject FROM external_identities WHERE external_identity_id = ?",
				original.getExternalIdentityId()))
				.containsExactly("subject".getBytes(StandardCharsets.US_ASCII));
		assertThat(rawBytes(
				"SELECT identity_principal_id FROM external_identities WHERE external_identity_id = ?",
				original.getExternalIdentityId()))
				.containsExactly(uuidBytes(principal.getIdentityPrincipalId()));

		IdentityPrincipal anotherPrincipal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
				externalIdentityRepository.saveAndFlush(new ExternalIdentity(
						anotherPrincipal, new OidcIssuer(issuer), new ExternalSubject("subject"), at, at)));
	}

	@Test
	void generatedMembershipIndexAllowsRevokedEpisodesAndRejectsConcurrentLiveDuplicates() throws Exception {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(new OrganizationKey(uniqueSlug("membership")), "Membership Test"));
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		Instant at = Instant.now();
		for (int index = 0; index < 2; index++) {
			OrganizationMembership revoked =
					new OrganizationMembership(organization, principal, at.minusSeconds(30));
			revoked.transitionTo(OrganizationMembershipStatus.REVOKED, at);
			membershipRepository.saveAndFlush(revoked);
		}

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> first = executor.submit(() -> tryInsertLiveMembership(
					organization, principal, at, ready, start));
			Future<Boolean> second = executor.submit(() -> tryInsertLiveMembership(
					organization, principal, at, ready, start));
			ready.await();
			start.countDown();
			assertThat(first.get()).isNotEqualTo(second.get());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void databaseRejectsInvalidLifecycleStatusAndRestrictsParentDeletion() {
		Organization organization = organizationRepository.saveAndFlush(
				new Organization(new OrganizationKey(uniqueSlug("restrict")), "Restrictive FK"));
		IdentityPrincipal principal = identityPrincipalRepository.saveAndFlush(new IdentityPrincipal());
		membershipRepository.saveAndFlush(new OrganizationMembership(organization, principal, Instant.now()));

		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
				jdbcTemplate.update(
						"UPDATE organizations SET status = ? WHERE organization_id = ?",
						"CLOSED ",
						uuidBytes(organization.getOrganizationId())));
		assertThatExceptionOfType(DataAccessException.class).isThrownBy(() ->
				jdbcTemplate.update(
						"DELETE FROM identity_principals WHERE identity_principal_id = ?",
						uuidBytes(principal.getIdentityPrincipalId())));
	}

	private boolean tryInsertLiveMembership(
			Organization organization,
			IdentityPrincipal principal,
			Instant at,
			CountDownLatch ready,
			CountDownLatch start) throws InterruptedException {
		ready.countDown();
		start.await();
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		try {
			transaction.execute(status -> membershipRepository.saveAndFlush(
					new OrganizationMembership(organization, principal, at)));
			return true;
		} catch (org.springframework.dao.DataIntegrityViolationException exception) {
			return false;
		}
	}

	private byte[] rawBytes(String sql, UUID id) {
		return jdbcTemplate.queryForObject(sql, byte[].class, uuidBytes(id));
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " must be set when MariaDB integration tests are enabled.");
		}
		return value;
	}

	private static String uniqueSlug(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
	}

	private static byte[] uuidBytes(UUID uuid) {
		return ByteBuffer.allocate(16)
				.putLong(uuid.getMostSignificantBits())
				.putLong(uuid.getLeastSignificantBits())
				.array();
	}
}
