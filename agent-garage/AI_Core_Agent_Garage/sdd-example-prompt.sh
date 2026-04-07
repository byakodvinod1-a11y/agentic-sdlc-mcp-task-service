TEMP_FILE="/tmp/example.json"

cat > "${TEMP_FILE}" << EOF
{
  "data": "Build a Minimal Model Context Protocol (MCP) Server
\n
🎯 Objective
\n
Develop a MCP-compatible service in Node.js that allows an AI agent
to interact with a Task Management database.
\n
🧩 Architecture
\n
[ AI Agent ] ⇅ [ Custom MCP Server (Node.js) ] ⇅ [ PostgreSQL DB ]
\n
📦 Project Scope
\n
Technologies: Node.js, PostgreSQL, NestJS + Prisma
\n
🔧 Functional Requirements
\n
- GET /mcp/schema/tasks - Returns database schema as JSON-Schema
- POST /mcp/tasks - Accepts JSON array of Task objects
- GET /mcp/tasks/summary - Returns task statistics
\n
✅ Success Criteria
\n
1. MCP Server running and accessible
2. Schema inspection works
3. AI agent can insert 1000 task records
4. Summary reflects inserted data"
}
EOF

curl \
  http://localhost:5678/webhook/assistant_on_creating_specification_for_sdd \
  --request POST \
  --header "Content-Type: application/json" \
  --data "@${TEMP_FILE}"

rm "${TEMP_FILE}"
