-- Create application user with limited privileges
CREATE USER IF NOT EXISTS '${DB_APP_USER}' @'%' IDENTIFIED BY '${DB_APP_PASSWORD}';

GRANT SELECT, INSERT, UPDATE, DELETE ON ${DB_NAME}.* TO '${DB_APP_USER}'@'%';

-- Create read-only user
CREATE USER IF NOT EXISTS '${DB_READ_USER}' @'%' IDENTIFIED BY '${DB_READ_PASSWORD}';

GRANT SELECT ON ${DB_NAME}.* TO '${DB_READ_USER}'@'%';

-- Create guest user with minimal privileges
CREATE USER IF NOT EXISTS '${DB_GUEST_USER}' @'%' IDENTIFIED BY '${DB_GUEST_PASSWORD}';

GRANT SELECT ON ${DB_NAME}.public_* TO '${DB_GUEST_USER}'@'%';

-- Flush privileges to apply changes
FLUSH PRIVILEGES;

