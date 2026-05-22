-- ============================================================================
-- V1__initial_schema.sql
-- Esquema inicial de PostgreSQL para parking-system.
-- Diseñado para multi-tenant con `parking_id UUID` en TODAS las tablas
-- de negocio.  Aplicado automáticamente por Flyway al arrancar el backend.
-- ============================================================================

-- Extensiones requeridas.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- TimescaleDB es opcional: si no está disponible, las CREATE_HYPERTABLE fallan
-- silenciosamente en producción cuando se desactiva.  Aquí lo dejamos opt-in.

-- ----------------------------------------------------------------------------
-- Núcleo: parkings (raíz multi-tenant)
-- ----------------------------------------------------------------------------
CREATE TABLE parkings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(32)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    tax_id          VARCHAR(64),
    address         VARCHAR(500),
    city            VARCHAR(100),
    country         VARCHAR(2) DEFAULT 'CO',
    timezone        VARCHAR(64) DEFAULT 'America/Bogota',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Seguridad: users, roles, permissions
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code        VARCHAR(80) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE role_permissions (
    role_id        UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    username        VARCHAR(80)  NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,    -- bcrypt cost 12
    totp_secret     VARCHAR(128),             -- base32, NULL si no usa 2FA
    requires_2fa    BOOLEAN NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, username),
    UNIQUE (parking_id, email)
);
CREATE INDEX idx_users_parking ON users(parking_id);

CREATE TABLE user_roles (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id  UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);

-- ----------------------------------------------------------------------------
-- Tarifas
-- ----------------------------------------------------------------------------
CREATE TABLE tariffs (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id              UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    vehicle_type            VARCHAR(20) NOT NULL,
    first_hour_cents        BIGINT NOT NULL,
    subsequent_hour_cents   BIGINT NOT NULL,
    night_surcharge_percent INTEGER NOT NULL DEFAULT 0,
    night_from              TIME    NOT NULL DEFAULT '22:00',
    night_to                TIME    NOT NULL DEFAULT '06:00',
    grace_minutes           INTEGER NOT NULL DEFAULT 15,
    iva_percent             INTEGER NOT NULL DEFAULT 19,
    valid_from              TIMESTAMPTZ NOT NULL,
    valid_to                TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tariffs_parking ON tariffs(parking_id, vehicle_type, valid_from DESC);

-- ----------------------------------------------------------------------------
-- Zonas, vehículos, clientes, mensualidades
-- ----------------------------------------------------------------------------
CREATE TABLE zones (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id          UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    code                VARCHAR(16) NOT NULL,
    capacity            INTEGER NOT NULL,
    current_occupancy   INTEGER NOT NULL DEFAULT 0,
    allowed_vehicle_types VARCHAR(200) NOT NULL DEFAULT 'CAR,MOTORCYCLE',
    UNIQUE (parking_id, code)
);
CREATE INDEX idx_zones_parking ON zones(parking_id);

CREATE TABLE vehicles (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id          UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    plate               VARCHAR(16) NOT NULL,
    type                VARCHAR(20) NOT NULL,
    owner_name          VARCHAR(255),
    owner_document      VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, plate)
);
CREATE INDEX idx_vehicles_plate ON vehicles(parking_id, plate);

CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    full_name       VARCHAR(255) NOT NULL,
    document_number VARCHAR(64)  NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(40),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, document_number)
);

CREATE TABLE monthly_payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    customer_id     UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    amount_cents    BIGINT NOT NULL,
    valid_from      TIMESTAMPTZ NOT NULL,
    valid_to        TIMESTAMPTZ NOT NULL,
    paid_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_monthly_customer ON monthly_payments(parking_id, customer_id, valid_to);

-- ----------------------------------------------------------------------------
-- Sesiones de parqueo (particionadas por fecha)
-- ----------------------------------------------------------------------------
CREATE TABLE parking_sessions (
    id                  UUID NOT NULL DEFAULT uuid_generate_v4(),
    parking_id          UUID NOT NULL,
    local_id            UUID NOT NULL,
    plate               VARCHAR(16) NOT NULL,
    vehicle_type        VARCHAR(20) NOT NULL,
    zone_id             UUID,
    customer_id         UUID,
    operator_user_id    UUID,
    entry_gate          VARCHAR(32),
    entry_at            TIMESTAMPTZ NOT NULL,
    exit_at             TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_cents         BIGINT,
    iva_cents           BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, entry_at)
) PARTITION BY RANGE (entry_at);

