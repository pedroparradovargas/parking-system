-- =============================================================================
-- V5 — Configuración por parking (datos fiscales, horarios, conexiones).
--
-- Una sola fila por parking_id (FK + UNIQUE).  Editable solo por rol ADMIN.
-- =============================================================================

CREATE TABLE parking_config (
    parking_id              UUID PRIMARY KEY REFERENCES parkings(id) ON DELETE CASCADE,
    legal_name              VARCHAR(200),
    tax_id                  VARCHAR(40),
    legal_address           VARCHAR(300),
    city                    VARCHAR(120),
    dian_resolution         VARCHAR(120),
    dian_resolution_from    DATE,
    dian_resolution_to      DATE,
    invoice_series          VARCHAR(20),
    invoice_next_sequence   BIGINT NOT NULL DEFAULT 1,
    timezone                VARCHAR(60) NOT NULL DEFAULT 'America/Bogota',
    operating_mode          VARCHAR(20) NOT NULL DEFAULT '24x7' CHECK (operating_mode IN ('24x7', 'CUSTOM')),
    operating_schedule_json TEXT,
    ai_service_url          VARCHAR(300),
    notification_email      VARCHAR(200),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by              UUID REFERENCES users(id) ON DELETE SET NULL
);
