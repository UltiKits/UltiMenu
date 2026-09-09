# UltiMenu — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the steps
to exercise it and the observable truth that proves it works. It is an internal reference for
real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`.

## Conventions

- **Columns:** `ID`, `Preconditions`, `Steps`, `Expected`, `Layer`, `Covers`.
- **ID:** cites its `FEATURES.md` ID verbatim. A negative case suffixes the checklist ID only, as
  `.neg-<slug>` — a negative case still tests the same feature, so the base ID is unchanged.
- **Layer**, copied verbatim from Laojun's own `ultitools-real-client-uat` skill so no translation
  step exists at dispatch time: `protocol`, `java-client`, `os-input`, `pixel`, `server`, `human`.
  **This module's own division of labor across those values (D-19):** a row asserting that
  specific items, in specific slots, with specific names/lore/colors, appear inside the rendered
  GUI is `pixel` — nothing short of a real client window and a validated screenshot settles
  "does the menu look right", and this module's whole product is that GUI. A row asserting that a
  GUI *of some title* opened at all (without asserting its contents) is `java-client` — a
  programmatic client observing the inventory-open packet's title can settle that without a
  screenshot. A row asserting a server-side side effect of a click (an economy withdrawal, a
  dispatched command, a chat/console message) is `server`. No row in this document is downgraded
  from `pixel` to `protocol` to make it easier to run — an unready pixel harness is a legitimate
  `human-uat-pending` exit; a `protocol` assertion standing in for a rendering claim is not.
- **Human-authenticated-session rows (D-27b):** none exist in this module — it has no capability
  gated behind the maintainer's own UltiCloud panel session or an SMTP-gated recovery flow. Stated
  here for template consistency with the framework's own checklist.
- **Covers** back-references a Phase 9 GUI-excluded class name; left blank when no such class
  applies. This module's excluded class is `CustomMenuGui`
  (`.planning/phases/09-module-ecosystem-readiness-and-test-coverage/gui-exclusions/UltiMenu.md`)
  — every row below whose `Source` cites that class carries it in `Covers`.
- A row whose Preconditions name a prior row must appear after that row in file order — asserted
  mechanically: for every row, every checklist ID cited in its Preconditions cell must have a
  strictly smaller line number in this file than the row citing it (sweep class 8, D-27a).
- **Config-per-file rule (D-06):** one checklist row per `@ConfigEntity`-annotated class or per
  shipped-format yml file, never one row per key. Two such rows exist here: `ultimenu.config.
  config-yml` (the one `@ConfigEntity` class) and `ultimenu.config.menu-definition-yml` (the
  runtime-parsed menu-definition format, exercised through the shipped `menus/example.yml`).
- This module's shipped defaults are `language: "zh"` (core `config.yml`, not this module's own)
  and an empty `lang/zh.json` in this module (every Chinese string used as an `i18n(...)` key is
  its own value under `language: zh`, so an empty `zh.json` produces correct output by
  construction — not a defect). Every row below whose Expected quotes a literal in-game or console
  line therefore carries the precondition `language: en` set in `plugins/UltiTools/config.yml`, so
  the observed line matches this document's English-only text exactly — **except
  `ultimenu.menu.quick-open.neg-console` and the `@CmdExecutor` description assertion inside
  `ultimenu.config.menu-definition-yml`**, which quote the actual (untranslated, Chinese) text a
  known product defect produces regardless of the `language` setting (`UltiKits/UltiMenu#11`).

