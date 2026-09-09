# UltiMenu — Feature Inventory

This document catalogues every operator- or player-visible function, command, content item and
configuration key in this repository, as read directly from source. It is an internal reference
for UAT execution and issue reconciliation — the public description of these features lives on
<https://doc.ultikits.com/>. Update this file in the same pull request as any feature change.

## Conventions

- **ID grammar:** `<repo-slug>.<area>.<action>`, dot-separated, every segment lowercase ASCII
  drawn from `[a-z0-9-]`. `<repo-slug>` is the repository name lowercased with no separators —
  `ultimenu` here, `ultichat`, `ultitools`, and `ultitools-example` for
  `UltiTools-External-Example`. `<area>` is the feature section's slug. `<action>` is the verb.
  A `config` row is the one shape that exceeds three segments and is exempt from the
  lowercase-ASCII rule for its key-path suffix:
  `<repo-slug>.config.<file-stem>.<yml key path>`, the key path keeping its own dots and its own
  casing verbatim from the yml file — a config ID is a citation of the key, not a re-derived
  slug. An ID changes only when the feature's identity changes, never on rewording. IDs are
  unique within a repository.
- **Menu-definition config rows use a format name, not a file name, as `<file-stem>` (repository
  extension of the grammar above).** This module's real configuration surface is not one bound
  file but a *format* — every `.yml` file a server owner drops into `plugins/UltiTools/UltiMenu/
  menus/` is independently parsed against the same schema (`MenuServiceImpl#parseMenuFile` /
  `#parseButton`), and the operator chooses the file's name freely (`menus/example.yml`,
  `menus/rules.yml`, …). Citing any one file's name as `<file-stem>` would be arbitrary and would
  not describe the other files that use the identical schema. These 18 rows therefore use the
  literal stand-in `menu-definition` — `ultimenu.config.menu-definition.size`,
  `ultimenu.config.menu-definition.buttons.item`, etc. — naming the schema the parser understands,
  not any single instance of it. This is the one config sub-shape in this document that is not
  bound by `@ConfigEntity`/`@ConfigEntry` at all; see the reconciliation table's own `@ConfigEntry`
  line for why the annotation-site count cannot see this surface.
- **Kind**, exactly these eight values: `command`, `config`, `event`, `gui`, `scheduled`,
  `placeholder`, `persistence`, `gate`. Each maps one-to-one onto a reconciliation-table line.
  This module has no `scheduled` rows (no `@Scheduled` method — the update pattern other modules
  use does not apply to a GUI that is only rendered on open/click, not on a timer), no
  `placeholder` rows (it consumes PlaceholderAPI variables inside button/menu text via
  `PlaceholderAPI#setPlaceholders`, it does not register its own expansion), no `persistence` rows
  (no `@Table`/`DataOperator` usage anywhere in this module's source — grepping this module's
  `src/main/java` for `@Table`, `DataOperator`, and `getOperator` each returns 0 hits; nothing a
  player does with a menu survives a restart except the menu *definition* files themselves, which
  are operator-authored content, not runtime state), and no `gate` rows (0 `@ConditionalOnConfig`
  sites) — all four Kinds stay in the vocabulary for cross-repository consistency even though none
  appears below.
- **Tier**, exactly three: `player`, `admin`, `internal`. Judged from what the feature is for, not
  from whether it carries a permission string.
- **Manual**, exactly three: `detailed`, `brief`, `none`.
- **Target**, exactly four: `player`, `console`, `both`, or `n/a` — the first three read straight
  off `@CmdTarget` for a `command` row; it is a property, not a tier. `n/a` is for every other Kind
  (`config`, `event`, `gui`) — the concept of "who this targets" does not apply to a config key or
  a GUI render pass the way it applies to a command.
