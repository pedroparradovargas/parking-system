-- =============================================================================
-- V3 — Extensiones para el módulo Admin > Clientes:
--   - Pivot `customer_vehicles` (cliente ↔ placas).
--   - Columnas en `monthly_payments` para referenciar el `tariff_plan` aplicado
--     y el medio de pago.
-- =============================================================================

-- Asocia el pago a un plan de la tabla tariff_plans (V2) y registra el método.
ALTER TABLE monthly_payments
    ADD COLUMN plan_id        UUID REFERENCES tariff_plans(id) ON DELETE SET NULL,
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH';
CREATE INDEX idx_monthly_plan ON monthly_payments(plan_id);


CREATE TABLE customer_vehicles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plate           VARCHAR(16) NOT NULL,
    vehicle_type    VARCHAR(20) NOT NULL,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, plate)
);
CREATE INDEX idx_customer_vehicles_plate ON customer_vehicles(plate);
CREATE INDEX idx_customer_vehicles_customer ON customer_vehicles(customer_id);
