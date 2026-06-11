#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOOKS_DIR="$ROOT/.githooks"

if [[ ! -d "$ROOT/.git" ]]; then
  echo "Not a git repository: $ROOT" >&2
  exit 1
fi

if [[ ! -d "$HOOKS_DIR" ]]; then
  echo "Missing hooks directory: $HOOKS_DIR" >&2
  exit 1
fi

chmod +x "$HOOKS_DIR"/*
git -C "$ROOT" config core.hooksPath .githooks
echo "Configured core.hooksPath -> .githooks"
