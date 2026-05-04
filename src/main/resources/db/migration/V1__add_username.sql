ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(30);

UPDATE users
SET username = LEFT(LOWER(REGEXP_REPLACE(COALESCE(name, 'user'), '\\s+', '', 'g')), 20) || '_' || id
WHERE username IS NULL;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username ON users (username);
