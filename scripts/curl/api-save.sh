#!/usr/bin/env bash
set -euo pipefail

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/api.sh
source "$SCRIPTS/lib/api.sh"

curl -s -X POST "$(api_base)/api/products" \
  -H 'Content-Type: application/json' \
  -d '{"id":"p-1","name":"Coffee Mug","sku":"MUG-001","price":12.99}' | jq .