## Menu Commands

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultimenu.menu.list | `language: en`; at least one menu loaded (the shipped `menus/example.yml`, unmodified) | Run `/menu list` | Chat/console shows `=== Available Menus ===` (gold), then one line per loaded menu reading `<filename> - <color-translated title>` (aqua filename, white separator) — for the shipped default: `example - Server Menu - <player>` (title's `{player}` token resolved to the sender's own name) | server | |
| ultimenu.menu.list.neg-empty | `language: en`; the `menus/` folder exists but contains zero `.yml` files (rename or remove `example.yml`, then `/menu reload`) | Run `/menu list` | Chat/console shows `No menus available` (yellow) — no header, no entry lines; `MenuServiceImpl#loadMenus` logged `No menu configuration files found` on the preceding reload | server | |
| ultimenu.menu.open | `language: en`; the shipped `example` menu present; sender is a player holding `ultikits.menu.use` | Run `/menu open example` | A GUI opens titled `Server Menu - <player>` (color-translated, `{player}` resolved to the opening player's own name) — see `ultimenu.menu.render` for the full slot-by-slot content assertion | java-client | |
| ultimenu.menu.open.neg-console | `language: en`; run from console (not a player) | Run `/menu open example` | The command is rejected before `MenuCommands#onOpen` runs at all — the framework's own sender-type validator refusal (this mapping carries `@CmdTarget(PLAYER)`), not this module's own hand-written message; no GUI opens, nothing else happens | server | |
| ultimenu.menu.quick-open | `language: en`; the shipped `example` menu present; sender is a player | Run `/menu example` | A GUI opens titled `Server Menu - <player>`, functionally identical to `ultimenu.menu.open`'s result | java-client | |
| ultimenu.menu.quick-open.neg-console | `language: en`; run from console | Run `/menu example` | The console receives the LITERAL, untranslated Chinese i18n-key text at `MenuCommands.java:60` (translated here per D-02's English-only rule as "Only players can open the menu!") — not an English refusal — because `plugin.i18n(<that Chinese literal>)` has no `lang/en.json` entry and falls back to returning its own lookup key verbatim. A known product defect, `UltiKits/UltiMenu#11` | server | |
| ultimenu.menu.quick-open.neg-not-found | `language: en`; no menu named `does-not-exist` is loaded | Run `/menu does-not-exist` | Chat shows `Menu 'does-not-exist' does not exist!` (red) | server | |
| ultimenu.menu.reload | `language: en`; sender holds both `ultikits.menu.use` and `ultikits.menu.admin`; edit `menus/example.yml`'s `title` to a distinguishable value on disk first (do not reload yet) | Run `/menu reload`, then `/menu list` | Chat/console shows `Reloaded 1 menus` (green); the immediately following `/menu list` shows the EDITED title, not the previous one — `MenuServiceImpl#loadMenus` re-parsed the file from disk | server | |
| ultimenu.menu.reload.neg-permission | `language: en`; sender holds `ultikits.menu.use` (so it reaches `onReload` at all) but NOT `ultikits.menu.admin` | Run `/menu reload` | Chat shows `You don't have permission to execute this command!` (red); `menuService.reload()` is never called — the sender's own edited-on-disk title from a prior attempt (if any) is NOT picked up, confirming no reload happened | server | |

## Item Binding

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultimenu.itembind.trigger | The shipped `example` menu present (`bind-item: COMPASS`, `bind-name: "Server Menu"` [color-translated], `bind-lore: "Right-click to open"` [color-translated]); sender's MAIN hand holds a compass with that exact display name and a lore line containing that substring; sender holds `ultikits.menu.use` | Right-click (air or a block) while holding the compass | The interaction is cancelled (the compass is not otherwise used); a GUI opens for the `example` menu, functionally identical to `ultimenu.menu.open`'s result | java-client | |
| ultimenu.itembind.trigger.neg-wrong-name | Same compass item type, but its display name does NOT match `example`'s `bind-name` (a plain, unrenamed compass) | Right-click while holding the plain compass | No menu opens; the interaction proceeds normally (e.g. the compass points to its lodestone/spawn as usual, no `event.setCancelled` was called for this item) | java-client | |
| ultimenu.itembind.trigger.off-hand | Main hand holds an unrelated item (e.g. dirt); off-hand holds the same matching compass as `ultimenu.itembind.trigger` | Right-click while holding both | A GUI opens for the `example` menu — `ItemBindListener#onPlayerInteract` fell through the main-hand check (dirt does not match any menu's `bind-item`) and matched on the off-hand item instead | java-client | |

## GUI Rendering

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultimenu.menu.render | The shipped, unmodified `example` menu; sender is a player; PlaceholderAPI installed with the `server` expansion (for `%server_online%`) | Run `/menu example` and observe the opened inventory | The inventory has exactly 3 rows (27 slots, per `size: 27`), titled `Server Menu - <player>`; slot 10 holds a BOOK named `Server Info` (bold, aqua) with 2 lore lines (`Welcome to the server!` and `Online: <n>`, the second with the live online-player count substituted for `%server_online%`); slot 12 holds an ENDER_PEARL named `Spawn` (bold, pink) with 2 lore lines; slot 14 holds an IRON_SWORD named `Starter Kit` (bold, yellow) with 2 lore lines; slot 16 holds a WRITABLE_BOOK named `Server Rules` (bold, red) with 1 lore line; every other slot is empty | pixel | CustomMenuGui |
| ultimenu.menu.render.neg-empty | A menu definition yml with `buttons:` omitted entirely (no key at all, not merely an empty map) | Open that menu | The inventory opens (correct size and title) but shows NO items in ANY slot — `CustomMenuGui#onOpen` returns immediately when `menuDefinition.getButtons()` is null | pixel | CustomMenuGui |
| ultimenu.menu.button-click | The shipped `example` menu open; sender clicks the `spawn` button (`ENDER_PEARL`, no `permission`, `price: 0`, `player-commands: ["spawn"]`, `close-on-click` unset -> default `true`) | Click the `spawn` button | The inventory closes; the server dispatches `/spawn` AS the clicking player (`Player#performCommand`), synchronously — confirmed by the player's own subsequent teleport/command-feedback for `/spawn` on this server | server | CustomMenuGui |
| ultimenu.menu.button-click.neg-debounce | The shipped `example` menu open; `click_cooldown_ms` at its shipped default (200ms) | Click the `spawn` button twice in rapid succession, well under 200ms apart (a scripted double-click, not two manual clicks) | Only the FIRST click's `/spawn` dispatch is observed; the second click, inside the debounce window, produces no server-side effect at all — `CustomMenuGui#handleButtonClick` returns before doing anything | server | CustomMenuGui |
| ultimenu.menu.button-click.neg-permission | A button in a test menu with `permission: "test.button.gate"` set; the clicking player does NOT hold that node | Click the gated button | Chat shows `You don't have permission to use this button!` (red); none of the button's configured commands run, and the inventory does NOT close | server | CustomMenuGui |
| ultimenu.menu.button-click.economy | The shipped `example` menu open; sender clicks `kit-starter` (`IRON_SWORD`, `price: 100`, 3 console commands); Vault + an Economy provider installed; sender's balance >= 100 | Click the `kit-starter` button | Chat shows `Deducted $100.00` (green, exact currency format per the installed Economy provider); the sender's balance decreases by exactly 100; on the NEXT server tick, the three console commands run (`give <player> iron_sword 1`, `give <player> iron_pickaxe 1`, `give <player> bread 16`), delivering those items to the player's inventory; the GUI closes (`close-on-click` unset -> default `true`) | server | CustomMenuGui |
| ultimenu.menu.button-click.neg-economy-insufficient | Same as `ultimenu.menu.button-click.economy`, except the sender's balance is < 100 | Click the `kit-starter` button | Chat shows `Insufficient balance! Requires $100.00` (red); NO balance change, none of the three console commands run, the GUI does NOT close | server | CustomMenuGui |
| ultimenu.menu.button-click.neg-economy-unavailable | Same as `ultimenu.menu.button-click.economy`, except Vault (or any Economy provider) is NOT installed | Click the `kit-starter` button | Chat shows `Economy system is not available!` (red); no balance check is even attempted, none of the three console commands run | server | CustomMenuGui |
| ultimenu.menu.submenu-open | The shipped `example` menu open; sender clicks `rules` (`WRITABLE_BOOK`, `open-menu: rules`); a second menu file `menus/rules.yml` is loaded with at least one distinct button of its own | Click the `rules` button | The `example` inventory closes; on the NEXT server tick, a NEW inventory opens for the `rules` menu, showing THAT menu's own title and buttons — visually and titularly distinct from `example`'s inventory, not a re-render of the same GUI | pixel | CustomMenuGui |
| ultimenu.menu.submenu-open.neg-not-found | A test menu with a button whose `open-menu` names a menu that is NOT currently loaded (e.g. `does-not-exist`) | Click that button | Chat shows `Menu 'does-not-exist' does not exist!` (red); the CURRENT menu's inventory stays open — `CustomMenuGui#handleButtonClick` returns before calling `player.closeInventory()` | server | CustomMenuGui |

## Configuration

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultimenu.config.config-yml | Fresh `config/config.yml` at its shipped default (`click_cooldown_ms: 200`) | Load the file; confirm the one key is present at its documented default; then set `click_cooldown_ms: 2000` (default 200), reload, and repeat `ultimenu.menu.button-click.neg-debounce`'s double-click but with a 500ms gap between clicks (inside the NEW 2000ms window, outside the OLD 200ms one) | The key is present at 200 before the change; after raising it to 2000, a second click 500ms after the first is ALSO suppressed — proving the raised value took effect, not merely surviving the old shorter window | protocol | |
| ultimenu.config.menu-definition-yml | Fresh `menus/example.yml` at its shipped content (7 top-level keys, 4 buttons each carrying its own 11 keys) | Load the file; confirm all 7 top-level keys (`size`, `title`, `command`, `permission`, `bind-item`, `bind-name`, `bind-lore`) and, for each of the 4 shipped buttons, all 11 button keys are present at their documented values; then exercise 3 representative changes in separate reloads so no change masks another's effect: (a) set `size: 10` (invalid — not a multiple of 9) and reload, confirming the menu fails to load entirely (a warning is logged, `/menu list` no longer shows `example`); restore `size: 27` before continuing; (b) set `permission: "test.menu.gate"` (default unset) and confirm a player lacking that node is refused with `You don't have permission to open this menu!` on `/menu example`, where an unset `permission` previously let them through; (c) confirm the class-level `@CmdExecutor` description registered for `/menu` at boot is the literal, untranslated Chinese string at `MenuCommands.java:27` (translated here per D-02's English-only rule as "menu management commands") regardless of `language: en` — `plugin.i18n(cmdExecutor.description())` has no `lang/en.json` entry for this literal either, the same defect class as `ultimenu.menu.quick-open.neg-console` (`UltiKits/UltiMenu#11`) — observable via `/help menu` or an equivalent Bukkit help listing. Do NOT expect the `command` key to have any effect when varied — it is declared and parsed but never read (`UltiKits/UltiMenu#12`) | All 18 keys present at their documented values before any change; (a) the invalid size fails the whole menu closed, not merely clamped; (b) the permission gate takes effect exactly as `ultimenu.config.menu-definition.permission` describes; (c) the registered command description is the raw Chinese literal at `MenuCommands.java:27`, matching the filed defect | server | |
