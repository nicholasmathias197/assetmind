ALTER TABLE users
    ADD COLUMN feature_access VARCHAR(1024);

UPDATE users
SET feature_access = ''
WHERE feature_access IS NULL;