- **Permission:** the literal node string, `none`, or `n/a`, each optionally suffixed with the
  literal text `(requireOp=true)` when the row's class-level `@CmdExecutor` carries that flag.
  Neither of this module's mechanisms sets `requireOp = true` — `MenuCommands`'s class-level
  `@CmdExecutor(permission = "ultikits.menu.use")` declares no `requireOp`, so no row below carries
  that suffix. **Repository extension of the suffix grammar:** `ultimenu.menu.reload` is gated by
  TWO independent permission checks that are not the same mechanism — the class-level
  `ultikits.menu.use` (framework-validated: Bukkit-registered via `CommandManager
  .registerCommandDirect`'s `command.setPermission(...)`, and re-checked by `PermissionValidator`)
  AND a second, hand-written `sender.hasPermission("ultikits.menu.admin")` check inside
  `MenuCommands#onReload` itself — a plain `if` statement, not a `@CmdMapping(permission = …)`
  declaration, so it is invisible to Bukkit's own tab-completion filtering and to
  `PermissionValidator`'s method-level branch (contrast `UAT-CHECKLIST.md`'s note on
  `PermissionValidator`'s two enforcement paths in the framework's own `FEATURES.md`/checklist,
  which this module's `reload` command deliberately does not use). This row's Permission cell
  states both, connected by ` + ` and marking the hand-written one:
  `ultikits.menu.use + ultikits.menu.admin (hand-checked, not framework-validated)`. `n/a` is for
  every Kind that is not `command`.
- **Source:** `ClassName#member` — the class and member that actually reads or applies the
  feature — for every Kind, `config` included. The 18 `menu-definition` config rows all cite
  `MenuServiceImpl#parseMenuFile` or `MenuServiceImpl#parseButton` (whichever method actually reads
  that key), never a bound field, because there is no bound field for any of them.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text. A
  hazard noticed while reading becomes a negative checklist row or a filed issue, not a note here,
  except where a feature's actual runtime behaviour genuinely diverges from what it appears to do
  (a dead key, an untranslated string) — that fact is itself part of "what the feature does" and is
  stated here as a plain, sourced observation, with the filed issue number, never as advice on how
  to fix it.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

This module is a single-root, single-file-per-class Maven project (9 files under `src/main/java`,
no worktree directory, no javadoc/string-literal false positive found for any annotation kind
measured below), so none of the three traps this command family defeats (multi-root layout, git
worktrees, javadoc/string mentions) actually changes any count for this repository — the same
robust command is still used, because it must work unmodified across all 18 repositories.

**Positive control:** the line-start form returns `@CmdExecutor` = 1, `@CmdMapping` = 4,
`@EventListener` = 1 (class), `@EventHandler` = 1 (handler method), `@Scheduled` = 0,
`@ConditionalOnConfig` = 0, `@ConfigEntity` = 1 (class), `@ConfigEntry` = 1 — confirmed by reading
`MenuCommands.java` directly (4 `@CmdMapping` sites at lines 49, 71, 85, 106: `<name>`,
`open <name>`, `list`, `reload`), `ItemBindListener.java` (1 `@EventHandler` site at line 64:
`onPlayerInteract`), and `MenuConfig.java` (1 `@ConfigEntry` site at line 14: `click_cooldown_ms`)
directly, not by trusting the count alone. This document's command-row count matches the
`@CmdMapping` annotation-site count exactly (4 against 4). The `@ConfigEntry` line is the one
reconciliation-table line this repository leaves deliberately unbalanced: 1 annotation site
against 19 `config` rows below (1 for `click_cooldown_ms` itself, plus 18 `menu-definition` rows)
— the reason is stated once, here and in the reconciliation table: this module's real
configuration surface is a runtime-parsed menu-definition format that no annotation binds, see the
Conventions entry above.

## Menu Commands

`MenuCommands` — class-level `@CmdExecutor(permission = "ultikits.menu.use", description = <a
Chinese literal, translated here per D-02's English-only rule as "menu management commands">,
alias = {"menu"})`, `@CmdTarget(BOTH)`. Per-command target and sender-type handling is inconsistent
by design across the four mappings (see each row's own note), the same pattern the framework's own
`FEATURES.md` and `Modules/UltiChat/FEATURES.md` document for their own wildcard-vs-exact-literal
command classes. `CommandManager#registerAll` (`plugin.i18n(cmdExecutor.description())`) runs this
description literal through `i18n(...)` before handing it to Bukkit as the registered command's own
description — and this specific literal has no `lang/en.json` entry either, the same defect class
as `ultimenu.menu.quick-open.neg-console`'s message (see `UAT-CHECKLIST.md`); both are filed under
the same issue, `UltiKits/UltiMenu#11`.

**Note on `/menu help` and bare `/menu`:** `BaseCommandExecutor#onCommand` short-circuits a literal
`help` argument to `MenuCommands#handleHelp` before format-matching runs (the same short-circuit
the framework's own `FEATURES.md` documents for `/ul help`). `handleHelp` sends four lines of
hardcoded Chinese text with no `i18n(...)` call at all, so `/menu help` never localizes regardless
of the `language` setting — unlike this module's four real commands below, every one of which
routes its player-facing text through `i18n(...)`. This document's command-row count is fixed at
exactly 4 (matching the 4 real `@CmdMapping` sites 1:1), with no added short-circuit-only row, the
same deliberate scope decision `Modules/UltiChat/FEATURES.md` records for its own `help`
short-circuits.

**Note on the reserved-name guard in `onQuickOpen`:** the method returns early, doing nothing, if
`name` case-insensitively equals `list`, `reload`, `open`, or `help` — a defensive guard against
the wildcard `<name>` mapping "stealing" those words from the other three mappings. Under this
framework's scored format matching (exact literal 10 pts, `<param>` 1 pt, exact-length bonus 5,
documented in the framework's own `CLAUDE.md`), a literal `/menu list` already scores higher against
the `list` mapping than against `<name>` before this guard ever runs, so the guard is unreachable
in practice; this is a source-level observation, not a testable behaviour, and no checklist row
below exercises it.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.menu.list | List every loaded menu's file name and (color-translated) title | command | `/menu list` | ultikits.menu.use | both | player | brief | MenuCommands#onList |
| ultimenu.menu.open | Open a named menu by its file-stem name, refusing a console sender before doing any work (`@CmdTarget(PLAYER)` on this mapping — Bukkit's own `SenderTypeValidator` rejects console here, not a hand-written check) | command | `/menu open <name>` | ultikits.menu.use | player | player | brief | MenuCommands#onOpen |
| ultimenu.menu.quick-open | Shorthand for `ultimenu.menu.open`: open a named menu directly as `/menu <name>`. Accepts `@CmdTarget(BOTH)` at the mapping (unlike `onOpen`) and hand-checks `sender instanceof Player` itself, sending an i18n-keyed refusal to a console sender instead of relying on the framework's sender-type validator — see `ultimenu.menu.quick-open.neg-console` in `UAT-CHECKLIST.md` for why this specific refusal message never actually localizes | command | `/menu <name>` | ultikits.menu.use | both | player | brief | MenuCommands#onQuickOpen |
| ultimenu.menu.reload | Reload every menu definition file from disk (see `ultimenu.config.menu-definition.*` below for what "definition" covers) and report the new menu count. Gated by TWO permission checks — see this row's own Permission cell for the mechanism split | command | `/menu reload` | ultikits.menu.use + ultikits.menu.admin (hand-checked, not framework-validated) | both | admin | brief | MenuCommands#onReload |

## Item Binding

`ItemBindListener` — a single `@EventHandler` on `PlayerInteractEvent`, checked against every
loaded menu's `bind-item`/`bind-name`/`bind-lore` fields (see the `menu-definition` config rows
below) on every right-click. Main-hand is checked before off-hand; the first matching menu (in
undefined `HashMap` iteration order — `MenuServiceImpl#menus` is a plain `HashMap`, not a
`LinkedHashMap`) wins if two menus somehow declare the identical bind item/name/lore combination.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.itembind.trigger | Right-click (air or block) while holding an item matching a loaded menu's `bind-item` type, and (if configured) its `bind-name`/`bind-lore` after color-code translation and stripping, cancels the interaction and opens that menu; a permission-gated bind refuses with a message instead of opening | event | right-click while holding a bound item, main hand checked first, then off hand | n/a | n/a | player | brief | ItemBindListener#onPlayerInteract |

## GUI Rendering

`CustomMenuGui` (`com.ultikits.plugins.menu.gui.CustomMenuGui`, package `com.ultikits.plugins.menu.gui`)
— the one class Phase 9's GUI-exclusion register removes from this module's JaCoCo `check` gate
(`.planning/phases/09-module-ecosystem-readiness-and-test-coverage/gui-exclusions/UltiMenu.md`),
and per D-19 this module's primary demonstration of the pixel evidence channel: nothing about
"does the menu look right" is answerable at the protocol layer. Every button placed in the GUI
carries its own click handler assembled from the button's own configuration (see the
`menu-definition` `buttons.*` config rows) — permission, price, player/console commands,
close-on-click, and an optional sub-menu link, all inside one `handleButtonClick` method, applied
in that fixed order: debounce → permission → economy → player commands → console commands →
sub-menu (if set, returns before the close-on-click check below runs) → close-on-click.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.menu.button-click | Handle a click on a placed button: a per-GUI-instance click debounce (`click_cooldown_ms`, shared across every button in that open inventory, not per-button), a button-level permission check, a Vault economy charge if `price` > 0, then dispatch of the button's configured player commands (as the clicking player) and console commands (as console, on the next tick, with `{player}` substituted first) | gui | click a placed button inside an open custom menu | n/a | n/a | player | brief | CustomMenuGui#handleButtonClick |
| ultimenu.menu.render | Render a menu's configured buttons into the inventory at their declared slot positions when the GUI opens, with `{player}` and PlaceholderAPI placeholders resolved and `&`-color codes translated in the display name and lore, and custom model data applied if set | gui | open any menu (`/menu <name>`, `/menu open <name>`, or a bound item) | n/a | n/a | player | brief | CustomMenuGui#onOpen |
| ultimenu.menu.submenu-open | A button whose `open-menu` key names another loaded menu closes the current inventory and opens the named menu on the next server tick; permission, economy and player/console commands configured on the SAME button still run first, in the fixed order this section's preamble states — only the trailing `close-on-click` branch is skipped in favor of the sub-menu navigation, not those earlier effects | gui | click a button configured with `open-menu` | n/a | n/a | player | brief | CustomMenuGui#handleButtonClick |

## Configuration

### Framework-bound (`@ConfigEntity`/`@ConfigEntry`)

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.config.config.click_cooldown_ms | Global per-GUI-instance click debounce, shared by every button in one open menu (not one debounce timer per button) | config | `config/config.yml: click_cooldown_ms (default: 200, range 50-5000)` | n/a | n/a | admin | brief | CustomMenuGui#handleButtonClick |

### Menu-definition format (runtime-parsed, not annotation-bound)

Every key `MenuServiceImpl#parseMenuFile`/`#parseButton` reads from a menu definition file, at the
`menu-definition` format name explained in the Conventions section above. 18 rows: 7 top-level menu
keys, 11 per-button keys (a menu's `buttons` map may hold any number of entries, each parsed
identically). Defaults below are this parser's own fallback when a key is absent — NOT necessarily
the shipped `menus/example.yml`'s own values, which are also given per row for the one row where
they differ meaningfully.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.config.menu-definition.bind-item | Item type (Bukkit `Material` name, case-insensitive) that, held in either hand, triggers opening this menu on right-click; an unrecognized material name is silently ignored (parsed as absent, `bindItem` stays `null`, no binding for that menu) | config | `menus/<name>.yml: bind-item (default: unset — no binding)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.bind-lore | A single string that must appear as a substring of at least one (color-stripped) lore line on the held item for the bind to trigger; unset means lore is not checked | config | `menus/<name>.yml: bind-lore (default: unset — lore not checked)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.bind-name | The held item's (color-stripped) display name must exactly equal this value for the bind to trigger; unset means display name is not checked | config | `menus/<name>.yml: bind-name (default: unset — name not checked)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.buttons.close-on-click | Whether clicking this button closes the inventory afterward — ignored when the button also sets `open-menu`, since a sub-menu link always closes-then-reopens regardless of this key's value | config | `menus/<name>.yml: buttons.<id>.close-on-click (default: true)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.console-commands | Console commands dispatched on click, `{player}` replaced with the clicking player's name and PlaceholderAPI-resolved, run via `Bukkit#dispatchCommand` against the console sender on the main thread on the next server tick (not synchronously in the click handler) | config | `menus/<name>.yml: buttons.<id>.console-commands (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.custom-model-data | Custom model data applied to the button's item; `0` (the default) means none is applied — `ItemMeta#setCustomModelData` is only called when this value is greater than zero | config | `menus/<name>.yml: buttons.<id>.custom-model-data (default: 0 — none applied)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.item | The button's item material (Bukkit `Material` name, case-insensitive); required — a button whose `item` is missing, or names an unrecognized material, is skipped entirely (a warning is logged, no button placed for that entry) | config | `menus/<name>.yml: buttons.<id>.item (required, no default)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.lore | The button's item lore lines, `&`-color-coded and placeholder-resolved when rendered | config | `menus/<name>.yml: buttons.<id>.lore (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.name | The button's item display name, `&`-color-coded and placeholder-resolved when rendered; if empty or unset, the item's default (material) name shows instead | config | `menus/<name>.yml: buttons.<id>.name (default: unset)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.open-menu | Name of another loaded menu to open (closing the current one) when this button is clicked, instead of running the close-on-click behaviour; a name that does not match any loaded menu produces a refusal message and the current menu stays open | config | `menus/<name>.yml: buttons.<id>.open-menu (default: unset — no sub-menu link)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.permission | Permission node required to click this specific button; unset or empty means any viewer of the menu may click it (the menu-level `permission` key already gated who can open the menu at all) | config | `menus/<name>.yml: buttons.<id>.permission (default: unset — no button-level check)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.player-commands | Commands dispatched as the clicking player (`Player#performCommand`), `{player}` replaced with the player's own name and PlaceholderAPI-resolved, executed synchronously in the click handler (not deferred a tick, unlike `console-commands`) | config | `menus/<name>.yml: buttons.<id>.player-commands (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.position | The button's inventory slot index (0-based, within the menu's total `size` slots); no bounds validation against `size` is performed by the parser itself — Bukkit's own inventory API is what would reject an out-of-range slot | config | `menus/<name>.yml: buttons.<id>.position (default: 0)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.price | Vault currency amount withdrawn from the clicking player before running the button's commands; `0` (the default) skips the economy check entirely — no Vault dependency, no balance check, for a free button | config | `menus/<name>.yml: buttons.<id>.price (default: 0.0 — no charge)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.command | Declared and stored on `MenuDefinition#command`; never read anywhere else in this module's source (`MenuDefinition#getCommand()` has 0 call sites outside its own class) — setting this key has no observable effect. Only `/menu <name>` and `/menu open <name>` (or a matching bound item) can ever open a menu; no per-menu dedicated command is ever registered from it. A known product defect, `UltiKits/UltiMenu#12` | config | `menus/<name>.yml: command (default: unset, has no effect regardless of value, see UltiKits/UltiMenu#12)` | n/a | n/a | admin | detailed | MenuDefinition#command |
| ultimenu.config.menu-definition.permission | Permission required to open this menu, checked in `MenuCommands#openMenuByName` (the `/menu`/`/menu open` command paths) and `ItemBindListener#tryOpenMenuForItem` (the bound-item path) before the GUI is constructed. The two paths are NOT equally gated: the command path additionally requires the class-level `ultikits.menu.use` (Bukkit-registered on `MenuCommands`) before `openMenuByName` is ever reached, but `tryOpenMenuForItem` checks ONLY `menu.getPermission()` and never `ultikits.menu.use` at all — when this key is unset or empty, a player who does NOT hold `ultikits.menu.use` can still open the menu via a bound item, bypassing the base command permission entirely (`UltiKits/UltiMenu#14`, filed, not fixed here) | config | `menus/<name>.yml: permission (default: unset — no per-menu check on either path)` | n/a | n/a | admin | detailed | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.size | Inventory row count × 9, validated to be 9-54 and an exact multiple of 9; a file with an invalid size is rejected entirely (logged warning, the whole menu fails to load — no partial load) | config | `menus/<name>.yml: size (default: 27, valid range 9-54, multiple of 9)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.title | The menu's inventory title, `&`-color-coded and `{player}`/PlaceholderAPI-resolved before display | config | `menus/<name>.yml: title (default: "Menu")` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
