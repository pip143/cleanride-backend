ALTER TABLE reviews ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE reviews
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;
