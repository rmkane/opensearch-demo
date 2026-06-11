#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=scripts/lib/env.sh
source "$SCRIPTS/lib/env.sh"

load_local_env "$ROOT"
require_opensearch_env

curl -k -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" \
  "$OPENSEARCH_URI/_cluster/health?pretty"
