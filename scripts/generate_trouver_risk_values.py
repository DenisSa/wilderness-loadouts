#!/usr/bin/env python3
"""Generate the static risk table for Trouver-locked items.

The generator deliberately joins source data instead of deriving GP values from
item names:

* Jagex's Trouver rework defines the low- and high-tier item families.
* RuneLite's generated gameval ItemID source supplies exact locked item IDs.

Only a few aliases below bridge differing source names. They never assign a GP
value or add an item family that is absent from the source data.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, Set, Tuple


JAGEX_TROUVER_URL = (
    "https://secure.runescape.com/m=news/"
    "bank-tags-trouver-system-rework--more?oldschool=1"
)
RUNELITE_COMMIT_URL = "https://api.github.com/repos/runelite/runelite/commits/master"
RUNELITE_ITEM_ID_URL = (
    "https://raw.githubusercontent.com/runelite/runelite/{revision}/"
    "runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java"
)
USER_AGENT = "wilderness-loadouts generator (https://github.com/DenisSa/wilderness-loadouts)"
DEEP_WILDERNESS_REPAIR_COST = 500_000

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = (
    REPOSITORY_ROOT
    / "src/main/java/com/denissa/wildernessloadouts/TrouverRiskValues.java"
)


@dataclass(frozen=True)
class GamevalItem:
    constant: str
    item_id: int
    display_name: str


def request_text(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        content = response.read()
        charset = response.headers.get_content_charset() or "utf-8"
        try:
            return content.decode(charset)
        except UnicodeDecodeError:
            return content.decode("windows-1252")


def resolve_runelite_revision() -> str:
    payload = json.loads(request_text(RUNELITE_COMMIT_URL))
    revision = payload.get("sha")
    if not isinstance(revision, str) or not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("RuneLite's commit API did not return a full commit SHA")
    return revision


def strip_markup(value: str) -> str:
    return re.sub(r"<[^>]+>", "", html.unescape(value)).strip()


def normalize_name(value: str) -> str:
    normalized = strip_markup(value).replace("’", "'").replace(" ", " ")
    normalized = re.sub(r"\s*\((?:l|or|t|uncharged)\)", "", normalized, flags=re.I)
    return " ".join(normalized.lower().split())


def extract_jagex_list(page: str, marker: str) -> Set[str]:
    marker_position = page.find(marker)
    if marker_position < 0:
        raise ValueError(f"Could not locate Jagex category marker: {marker}")
    details_start = page.find("<details", marker_position)
    details_end = page.find("</details>", details_start)
    if details_start < 0 or details_end < 0:
        raise ValueError(f"Could not locate Jagex category list after: {marker}")
    names = {
        normalize_name(match)
        for match in re.findall(r"<li>(.*?)</li>", page[details_start:details_end], re.S)
    }
    if not names:
        raise ValueError(f"Jagex category list was empty after: {marker}")
    return names


def parse_gameval_items(source: str) -> Mapping[str, GamevalItem]:
    pattern = re.compile(
        r"^\t/\*\*\r?\n"
        r"\t \* (?P<display>[^\r\n]+)\r?\n"
        r"\t \*/\r?\n"
        r"\tpublic static final int (?P<constant>[A-Z0-9_]+) = (?P<id>\d+);$",
        re.M,
    )
    items: Dict[str, GamevalItem] = {}
    for match in pattern.finditer(source):
        constant = match.group("constant")
        items[constant] = GamevalItem(
            constant=constant,
            item_id=int(match.group("id")),
            display_name=match.group("display"),
        )
    if not items:
        raise ValueError("No top-level RuneLite ItemID constants were parsed")
    return items


def source_name(item: GamevalItem) -> str:
    constant = item.constant
    if constant.startswith("CASTLEWARS_MAGE_"):
        return normalize_name("Decorative magic armour")
    if constant.startswith("CASTLEWARS_RANGE_"):
        return normalize_name("Decorative ranged armour")
    if constant.startswith("CASTLEWARS_") and "_HALO_" not in constant:
        return normalize_name("Decorative armour (gold)")
    if constant.startswith("INFERNAL_DEFENDER_GHOMMAL_"):
        return normalize_name("Avernic defender")
    return normalize_name(item.display_name)


def build_values(
    items: Mapping[str, GamevalItem],
    low_tier_names: Set[str],
    high_tier_names: Set[str],
) -> Tuple[
    Mapping[int, Tuple[str, int]],
    Mapping[int, str],
    Mapping[int, str],
    Mapping[int, str],
]:
    locked_items = [
        item
        for item in items.values()
        if item.constant.endswith("_TROUVER")
    ]
    mangled_normal_constants = {
        constant.removesuffix("_MANGLED")
        for constant in items
        if constant.endswith("_TROUVER_MANGLED")
    }

    values: Dict[int, Tuple[str, int]] = {}
    excluded_legacy: Dict[int, str] = {}
    unlocked_high: Dict[int, str] = {}
    unlocked_legacy: Dict[int, str] = {}
    covered_low: Set[str] = set()
    covered_high: Set[str] = set()
    for item in locked_items:
        name = source_name(item)
        is_cache_mangled = item.constant in mangled_normal_constants
        if name in high_tier_names or is_cache_mangled:
            values[item.item_id] = (item.constant, DEEP_WILDERNESS_REPAIR_COST)
            add_unlocked_variant(items, item, unlocked_high)
            if name in high_tier_names:
                covered_high.add(name)
            continue

        if name not in low_tier_names:
            continue
        excluded_legacy[item.item_id] = item.constant
        add_unlocked_variant(items, item, unlocked_legacy)
        covered_low.add(name)

    missing_low = sorted(low_tier_names - covered_low)
    missing_high = sorted(high_tier_names - covered_high)
    if missing_low or missing_high:
        messages = []
        if missing_low:
            messages.append("unmatched low-tier names: " + ", ".join(missing_low))
        if missing_high:
            messages.append("unmatched high-tier names: " + ", ".join(missing_high))
        raise ValueError("; ".join(messages))
    if not values:
        raise ValueError("No Trouver risk values were generated")
    if not excluded_legacy:
        raise ValueError("No legacy low-tier Trouver IDs were generated")
    if not unlocked_high:
        raise ValueError("No unlocked high-tier Trouver IDs were generated")
    if not unlocked_legacy:
        raise ValueError("No unlocked legacy low-tier Trouver IDs were generated")
    return values, excluded_legacy, unlocked_high, unlocked_legacy


def add_unlocked_variant(
    items: Mapping[str, GamevalItem],
    locked_item: GamevalItem,
    unlocked_capable: Dict[int, str],
) -> None:
    unlocked_constant = locked_item.constant.removesuffix("_TROUVER")
    unlocked_item = items.get(unlocked_constant)
    if unlocked_item is None:
        raise ValueError(
            f"Could not find unlocked variant {unlocked_constant} for "
            f"{locked_item.constant}"
        )
    unlocked_capable[unlocked_item.item_id] = unlocked_item.constant


def grouped_constants(values: Mapping[int, Tuple[str, int]]) -> Iterable[Tuple[int, List[str]]]:
    groups: Dict[int, List[str]] = {}
    for constant, cost in values.values():
        groups.setdefault(cost, []).append(constant)
    for cost in sorted(groups):
        yield cost, sorted(groups[cost])


def render_java(
    values: Mapping[int, Tuple[str, int]],
    excluded_legacy: Mapping[int, str],
    unlocked_high: Mapping[int, str],
    unlocked_legacy: Mapping[int, str],
    runelite_revision: str,
    jagex_hash: str,
) -> str:
    cases: List[str] = []
    for cost, constants in grouped_constants(values):
        for constant in constants:
            cases.append(f"\t\t\tcase ItemID.{constant}:")
        cases.append(f"\t\t\t\treturn {cost:_}L;")

    legacy_cases = [
        f"\t\t\tcase ItemID.{constant}:"
        for constant in sorted(excluded_legacy.values())
    ]
    legacy_cases.append("\t\t\t\treturn true;")

    unlocked_high_cases = [
        f"\t\t\tcase ItemID.{constant}:"
        for constant in sorted(unlocked_high.values())
    ]
    unlocked_high_cases.append("\t\t\t\treturn true;")

    unlocked_legacy_cases = [
        f"\t\t\tcase ItemID.{constant}:"
        for constant in sorted(unlocked_legacy.values())
    ]
    unlocked_legacy_cases.append("\t\t\t\treturn true;")

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
 * Generated by scripts/generate_trouver_risk_values.py. Do not edit manually.
 * RuneLite revision: {runelite_revision}
 * Jagex Trouver categories SHA-256: {jagex_hash}
 */
final class TrouverRiskValues
{{
\tprivate TrouverRiskValues()
\t{{
\t}}

\tstatic long getRepairCost(int itemId)
\t{{
\t\tswitch (itemId)
\t\t{{
{chr(10).join(cases)}
\t\t\tdefault:
\t\t\t\treturn 0L;
\t\t}}
\t}}

\tstatic boolean isLegacyLowTier(int itemId)
\t{{
\t\tswitch (itemId)
\t\t{{
{chr(10).join(legacy_cases)}
\t\t\tdefault:
\t\t\t\treturn false;
\t\t}}
\t}}

\tstatic boolean isUnlockedHighTier(int itemId)
\t{{
\t\tswitch (itemId)
\t\t{{
{chr(10).join(unlocked_high_cases)}
\t\t\tdefault:
\t\t\t\treturn false;
\t\t}}
\t}}

\tstatic boolean isUnlockedLegacyLowTier(int itemId)
\t{{
\t\tswitch (itemId)
\t\t{{
{chr(10).join(unlocked_legacy_cases)}
\t\t\tdefault:
\t\t\t\treturn false;
\t\t}}
\t}}
}}
"""


