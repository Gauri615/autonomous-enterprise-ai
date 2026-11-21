CREATE TABLE organizations (
    organization_id BINARY(16) NOT NULL,
    organization_key VARBINARY(100) NOT NULL,
    display_name VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_organizations PRIMARY KEY (organization_id),
    CONSTRAINT uq_organizations_key UNIQUE (organization_key),
    CONSTRAINT ck_organizations_key CHECK (
        OCTET_LENGTH(organization_key) BETWEEN 1 AND 100
        AND organization_key REGEXP '^[a-z0-9]+(-[a-z0-9]+)*$'
    ),
    CONSTRAINT ck_organizations_status CHECK (
        BINARY status IN (BINARY 'ACTIVE', BINARY 'SUSPENDED', BINARY 'CLOSED')
    )
) ENGINE=InnoDB;

CREATE TABLE identity_principals (
    identity_principal_id BINARY(16) NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    disabled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_identity_principals PRIMARY KEY (identity_principal_id),
    CONSTRAINT ck_identity_principals_status CHECK (
        BINARY status IN (BINARY 'ACTIVE', BINARY 'DISABLED')
    ),
    CONSTRAINT ck_identity_principals_disabled_at CHECK (
        (BINARY status = BINARY 'DISABLED' AND disabled_at IS NOT NULL)
        OR (BINARY status = BINARY 'ACTIVE' AND disabled_at IS NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE external_identities (
    external_identity_id BINARY(16) NOT NULL,
    identity_principal_id BINARY(16) NOT NULL,
    issuer VARBINARY(480) NOT NULL,
    subject VARBINARY(255) NOT NULL,
    verified_at DATETIME(6) NOT NULL,
    linked_at DATETIME(6) NOT NULL,
    unlinked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_external_identities PRIMARY KEY (external_identity_id),
    CONSTRAINT uq_external_identity_issuer_subject UNIQUE (issuer, subject),
    KEY ix_external_identity_principal (identity_principal_id),
    CONSTRAINT fk_external_identity_principal
        FOREIGN KEY (identity_principal_id)
        REFERENCES identity_principals (identity_principal_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_external_identity_issuer_length CHECK (
        OCTET_LENGTH(issuer) BETWEEN 1 AND 480
    ),
    CONSTRAINT ck_external_identity_subject_ascii CHECK (
        OCTET_LENGTH(subject) BETWEEN 1 AND 255
        AND subject = CONVERT(subject USING ascii)
    ),
    CONSTRAINT ck_external_identity_link_times CHECK (
        linked_at >= verified_at
        AND (unlinked_at IS NULL OR unlinked_at >= linked_at)
    )
) ENGINE=InnoDB;

CREATE TABLE organization_memberships (
    membership_id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    identity_principal_id BINARY(16) NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
    generated_live_principal_id BINARY(16)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'REVOKED' THEN NULL ELSE identity_principal_id END
        ) STORED,
    invited_at DATETIME(6),
    activated_at DATETIME(6),
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_organization_memberships PRIMARY KEY (membership_id),
    CONSTRAINT uq_membership_org_membership UNIQUE (organization_id, membership_id),
    CONSTRAINT uq_membership_live_principal UNIQUE (organization_id, generated_live_principal_id),
    KEY ix_membership_principal (identity_principal_id),
    CONSTRAINT fk_membership_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (organization_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_membership_principal
        FOREIGN KEY (identity_principal_id)
        REFERENCES identity_principals (identity_principal_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_membership_status CHECK (
        BINARY status IN (
            BINARY 'PENDING',
            BINARY 'ACTIVE',
            BINARY 'SUSPENDED',
            BINARY 'REVOKED'
        )
    ),
    CONSTRAINT ck_membership_revoked_at CHECK (
        (BINARY status = BINARY 'REVOKED' AND revoked_at IS NOT NULL)
        OR (BINARY status <> BINARY 'REVOKED' AND revoked_at IS NULL)
    )
) ENGINE=InnoDB;
