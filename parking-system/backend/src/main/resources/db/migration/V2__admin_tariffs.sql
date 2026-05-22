-- =============================================================================
-- V2 — Extensión del módulo Administrador para gestión de tarifas.
--
-- Agrega:
--  - tariff_plans     : planes de mensualidad (Mensual / Trimestral / etc.)
--  - special_tariffs  : multiplicadores por reglas (fin de semana, festivos, ...)
--  - holidays         : calendario editable de festivos por parking
--  - columnas extra en `zones` (under_maintenance, enabled, display_order, notes)
--
-- Política de tariffs (tabla V1): NO se sobrescriben.  Editar una tarifa genera
-- una nueva fila con `valid_from = ahora`, y la versión anterior se cierra
-- poniendo `valid_to = nuevaTarifa.valid_from - 1ms`.  Esta migración no
-- modifica la tabla tariffs — el versionado se hace desde la capa de
-- aplicación (AdminTariffRepository).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Planes de mensualidad
-- -----------------------------------------------------------------------------
CREATE TABLE tariff_plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    name            VARCHAR(80)   NOT NULL,
    duration_days   INTEGER       NOT NULL CHECK (duration_days > 0),
    price_cents     BIGINT        NOT NULL CHECK (price_cents >= 0),
    vehicle_type    VARCHAR(20)   NOT NULL,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, name, vehicle_type)
);
CREATE INDEX idx_tariff_plans_parking ON tariff_plans(parking_id, enabled);

-- -----------------------------------------------------------------------------
-- Tarifas especiales (multiplicadores)
--
-- rule_type:
--   WEEKEND      : aplica sábados y domingos del calendario civil
--   HOLIDAY      : aplica los días definidos en la tabla `holidays`
--   DATE_RANGE   : aplica entre `date_from` y `date_to` (inclusive)
--   DAY_OF_WEEK  : aplica los días de la semana listados en `day_of_week_csv`
--                  (e.g. "1,2,3,4,5" = lunes-viernes)
-- -----------------------------------------------------------------------------
CREATE TABLE special_tariffs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    name            VARCHAR(80)   NOT NULL,
    rule_type       VARCHAR(20)   NOT NULL
                    CHECK (rule_type IN ('WEEKEND', 'HOLIDAY', 'DATE_RANGE', 'DAY_OF_WEEK')),
    multiplier      NUMERIC(4,2)  NOT NULL CHECK (multiplier >= 0),
    date_from       DATE,
    date_to         DATE,
    day_of_week_csv VARCHAR(32),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, name)
);
CREATE INDEX idx_special_tariffs_parking ON special_tariffs(parking_id, enabled);

-- -----------------------------------------------------------------------------
-- Festivos editables por parking
-- -----------------------------------------------------------------------------
CREATE TABLE holidays (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parking_id      UUID NOT NULL REFERENCES parkings(id) ON DELETE CASCADE,
    date            DATE          NOT NULL,
    name            VARCHAR(120)  NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (parking_id, date)
);
CREATE INDEX idx_holidays_parking ON holidays(parking_id, date);

-- -----------------------------------------------------------------------------
-- Extensión de zonas
-- -----------------------------------------------------------------------------
ALTER TABLE zones
    ADD COLUMN under_maintenance INTEGER     NOT NULL DEFAULT 0 CHECK (under_maintenance >= 0),
    ADD COLUMN enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN display_order     INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN notes             TEXT;
