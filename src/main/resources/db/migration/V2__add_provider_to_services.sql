-- Add provider_id column to services table
ALTER TABLE services ADD COLUMN IF NOT EXISTS provider_id BIGINT;

-- Add foreign key constraint
ALTER TABLE services ADD CONSTRAINT fk_services_provider_id 
FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_services_provider_id ON services(provider_id);
