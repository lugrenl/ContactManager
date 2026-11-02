-- Read-only diagnostics for Migrate_contacts_to_personal_owner.sql.
-- Run as the database owner before the migration. This file changes no data.
--
-- Expected target after migration:
--   contacts(contact_id, name, surname, email, phone_number, user_id NOT NULL)
--   no user_contacts table.

-- 1. Contact links and expected row count after expanding shared contacts.
SELECT count(*) AS user_contact_links FROM user_contacts;
SELECT count(*) AS total_contacts FROM contacts;
SELECT count(DISTINCT contact_id) AS linked_contacts FROM user_contacts;

-- 2. Contacts without an owner. The migration aborts if this returns rows.
SELECT c.contact_id, c.name, c.surname, c.email, c.phone_number
  FROM contacts c
 WHERE NOT EXISTS (
     SELECT 1 FROM user_contacts uc WHERE uc.contact_id = c.contact_id
 )
 ORDER BY c.contact_id;

-- 3. Contacts shared by more than one user. These will be expanded.
SELECT contact_id, count(*) AS owner_count
  FROM user_contacts
 GROUP BY contact_id
HAVING count(*) > 1
 ORDER BY contact_id;

-- 4. Duplicate business data that would land in one user's address book.
--    The migration aborts if this returns rows.
SELECT uc.user_id,
       c.name,
       c.surname,
       c.email,
       c.phone_number,
       count(*) AS duplicate_count,
       string_agg(c.contact_id::text, ', ' ORDER BY c.contact_id) AS contact_ids
  FROM contacts c
  JOIN user_contacts uc ON uc.contact_id = c.contact_id
 GROUP BY uc.user_id, c.name, c.surname, c.email, c.phone_number
HAVING count(*) > 1
 ORDER BY uc.user_id, c.name, c.surname;

-- 5. Required fields and basic referential sanity.
SELECT count(*) AS contacts_with_null_name_or_surname
  FROM contacts
 WHERE name IS NULL OR surname IS NULL;

SELECT count(*) AS links_with_missing_contact
  FROM user_contacts uc
 WHERE NOT EXISTS (SELECT 1 FROM contacts c WHERE c.contact_id = uc.contact_id);

SELECT count(*) AS links_with_missing_user
  FROM user_contacts uc
 WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = uc.user_id);

-- 6. Actual ID types; the migration reuses these types for contacts_new.
SELECT c.relname AS table_name,
       a.attname AS column_name,
       format_type(a.atttypid, a.atttypmod) AS column_type
  FROM pg_attribute a
  JOIN pg_class c ON c.oid = a.attrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = current_schema()
   AND c.relname IN ('users', 'contacts', 'user_contacts')
   AND a.attname IN ('user_id', 'contact_id')
   AND NOT a.attisdropped
 ORDER BY c.relname, a.attname;

-- 7. Per-user expected contact counts after migration.
SELECT uc.user_id, count(*) AS expected_contacts
  FROM user_contacts uc
 GROUP BY uc.user_id
 ORDER BY uc.user_id;
