-- Dev Services only (%dev). Read-only login for the local MCP server; the password is for the throwaway dev container.
CREATE ROLE mcp_readonly LOGIN PASSWORD 'mcp_readonly_local';
GRANT pg_read_all_data TO mcp_readonly;
ALTER ROLE mcp_readonly SET default_transaction_read_only = on;
