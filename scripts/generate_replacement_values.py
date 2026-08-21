#!/usr/bin/env python3
"""Generate exact untradeable replacement values from checked source rows.

The input names an authoritative OSRS Wiki page for every value. By default the
generator downloads those pages and verifies that the item name and exact GP
value still occur together, then joins the row to RuneLite's generated ItemID
source. This keeps source interpretation in a reviewable data file while
preventing hand-written Java IDs or inferred values.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import sys
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Mapping


RUNELITE_COMMIT_URL = "https://api.github.com/repos/runelite/runelite/commits/master"
RUNELITE_ITEM_ID_URL = (
    "https://raw.githubusercontent.com/runelite/runelite/{revision}/"
    "runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java"
)
USER_AGENT = "wilderness-loadouts generator (https://github.com/DenisSa/wilderness-loadouts)"
REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = REPOSITORY_ROOT / "scripts/replacement_values.json"
DEFAULT_OUTPUT = (
    REPOSITORY_ROOT
    / "src/main/java/com/denissa/wildernessloadouts/ExactReplacementValues.java"
)


def request_text(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        content = response.read()
        charset = response.headers.get_content_charset() or "utf-8"
        return content.decode(charset, errors="replace")


def resolve_runelite_revision() -> str:
    payload = json.loads(request_text(RUNELITE_COMMIT_URL))
    revision = payload.get("sha")
    if not isinstance(revision, str) or not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("RuneLite's commit API did not return a full commit SHA")
    return revision


def parse_item_constants(source: str) -> Mapping[str, str]:
    pattern = re.compile(
        r"/\*\*\s*\*\s*(?P<display>[^\r\n]+).*?\*/\s*"
        r"public static final int (?P<constant>[A-Z0-9_]+) = \d+;",
        re.S,
    )
    constants = {
        match.group("constant"): html.unescape(match.group("display")).strip()
        for match in pattern.finditer(source)
    }
    if not constants:
        raise ValueError("No RuneLite ItemID constants were parsed")
    return constants


def normalize_text(value: str) -> str:
    value = re.sub(r"<script.*?</script>|<style.*?</style>", " ", value, flags=re.I | re.S)
    value = re.sub(r"<[^>]+>", " ", html.unescape(value))
    return " ".join(value.replace("’", "'").split()).lower()


def verify_source_row(page: str, item_name: str, cost: int) -> None:
    normalized = normalize_text(page)
    normalized_name = item_name.replace("’", "'").lower()
    prices = {str(cost), f"{cost:,}"}
    positions = [match.start() for match in re.finditer(re.escape(normalized_name), normalized)]
    if not positions:
        raise ValueError(f"source page no longer contains item name: {item_name}")
    if not any(
        any(price in normalized[position:position + 1800] for price in prices)
        for position in positions
    ):
        raise ValueError(
            f"source page does not show {cost:,} GP near item name: {item_name}"
        )


def java_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def render_java(
    rows: Iterable[Mapping[str, object]],
    sources: Mapping[str, Mapping[str, str]],
    revision: str,
    input_hash: str,
) -> str:
    cases: List[str] = []
    for row in sorted(rows, key=lambda item: str(item["constant"])):
        source = sources[str(row["source"])]
        cases.extend(
            [
                f"\t\t\tcase ItemID.{row['constant']}:",
                "\t\t\t\treturn LossProfile.exact(",
                f"\t\t\t\t\t{int(row['cost']):_}L,",
                f"\t\t\t\t\t{int(row['protected_cost']):_}L,",
                f"\t\t\t\t\t{str(bool(row['protectable'])).lower()},",
                f"\t\t\t\t\tLossProfile.ReacquisitionMethod.{row['method']},",
                f"\t\t\t\t\tLossProfile.NonMonetaryBurden.{row['burden']},",
                f"\t\t\t\t\t\"{java_string(source['label'])}\");",
            ]
        )

    return f"""/*
 * Copyright (c) 2026, DenisSa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.denissa.wildernessloadouts;

import net.runelite.api.gameval.ItemID;

/**
 * Generated by scripts/generate_replacement_values.py. Do not edit manually.
 * RuneLite revision: {revision}
 * Source data SHA-256: {input_hash}
 */
final class ExactReplacementValues
{{
\tprivate ExactReplacementValues()
\t{{
\t}}

\tstatic LossProfile get(int itemId)
\t{{
\t\tswitch (itemId)
\t\t{{
{chr(10).join(cases)}
\t\t\tdefault:
\t\t\t\treturn null;
\t\t}}
\t}}
}}
"""


def generate(input_path: Path, verify_sources: bool) -> str:
    input_bytes = input_path.read_bytes()
    data = json.loads(input_bytes)
    sources: Dict[str, Mapping[str, str]] = data["sources"]
    rows: List[Mapping[str, object]] = data["items"]

    revision = resolve_runelite_revision()
    constants = parse_item_constants(
        request_text(RUNELITE_ITEM_ID_URL.format(revision=revision))
    )
    seen = set()
    pages: Dict[str, str] = {}
    for row in rows:
        constant = str(row["constant"])
        if constant in seen:
            raise ValueError(f"duplicate ItemID constant: {constant}")
        seen.add(constant)
        actual_name = constants.get(constant)
        if actual_name is None:
            raise ValueError(f"RuneLite ItemID constant does not exist: {constant}")
        if actual_name.lower() != str(row["name"]).lower():
            raise ValueError(
                f"RuneLite name mismatch for {constant}: {actual_name!r} != {row['name']!r}"
            )
        source_key = str(row["source"])
        source = sources.get(source_key)
        if source is None:
            raise ValueError(f"unknown source key for {constant}: {source_key}")
        if verify_sources:
            if source_key not in pages:
                pages[source_key] = request_text(source["url"])
            verify_source_row(
                pages[source_key],
                str(row.get("source_name", row["name"])),
                int(row["cost"]),
            )

    input_hash = hashlib.sha256(input_bytes).hexdigest()
    return render_java(rows, sources, revision, input_hash)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--skip-source-check",
        action="store_true",
        help="skip OSRS Wiki content checks but still validate current RuneLite IDs",
    )
    args = parser.parse_args()
    try:
        generated = generate(args.input, not args.skip_source_check)
    except (KeyError, OSError, TypeError, ValueError, json.JSONDecodeError) as error:
        print(f"generation failed: {error}", file=sys.stderr)
        return 1

    if args.check:
        current = args.output.read_text(encoding="utf-8") if args.output.exists() else ""
        if current != generated:
            print(f"generated output is stale: {args.output}", file=sys.stderr)
            return 1
        print(f"generated output is current: {args.output}")
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(generated, encoding="utf-8", newline="\n")
    print(f"generated {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
