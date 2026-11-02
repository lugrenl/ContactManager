-- Manual schema change for existing PostgreSQL databases.
-- The application uses hibernate.hbm2ddl.auto=validate and does not create constraints.
--
-- Before applying, make sure there are no duplicate user names:
--   SELECT name, COUNT(*) FROM users GROUP BY name HAVING COUNT(*) > 1;
-- Resolve any duplicates manually; this script does not delete or merge data.
--
-- Apply as a database owner:
--   psql -h <host> -U <user> -d <database> -f Add_unique_users_name.sql

ALTER TABLE users ADD CONSTRAINT uk_users_name UNIQUE (name);
