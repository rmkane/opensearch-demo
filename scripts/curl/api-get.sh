#!/usr/bin/env bash
set -euo pipefail

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/api.sh
source "$SCRIPTS/lib/api.sh"

ID="${1:-p-1}"

curl -s "$(api_base)/api/products/${ID}" | jq .