def generate() -> str:
    runelite_revision = resolve_runelite_revision()
    item_source = request_text(RUNELITE_ITEM_ID_URL.format(revision=runelite_revision))
    jagex_page = request_text(JAGEX_TROUVER_URL)

    low_tier_names = extract_jagex_list(jagex_page, "The items in this category are:")
    high_tier_names = extract_jagex_list(
        jagex_page,
        "For clarity, these 'higher tier' items are as follows:",
    )
    values, excluded_legacy, unlocked_high, unlocked_legacy = build_values(
        parse_gameval_items(item_source),
        low_tier_names,
        high_tier_names,
    )
    jagex_input = "\n".join(
        [f"deep-wilderness={DEEP_WILDERNESS_REPAIR_COST}"]
        + [f"low={name}" for name in sorted(low_tier_names)]
        + [f"high={name}" for name in sorted(high_tier_names)]
    )
    jagex_hash = hashlib.sha256(jagex_input.encode("utf-8")).hexdigest()
    return render_java(
        values,
        excluded_legacy,
        unlocked_high,
        unlocked_legacy,
        runelite_revision,
        jagex_hash,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail instead of writing when the generated file differs",
    )
    args = parser.parse_args()

    try:
        generated = generate()
    except (KeyError, OSError, ValueError, json.JSONDecodeError) as error:
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
