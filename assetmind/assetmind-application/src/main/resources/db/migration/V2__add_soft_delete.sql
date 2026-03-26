ALTER TABLE assets ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_assets_deleted ON assets(deleted);

