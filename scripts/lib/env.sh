#!/usr/bin/env bash

load_local_env() {
  local root="${1:-}"
  if [[ -z "$root" ]]; then
    root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  fi
  if [[ -f "$root/local.env" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "$root/local.env"
    set +a
  fi
}

require_opensearch_env() {
  : "${OPENSEARCH_URI:?OPENSEARCH_URI is required (source local.env or set in environment)}"
  : "${OPENSEARCH_USERNAME:?OPENSEARCH_USERNAME is required}"
  : "${OPENSEARCH_PASSWORD:?OPENSEARCH_PASSWORD is required}"
}
