#!/usr/bin/env python3
"""
Pack a directory tree into one text file (encode) or unpack it (decode).
Skips binary files. Encode skips paths using built-in ignore rules, optional JSON
config, and glob patterns (stdlib ``fnmatch`` per path segment; ``**`` matches
any depth).

Encode emits format v1: a prolog line ``BUNDLE_TREE_FORMAT_V1``, then for each file one
**JSON object on a single line** with keys ``v`` (1), ``path`` (relative POSIX path),
and ``bytes`` (UTF-8 payload length), a newline, then exactly that many bytes of body.
Paths with unusual characters are escaped by JSON; parsing uses the stdlib JSON decoder.

Files are ordered by **directory-first** walk (subdirectories sorted by name, recursed
before files in each directory). Empty directories are not recorded.

Decode requires that prolog and JSON records (bundles from this tool only).

Config: optional ``--config PATH`` or ``<encode-dir>/.bundle_tree.json``:

.. code-block:: json

   {
     "exclude_defaults": false,
     "ignore_path_parts": ["extra_dir"],
     "ignore_globs": ["**/*.gen.ts"]
   }

When ``exclude_defaults`` is true, only patterns from the config file apply (empty
lists if omitted). Otherwise defaults are merged with the config lists.
"""

from __future__ import annotations

import argparse
import json
import sys
from fnmatch import fnmatch
from pathlib import Path
from typing import Any

FORMAT_V1_PROLOG = "BUNDLE_TREE_FORMAT_V1\n"
FORMAT_V1_PROLOG_B = FORMAT_V1_PROLOG.encode("utf-8")

RECORD_VERSION = 1

# Any path segment equal to one of these names is skipped (any depth).
DEFAULT_IGNORE_PATH_PARTS: frozenset[str] = frozenset(
    {
        ".git",
        ".svn",
        ".hg",
        "node_modules",
        "__pycache__",
        ".venv",
        "venv",
        ".env",
        "dist",
        "build",
        "out",
        ".next",
        ".nuxt",
        ".turbo",
        "target",
        "coverage",
        ".cache",
        ".parcel-cache",
        "Pods",
        ".bundle_tree.json",  # segment name; only matches this config filename
    }
)

# Glob patterns: matched against the full relative POSIX path; ``/`` separates segments;
# ``*`` and ``?`` use fnmatch per segment; ``**`` matches zero or more segments.
DEFAULT_IGNORE_GLOBS: list[str] = [
    "**/*.pyc",
    "**/*.pyo",
    "**/*.pyd",
    "**/.DS_Store",
    "**/*.so",
    "**/*.dylib",
    "**/*.dll",
    "**/package-lock.json",
    "**/npm-shrinkwrap.json",
    "**/pnpm-lock.yaml",
]


def _split_posix_path(p: str) -> list[str]:
    p = p.replace("\\", "/").strip("/")
    if not p:
        return []
    return [seg for seg in p.split("/") if seg != ""]


def _glob_match_parts(rel_parts: list[str], pat_parts: list[str]) -> bool:
    """Match rel path parts against pattern parts (supports ** between slashes)."""

    def dfs(ri: int, pi: int) -> bool:
        if pi == len(pat_parts):
            return ri == len(rel_parts)
        if pat_parts[pi] == "**":
            for k in range(ri, len(rel_parts) + 1):
                if dfs(k, pi + 1):
                    return True
            return False
        if ri >= len(rel_parts):
            return False
        if not fnmatch(rel_parts[ri], pat_parts[pi]):
            return False
        return dfs(ri + 1, pi + 1)

    return dfs(0, 0)


def _compile_glob_patterns(patterns: list[str]) -> list[list[str]]:
    out: list[list[str]] = []
    for raw in patterns:
        s = raw.replace("\\", "/").strip()
        if not s or s.startswith("#"):
            continue
        parts = _split_posix_path(s)
        if parts:
            out.append(parts)
    return out