CREATE INDEX idx_sessions_parking_status ON parking_sessions(parking_id, status, entry_at);
-- PostgreSQL exige que un UNIQUE sobre tabla particionada incluya la columna
-- de partición.  Esto NO debilita la garantía de idempotencia: el cliente
-- envía siempre el mismo `entry_at` para un `local_id` dado.
CREATE UNIQUE INDEX idx_sessions_local_id ON parking_sessions(parking_id, local_id, entry_at);

-- Particiones por mes para 2025 — las nuevas se crean con un job cron.
CREATE TABLE parking_sessions_2025_01 PARTITION OF parking_sessions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE parking_sessions_2025_02 PARTITION OF parking_sessions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE parking_sessions_2025_03 PARTITION OF parking_sessions
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE parking_sessions_default PARTITION OF parking_sessions DEFAULT;

-- ----------------------------------------------------------------------------
-- Recibos
-- ----------------------------------------------------------------------------
CREATE TABLE receipts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    session_id      UUID NOT NULL,
    local_id        UUID NOT NULL,
    sequence_local  VARCHAR(32) NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL,
    subtotal_cents  BIGINT NOT NULL,
    iva_cents       BIGINT NOT NULL,
    total_cents     BIGINT NOT NULL,
    cufe            VARCHAR(255),
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, local_id)
);
CREATE INDEX idx_receipts_parking_date ON receipts(parking_id, issued_at DESC);

-- ----------------------------------------------------------------------------
-- Auditoría inmutable con hash chain (también particionada)
-- ----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id              UUID NOT NULL DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL,
    ts              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    action          VARCHAR(80)  NOT NULL,
    entity          VARCHAR(80)  NOT NULL,
    entity_id       VARCHAR(80)  NOT NULL,
    actor_user_id   UUID,
    payload         JSONB        NOT NULL,
    prev_hash       VARCHAR(64),
    current_hash    VARCHAR(64) NOT NULL,
    PRIMARY KEY (id, ts)
) PARTITION BY RANGE (ts);
CREATE INDEX idx_audit_ts ON audit_log(parking_id, ts DESC);
CREATE TABLE audit_log_default PARTITION OF audit_log DEFAULT;

-- ----------------------------------------------------------------------------
-- Sync log: rastreo de qué se recibió de qué dispositivo y cuándo.
-- ----------------------------------------------------------------------------
CREATE TABLE sync_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    device_id       VARCHAR(80) NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload_size    INTEGER NOT NULL,
    accepted_count  INTEGER NOT NULL,
    conflict_count  INTEGER NOT NULL,
    rejected_count  INTEGER NOT NULL
);
CREATE INDEX idx_sync_device ON sync_log(parking_id, device_id, received_at DESC);

-- ----------------------------------------------------------------------------
-- Seeds básicos (roles, parking demo, usuario admin con bcrypt de "admin123").
-- En producción este admin debe ser eliminado tras el primer arranque.
-- bcrypt cost 12 para "admin123": (se reemplaza por el primer flyway run con seed dinámico)
-- ----------------------------------------------------------------------------
INSERT INTO roles(name, description) VALUES
    ('SUPERADMIN', 'Acceso total al sistema'),
    ('ADMIN', 'Administrador de un parking'),
    ('CASHIER', 'Operador de caja'),
    ('VIEWER', 'Sólo lectura / reportes')
ON CONFLICT DO NOTHING;

INSERT INTO permissions(code, description) VALUES
    ('parking.read', 'Ver datos del parking'),
    ('parking.write', 'Modificar datos del parking'),
    ('session.open', 'Abrir sesiones'),
    ('session.close', 'Cerrar sesiones / cobrar'),
    ('tariff.write', 'Editar tarifas'),
    ('user.manage', 'Gestionar usuarios'),
    ('report.read', 'Ver reportes')
ON CONFLICT DO NOTHING;
