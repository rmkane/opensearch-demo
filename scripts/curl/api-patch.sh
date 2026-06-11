#!/usr/bin/env bash
set -euo pipefail

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/api.sh
source "$SCRIPTS/lib/api.sh"

ID="${1:-p-1}"

curl -s -X PATCH "$(api_base)/api/products/${ID}" \
  -H 'Content-Type: application/json' \
  -d '{"price":9.99}' | jq .
