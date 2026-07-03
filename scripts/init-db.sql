-- Extensions needed by Flyway migrations (uuid_generate_v4, gin_trgm_ops).
-- Installed once in the default "public" schema; every service connects
-- with currentSchema=<service_schema>,public on its JDBC URL so these
-- functions/operators resolve via the search_path fallback to public.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Each microservice owns its own schema (= its own Flyway history table),
-- so 5 independent "V1__..." migrations can coexist in one physical database
-- without colliding in flyway_schema_history.
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS event_schema;
CREATE SCHEMA IF NOT EXISTS booking_schema;
CREATE SCHEMA IF NOT EXISTS ticket_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;

GRANT ALL PRIVILEGES ON SCHEMA user_schema         TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA event_schema        TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA booking_schema       TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA ticket_schema        TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA notification_schema  TO appuser;
