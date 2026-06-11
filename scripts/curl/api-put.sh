#!/usr/bin/env bash
set -euo pipefail

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/api.sh
source "$SCRIPTS/lib/api.sh"

ID="${1:-p-1}"

curl -s -X PUT "$(api_base)/api/products/${ID}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Large Coffee Mug","sku":"MUG-001","price":14.99}' | jq .
