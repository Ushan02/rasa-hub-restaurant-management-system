SELECT version() AS postgres_version;

SELECT current_database() AS database_name,
       current_user AS connected_role,
       current_schema() AS current_schema,
       current_schemas(false) AS active_schemas,
       current_setting('search_path') AS search_path;
