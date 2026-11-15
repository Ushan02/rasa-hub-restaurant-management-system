CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(250) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_account_id BIGINT,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    occurred_at TIMESTAMP NOT NULL,
    description VARCHAR(500),
    business_reference VARCHAR(120),
    location_code VARCHAR(5),
    CONSTRAINT uk_audit_logs_event_key UNIQUE (event_key)
);

CREATE INDEX idx_audit_entity
    ON audit_logs (entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_actor
    ON audit_logs (actor_account_id, occurred_at DESC);
CREATE INDEX idx_audit_location
    ON audit_logs (location_code, occurred_at DESC);
CREATE INDEX idx_audit_action
    ON audit_logs (action, occurred_at DESC);
