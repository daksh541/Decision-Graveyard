#!/usr/bin/env python3
"""
Aggregate core project files into one clean text reference file.

Usage:
  python scripts/export_core_files.py
  python scripts/export_core_files.py --output exports/project_reference.txt
  python scripts/export_core_files.py --max-bytes 400000
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Iterable, List


DEFAULT_INCLUDE_PATTERNS = [
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "firestore.rules",
    "DOCUMENTATION.md",
    "PROJECT_REFERENCE.md",
    "TEST_CASES.md",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/sai/decisiongraveyard/**/*.java",
    "app/src/main/res/layout/*.xml",
    "app/src/main/res/menu/*.xml",
    "app/src/main/res/values/*.xml",
    "app/src/test/java/com/sai/decisiongraveyard/**/*.java",
]

DEFAULT_EXCLUDE_PATTERNS = [
    "**/build/**",
    "**/.gradle/**",
    "**/.idea/**",
    "**/local.properties",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export core project files into a single text file."
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Project root directory (default: repository root).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("exports/core_project_bundle.txt"),
        help="Output file path (relative to root unless absolute).",
    )
    parser.add_argument(
        "--max-bytes",
        type=int,
        default=800_000,
        help="Skip files larger than this many bytes (default: 800000).",
    )
    return parser.parse_args()


def unique_sorted(paths: Iterable[Path]) -> List[Path]:
    return sorted(set(paths), key=lambda p: str(p).lower())


def collect_files(root: Path, max_bytes: int) -> List[Path]:
    candidates: List[Path] = []
    for pattern in DEFAULT_INCLUDE_PATTERNS:
        candidates.extend(root.glob(pattern))

    files = []
    for path in unique_sorted(candidates):
        if not path.is_file():
            continue
        rel = path.relative_to(root)
        if any(rel.match(ex) for ex in DEFAULT_EXCLUDE_PATTERNS):
            continue
        try:
            if path.stat().st_size > max_bytes:
                continue
        except OSError:
            continue
        files.append(path)
    return files


def render_file_block(root: Path, path: Path) -> str:
    rel = path.relative_to(root)
    try:
        content = path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        content = f"[Error reading file: {exc}]"

    divider = "=" * 100
    return (
        f"{divider}\n"
        f"FILE: {rel.as_posix()}\n"
        f"{divider}\n\n"
        f"{content.rstrip()}\n\n"
    )


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else (root / args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    files = collect_files(root, args.max_bytes)
    header = (
        "DecisionGraveyard Core File Bundle\n"
        f"Root: {root}\n"
        f"Total files: {len(files)}\n\n"
    )

    with output.open("w", encoding="utf-8") as out:
        out.write(header)
        for path in files:
            out.write(render_file_block(root, path))

    print(f"Wrote {len(files)} files to {output}")


if __name__ == "__main__":
    main()
