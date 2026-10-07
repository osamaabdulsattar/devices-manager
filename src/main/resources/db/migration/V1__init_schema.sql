CREATE TABLE IF NOT EXISTS brands (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    brand_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devices_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT chk_devices_state CHECK (state IN ('AVAILABLE', 'IN_USE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_devices_brand_id ON devices (brand_id);
CREATE INDEX IF NOT EXISTS idx_devices_state ON devices (state);
