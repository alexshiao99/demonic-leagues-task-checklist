# Demonic Leagues Task Checklist

A RuneLite plugin that tracks your progress through the **Leagues VI — Demonic Pacts** task list from your sidebar, with live completion updates and an on-screen details overlay for the task you're currently working on.

## Features

- **Sidebar panel** listing all 1,592 Demonic Pacts tasks with live completion state read from the game client
- **Filter by region** — General, Varlamore, Karamja, Asgarnia, Desert, Fremennik, Kandarin, Kourend, Morytania, Tirannwn, Wilderness
- **Hide completed** — collapse the list to only unfinished tasks
- **Hide unmet skill requirement** — show only tasks you have the levels for right now
- **Search** by task name or description
- **Click a task** to select it — an overlay appears on the game canvas with the full description, completion status, skill requirements (color-coded red/green against your current levels), and any other requirements
- Progress counter in the panel header: *completed / total / shown*

## How completion is detected

The game stores per-task completion in a set of 62 VarPlayers (`LEAGUE_TASK_COMPLETED_0` through `_61`). Each varp holds a 32-bit bitfield, so task ID *N* is bit `N % 32` of varp `N / 32`. The plugin reads these directly — no manual marking, no chat scraping — and refreshes the panel whenever a `VarbitChanged` event fires on one of those varps.

## Data source & attribution

The task catalog shipped in `tasks.json` and the 11 per-region icons (`*_icon.png`) are taken from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki/):

- Task data — [Demonic Pacts League/Tasks](https://oldschool.runescape.wiki/w/Demonic_Pacts_League/Tasks)
- General region icon — [Globe-icon.png](https://oldschool.runescape.wiki/w/File:Globe-icon.png)
- Other region icons — `{Region} Area Badge.png` on the wiki (e.g. [Varlamore Area Badge](https://oldschool.runescape.wiki/w/File:Varlamore_Area_Badge.png))

Wiki content is licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/). This plugin uses only the factual task metadata (id, name, description, tier, region, skill and other requirements) and the region icon imagery for read-only display purposes, and attributes the OSRS Wiki and its contributors accordingly.

The extractor script (`ci/extract_league_tasks.py` in the source repository) fetches raw wikitext via the MediaWiki `?action=raw` endpoint and parses `{{DPLTaskRow|…}}` templates into JSON. To regenerate:

```bash
curl -sS "https://oldschool.runescape.wiki/w/Demonic_Pacts_League/Tasks?action=raw" -o /tmp/demonic_pacts_tasks.wikitext
python3 scripts/extract_league_tasks.py
```

## Development

```bash
./gradlew run   # launches RuneLite with this plugin side-loaded
```

Requires JDK 11.

## Known limitations

- The task catalog is a **static snapshot**. If Jagex adds or changes tasks after this plugin was built, they won't appear until a new release with an updated `tasks.json`.
- The "Hide unmet skill requirement" filter only models **skill** requirements. Quest, item, and area-unlock prerequisites are shown in the overlay but aren't filterable — a task like "Equip a Seercull" may appear even if the underlying quest isn't started.
- The plugin does not gate on world type, so completion flags you see reflect whatever the `LEAGUE_TASK_COMPLETED_*` varps currently hold. On a non-leagues account those will simply be all zero.

## License

BSD 2-clause. See individual source files for copyright headers.
