CREATE TABLE assets (
    id VARCHAR(64) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    asset_class VARCHAR(64) NOT NULL,
    cost_basis DECIMAL(19,2) NOT NULL,
    in_service_date DATE NOT NULL,
    useful_life_years INT NOT NULL
);

CREATE INDEX idx_assets_asset_class ON assets(asset_class);