def load_ignore_rules(root: Path, config_path: Path | None) -> tuple[frozenset[str], list[list[str]]]:
    """Return (ignore_path_parts, compiled_glob_part_lists)."""
    path = config_path if config_path is not None else root / ".bundle_tree.json"
    data: dict[str, Any] = {}
    if path.is_file():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            print(f"Invalid JSON in {path}: {e}", file=sys.stderr)
            raise SystemExit(1) from e

    exclude_defaults = bool(data.get("exclude_defaults", False))
    extra_parts = data.get("ignore_path_parts")
    extra_globs = data.get("ignore_globs")
    if extra_parts is not None and not isinstance(extra_parts, list):
        print(f"{path}: ignore_path_parts must be a JSON array of strings.", file=sys.stderr)
        raise SystemExit(1)
    if extra_globs is not None and not isinstance(extra_globs, list):
        print(f"{path}: ignore_globs must be a JSON array of strings.", file=sys.stderr)
        raise SystemExit(1)

    parts_list: list[str] = []
    globs_list: list[str] = []
    if not exclude_defaults:
        parts_list.extend(DEFAULT_IGNORE_PATH_PARTS)
        globs_list.extend(DEFAULT_IGNORE_GLOBS)
    if isinstance(extra_parts, list):
        parts_list.extend(str(x) for x in extra_parts)
    if isinstance(extra_globs, list):
        globs_list.extend(str(x) for x in extra_globs)

    path_parts = frozenset(parts_list)
    compiled = _compile_glob_patterns(globs_list)
    return path_parts, compiled


def is_ignored(rel_posix: str, path_parts: frozenset[str], compiled_globs: list[list[str]]) -> bool:
    rel_parts = _split_posix_path(rel_posix)
    if not rel_parts:
        return False
    for seg in rel_parts:
        if seg in path_parts:
            return True
    for pat_parts in compiled_globs:
        if _glob_match_parts(rel_parts, pat_parts):
            return True
    return False


def _sorted_files_dir_first(root: Path) -> list[Path]:
    """Regular files under root: subdirs (name-sorted) fully walked before files in each directory."""
    root = root.resolve()
    out: list[Path] = []

    def walk(directory: Path) -> None:
        try:
            entries = list(directory.iterdir())
        except OSError:
            return
        entries.sort(key=lambda p: p.name)
        subdirs = [p for p in entries if p.is_dir() and not p.is_symlink()]
        files_here = [p for p in entries if p.is_file() and not p.is_symlink()]
        for d in subdirs:
            walk(d)
        out.extend(files_here)

    walk(root)
    return out


def is_probably_text_file(path: Path, max_read: int = 65536) -> bool:
    try:
        data = path.read_bytes()[:max_read]
    except OSError:
        return False
    if b"\x00" in data:
        return False
    try:
        data.decode("utf-8")
        return True
    except UnicodeDecodeError:
        return False


