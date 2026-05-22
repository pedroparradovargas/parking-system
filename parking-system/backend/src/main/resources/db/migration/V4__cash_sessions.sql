-- =============================================================================
-- V4 — Cierres de caja por turno y operador.
--
-- Un cash_session se abre cuando el operador inicia turno y se cierra al final.
-- Los totales se calculan agregando las sesiones de parqueo cerradas durante
-- el rango [opened_at, closed_at).
-- =============================================================================

CREATE TABLE cash_sessions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id          UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    operator_user_id    UUID NOT NULL REFERENCES users(id),
    opened_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMPTZ,
    total_cash_cents    BIGINT NOT NULL DEFAULT 0,
    total_card_cents    BIGINT NOT NULL DEFAULT 0,
    total_other_cents   BIGINT NOT NULL DEFAULT 0,
    sessions_count      INTEGER NOT NULL DEFAULT 0,
    hash_chain          VARCHAR(64),
    notes               TEXT
);

CREATE INDEX idx_cash_sessions_parking ON cash_sessions(parking_id, opened_at DESC);
CREATE INDEX idx_cash_sessions_operator ON cash_sessions(operator_user_id, opened_at DESC);
