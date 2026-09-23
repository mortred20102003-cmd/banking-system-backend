#!/usr/bin/env bash
set -e
set +H                                  # disable history expansion so ! is literal
cd "$(dirname "$0")"

# Load .env safely — handles &, ?, =, ! in values
while IFS='=' read -r key value; do
  [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
  # strip CR if file has Windows line endings
  value="${value%$'\r'}"
  export "$key=$value"
done < .env

echo "▶ Starting Banking_System"
echo "  DB_URL       = $DB_URL"
echo "  DB_USERNAME  = $DB_USERNAME"
echo "  DB_PASSWORD  = ${DB_PASSWORD:0:6}…(len ${#DB_PASSWORD})"
echo "  SERVER_PORT  = $SERVER_PORT"
echo ""
exec mvn -q spring-boot:run