def encode_tree(
    root: Path,
    out_path: Path,
    *,
    path_parts: frozenset[str],
    compiled_globs: list[list[str]],
    skip_paths: set[str],
) -> None:
    root = root.resolve()
    parts: list[bytes] = [FORMAT_V1_PROLOG_B]
    file_count = 0

    for path in _sorted_files_dir_first(root):
        try:
            rel = path.relative_to(root)
        except ValueError:
            continue
        rel_posix = rel.as_posix()
        if rel_posix in skip_paths:
            continue
        if is_ignored(rel_posix, path_parts, compiled_globs):
            continue
        if not is_probably_text_file(path):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        body = text.encode("utf-8")
        record = {
            "v": RECORD_VERSION,
            "path": rel_posix,
            "bytes": len(body),
        }
        line = json.dumps(record, ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n"
        parts.append(line.encode("utf-8"))
        parts.append(body)
        file_count += 1

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(b"".join(parts))
    print(f"Wrote {file_count} files to {out_path}", file=sys.stderr)


def _decode_bundle_v1(data: bytes, out_root: Path) -> int:
    pos = 0
    if not data.startswith(FORMAT_V1_PROLOG_B):
        return 0
    pos = len(FORMAT_V1_PROLOG_B)
    count = 0
    while pos < len(data):
        nl = data.find(b"\n", pos)
        if nl == -1:
            print(f"Malformed bundle at byte offset {pos}: missing newline after JSON record.", file=sys.stderr)
            raise SystemExit(1)
        line_b = data[pos:nl]
        pos = nl + 1
        try:
            line = line_b.decode("utf-8")
            meta = json.loads(line)
        except UnicodeDecodeError as e:
            print(f"Malformed bundle: JSON record line is not UTF-8 ({e}).", file=sys.stderr)
            raise SystemExit(1) from e
        except json.JSONDecodeError as e:
            print(f"Malformed bundle at record {count + 1}: invalid JSON ({e}).", file=sys.stderr)
            raise SystemExit(1) from e
        if not isinstance(meta, dict):
            print(f"Malformed bundle: record must be a JSON object, got {type(meta).__name__}.", file=sys.stderr)
            raise SystemExit(1)
        if meta.get("v") != RECORD_VERSION:
            print(f"Unsupported record version: {meta.get('v')!r} (expected {RECORD_VERSION}).", file=sys.stderr)
            raise SystemExit(1)
        rel = meta.get("path")
        body_len = meta.get("bytes")
        if not isinstance(rel, str) or not rel.strip():
            print(f"Malformed bundle: invalid path in record: {rel!r}.", file=sys.stderr)
            raise SystemExit(1)
        rel = rel.replace("\\", "/").strip()
        if not isinstance(body_len, int) or body_len < 0:
            print(f"Malformed bundle: invalid byte length in record: {body_len!r}.", file=sys.stderr)
            raise SystemExit(1)
        body_end = pos + body_len
        if body_end > len(data):
            print("Malformed bundle: truncated payload.", file=sys.stderr)
            raise SystemExit(1)
        raw = data[pos:body_end]
        pos = body_end
        if rel.startswith("/") or ".." in Path(rel).parts:
            print(f"Skipping unsafe path: {rel!r}", file=sys.stderr)
            continue
        try:
            content = raw.decode("utf-8")
        except UnicodeDecodeError as e:
            print(f"Skipping {rel!r}: invalid UTF-8 in payload ({e}).", file=sys.stderr)
            continue
        target = out_root / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        print(rel, file=sys.stderr)
        count += 1
    if pos < len(data):
        print(f"Warning: {len(data) - pos} trailing bytes after last payload (ignored).", file=sys.stderr)
    return count


def decode_bundle(bundle_path: Path, out_root: Path) -> None:
    data = bundle_path.read_bytes()
    if data.startswith(b"\xef\xbb\xbf"):
        data = data[3:]

    out_root = out_root.resolve()
    out_root.mkdir(parents=True, exist_ok=True)

    if not data.startswith(FORMAT_V1_PROLOG_B):
        print(
            "Not a bundle_tree v1 file: expected prolog line BUNDLE_TREE_FORMAT_V1.",
            file=sys.stderr,
        )
        raise SystemExit(1)
    n = _decode_bundle_v1(data, out_root)
    if n == 0:
        print("No FILE sections found in bundle.", file=sys.stderr)
        raise SystemExit(1)
    print(f"Restored {n} files under {out_root}", file=sys.stderr)


def cmd_encode(args: argparse.Namespace) -> None:
    root = Path(args.directory).resolve()
    if not root.is_dir():
        print(f"Not a directory: {root}", file=sys.stderr)
        raise SystemExit(1)
    out_path = Path(args.output).resolve()
    if args.config:
        config_path = Path(args.config).resolve()
        if not config_path.is_file():
            print(f"Config not found: {config_path}", file=sys.stderr)
            raise SystemExit(1)
    else:
        config_path = None
    path_parts, compiled_globs = load_ignore_rules(root, config_path)
    skip: set[str] = set()
    try:
        out_rel = out_path.relative_to(root).as_posix()
        skip.add(out_rel)
    except ValueError:
        pass
    encode_tree(
        root,
        out_path,
        path_parts=path_parts,
        compiled_globs=compiled_globs,
        skip_paths=skip,
    )


def cmd_decode(args: argparse.Namespace) -> None:
    bundle = Path(args.bundle).resolve()
    if not bundle.is_file():
        print(f"Not a file: {bundle}", file=sys.stderr)
        raise SystemExit(1)
    out_root = Path(args.output).resolve()
    decode_bundle(bundle, out_root)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Pack/unpack a text-only directory tree using glob-based ignore rules and JSON-length-prefixed records.",
    )
    sub = p.add_subparsers(dest="command", required=True)

    pe = sub.add_parser("encode", help="Walk a directory and write one bundle file.")
    pe.add_argument(
        "directory",
        type=str,
        help="Root directory to traverse.",
    )
    pe.add_argument(
        "-o",
        "--output",
        required=True,
        help="Output bundle file path.",
    )
    pe.add_argument(
        "--config",
        type=str,
        default=None,
        metavar="PATH",
        help="JSON config (defaults: merge with built-in ignores). If omitted, use DIR/.bundle_tree.json when present.",
    )
    pe.set_defaults(func=cmd_encode)

    pd = sub.add_parser("decode", help="Read a bundle and recreate files under a directory.")
    pd.add_argument("bundle", type=str, help="Bundle file produced by encode.")
    pd.add_argument(
        "-o",
        "--output",
        required=True,
        help="Output directory (created if needed).",
    )
    pd.set_defaults(func=cmd_decode)

    ph = sub.add_parser("help", help="Show this help and exit.")

    def cmd_help(_: argparse.Namespace) -> None:
        p.print_help()

    ph.set_defaults(func=cmd_help)

    return p


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
