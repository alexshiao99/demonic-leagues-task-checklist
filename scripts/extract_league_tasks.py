#!/usr/bin/env python3
"""Extract Demonic Pacts league tasks from the OSRS wiki into tasks.json.

Reads raw wikitext from /tmp/demonic_pacts_tasks.wikitext and writes
tasks.json to the plugin's resource directory.
"""
import json
import re
import sys
from pathlib import Path

WIKITEXT_PATH = Path("/tmp/demonic_pacts_tasks.wikitext")
OUTPUT_PATH = Path(__file__).resolve().parent.parent / \
    "src/main/resources/com/smicalexshiao/leaguetasks/tasks.json"

TIERS = {"easy", "medium", "hard", "elite", "master"}
REGIONS = {
    "General", "Varlamore", "Karamja", "Asgarnia", "Desert",
    "Fremennik", "Kandarin", "Kourend", "Morytania", "Tirannwn",
    "Wilderness",
}

TEMPLATE_OPEN = "{{DPLTaskRow|"


def find_template_bodies(wikitext: str) -> list[str]:
    bodies = []
    i = 0
    while True:
        start = wikitext.find(TEMPLATE_OPEN, i)
        if start == -1:
            return bodies
        body_start = start + len(TEMPLATE_OPEN)
        brace_depth = 1
        link_depth = 0
        j = body_start
        while j < len(wikitext) and brace_depth > 0:
            if wikitext[j:j+2] == "{{":
                brace_depth += 1
                j += 2
            elif wikitext[j:j+2] == "}}":
                brace_depth -= 1
                if brace_depth == 0:
                    bodies.append(wikitext[body_start:j])
                    j += 2
                    break
                j += 2
            elif wikitext[j:j+2] == "[[":
                link_depth += 1
                j += 2
            elif wikitext[j:j+2] == "]]":
                link_depth = max(0, link_depth - 1)
                j += 2
            else:
                j += 1
        i = j


# Split top-level | separators — ignore | inside [[...]] or {{...}}.
def split_template_args(body: str) -> list[str]:
    args = []
    link_depth = 0
    brace_depth = 0
    current = []
    i = 0
    while i < len(body):
        if body[i:i+2] == "[[":
            link_depth += 1
            current.append("[[")
            i += 2
            continue
        if body[i:i+2] == "]]":
            link_depth = max(0, link_depth - 1)
            current.append("]]")
            i += 2
            continue
        if body[i:i+2] == "{{":
            brace_depth += 1
            current.append("{{")
            i += 2
            continue
        if body[i:i+2] == "}}":
            brace_depth = max(0, brace_depth - 1)
            current.append("}}")
            i += 2
            continue
        if body[i] == "|" and link_depth == 0 and brace_depth == 0:
            args.append("".join(current))
            current = []
            i += 1
            continue
        current.append(body[i])
        i += 1
    args.append("".join(current))
    return args


# [[target|display]] -> display; [[target]] -> target; strip bold/italic markers.
WIKILINK_PIPED = re.compile(r"\[\[([^\[\]|]+)\|([^\[\]]+)\]\]")
WIKILINK_SIMPLE = re.compile(r"\[\[([^\[\]]+)\]\]")
BOLD_ITALIC = re.compile(r"'{2,5}")


WHITESPACE_RE = re.compile(r"\s+")


def clean_wikitext(text: str) -> str:
    text = WIKILINK_PIPED.sub(r"\2", text)
    text = WIKILINK_SIMPLE.sub(r"\1", text)
    text = BOLD_ITALIC.sub("", text)
    text = WHITESPACE_RE.sub(" ", text)
    return text.strip()


# Convert skill-requirement templates like {{SCP|Magic|9|link=yes}} to "Magic 9".
SCP_RE = re.compile(r"\{\{SCP\|([^|}]+)\|([^|}]+)(?:\|[^}]*)?\}\}")
# Drop any remaining templates like {{SCP|Quest}} or decorative wrappers.
STRAY_TEMPLATE_RE = re.compile(r"\{\{[^{}]*\}\}")


def clean_requirements(text: str) -> str:
    text = SCP_RE.sub(r"\1 \2", text)
    # Strip leftover templates that didn't match the skill-level form, including
    # repeatedly so nested cases collapse.
    prev = None
    while prev != text:
        prev = text
        text = STRAY_TEMPLATE_RE.sub("", text)
    text = clean_wikitext(text)
    return text.strip()


def parse_template(body: str) -> dict | None:
    args = split_template_args(body)
    if len(args) < 2:
        return None
    positional = []
    named = {}
    for a in args:
        if "=" in a:
            k, _, v = a.partition("=")
            named[k.strip()] = v
        else:
            positional.append(a)
    if len(positional) < 2:
        return None
    name = clean_wikitext(positional[0])
    description = clean_wikitext(positional[1])
    tier = named.get("tier", "").strip().lower()
    region = named.get("region", "").strip()
    raw_id = named.get("id", "").strip()
    if tier not in TIERS:
        print(f"WARN: unknown tier for {name!r}: {tier!r}", file=sys.stderr)
        return None
    if not raw_id.isdigit():
        print(f"WARN: missing id for {name!r}", file=sys.stderr)
        return None
    if region not in REGIONS:
        print(f"WARN: unknown region for {name!r}: {region!r}", file=sys.stderr)
    return {
        "id": int(raw_id),
        "name": name,
        "description": description,
        "tier": tier.upper(),
        "region": region,
        "skillRequirements": clean_requirements(named.get("s", "")),
        "otherRequirements": clean_requirements(named.get("other", "")),
        "pactTask": named.get("pactTask", "").strip().lower() == "yes",
    }


def main() -> int:
    wikitext = WIKITEXT_PATH.read_text(encoding="utf-8")
    tasks = []
    seen_ids = set()
    for body in find_template_bodies(wikitext):
        task = parse_template(body)
        if task is None:
            continue
        if task["id"] in seen_ids:
            print(f"WARN: duplicate id {task['id']}: {task['name']!r}", file=sys.stderr)
            continue
        seen_ids.add(task["id"])
        tasks.append(task)
    tasks.sort(key=lambda t: t["id"])
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(
        json.dumps(tasks, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    print(f"Wrote {len(tasks)} tasks to {OUTPUT_PATH}")
    regions = {}
    tiers = {}
    max_id = 0
    for t in tasks:
        regions[t["region"]] = regions.get(t["region"], 0) + 1
        tiers[t["tier"]] = tiers.get(t["tier"], 0) + 1
        max_id = max(max_id, t["id"])
    print(f"Max id: {max_id}  (fits in {(max_id // 32) + 1} varps)")
    print(f"Regions: {regions}")
    print(f"Tiers: {tiers}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
