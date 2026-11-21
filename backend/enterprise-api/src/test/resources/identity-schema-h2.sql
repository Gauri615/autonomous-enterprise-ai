CREATE TABLE IF NOT EXISTS organizations (
    organization_id BINARY(16) NOT NULL PRIMARY KEY,
    organization_key VARBINARY(100) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS identity_principals (
    identity_principal_id BINARY(16) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    disabled_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS external_identities (
    external_identity_id BINARY(16) NOT NULL PRIMARY KEY,
    identity_principal_id BINARY(16) NOT NULL,
    issuer VARBINARY(480) NOT NULL,
    subject VARBINARY(255) NOT NULL,
    verified_at TIMESTAMP(6) NOT NULL,
    linked_at TIMESTAMP(6) NOT NULL,
    unlinked_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_external_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT fk_external_identity_principal FOREIGN KEY (identity_principal_id)
        REFERENCES identity_principals (identity_principal_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS organization_memberships (
    membership_id BINARY(16) NOT NULL PRIMARY KEY,
    organization_id BINARY(16) NOT NULL,
    identity_principal_id BINARY(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    generated_live_principal_id BINARY(16)
        GENERATED ALWAYS AS (CASE WHEN status = 'REVOKED' THEN NULL ELSE identity_principal_id END),
    invited_at TIMESTAMP(6),
    activated_at TIMESTAMP(6),
    revoked_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_membership_org_membership UNIQUE (organization_id, membership_id),
    CONSTRAINT uq_membership_live_principal UNIQUE (organization_id, generated_live_principal_id),
    CONSTRAINT fk_membership_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_principal FOREIGN KEY (identity_principal_id)
        REFERENCES identity_principals (identity_principal_id) ON DELETE RESTRICT
);
