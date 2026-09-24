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
  file but a *format* — every `.yml` file a server owner drops into
  `plugins/UltiTools/pluginConfig/UltiTools-Menu/menus/` is independently parsed against the same schema (`MenuServiceImpl#parseMenuFile` /
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
  that key) — except the removed `command` key, which is read only to report that it is still
  present and so cites `MenuServiceImpl#warnIfRemovedKeysPresent` (called from `parseMenuFile`) —
  never a bound field, because there is no bound field for any of them.
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

This module is a single-root, single-file-per-class Maven project (10 files under `src/main/java`,
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
line this repository's pull request's own Annotation-site reconciliation table (D-07 — carried in
the pull request BODY, not in this file) leaves deliberately unbalanced: 1 annotation site
against 19 `config` rows below (1 for `click_cooldown_ms` itself, plus 18 `menu-definition` rows)
— the reason is stated once, here and in that pull request table: this module's real
configuration surface is a runtime-parsed menu-definition format that no annotation binds, see the
Conventions entry above.

## Menu Commands

`MenuCommands` — class-level `@CmdExecutor(permission = "ultikits.menu.use", description =
"menu.command.description", alias = {"menu"})`, `@CmdTarget(BOTH)`. Per-command target and sender-type handling is inconsistent
by design across the four mappings (see each row's own note), the same pattern the framework's own
`FEATURES.md` and `Modules/UltiChat/FEATURES.md` document for their own wildcard-vs-exact-literal
command classes. `ultikits.menu.use` reaches the two opening
mappings twice over: Bukkit rejects the command without it, and `MenuAccess#allowOpen` then
requires it again before the GUI is built. That is deliberate and is not a second node — the
framework gate protects this one class, while the module's rule has to hold for the bound-item
and `open-menu` paths too, which do not pass through it (see
`ultimenu.config.menu-definition.permission`).
`CommandManager#registerAll` (`plugin.i18n(cmdExecutor.description())`) runs this
description key through `i18n(...)` before handing it to Bukkit as the registered command's own
description, so it follows the `language` setting: `Menu management command` under `language: en`.
Before `UltiKits/UltiMenu#11` was fixed the description was a Chinese sentence with no `lang/en.json`
entry, so it stayed Chinese under every language, as did `ultimenu.menu.quick-open`'s console refusal.

**Note on `/menu help` and bare `/menu`:** `BaseCommandExecutor#onCommand` short-circuits a literal
`help` argument to `MenuCommands#handleHelp` before format-matching runs (the same short-circuit
the framework's own `FEATURES.md` documents for `/ul help`). `handleHelp` sends a header and four
command lines through `i18n(...)`, like every player-facing line of this module's four real commands
below, so `/menu help` follows the `language` setting (exercised by `ultimenu.i18n.language` in
`UAT-CHECKLIST.md`); before 6.3.0 these five lines were fixed Chinese text. This document's command-row count is fixed at
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
| ultimenu.menu.quick-open | Shorthand for `ultimenu.menu.open`: open a named menu directly as `/menu <name>`. Accepts `@CmdTarget(BOTH)` at the mapping (unlike `onOpen`) and hand-checks `sender instanceof Player` itself, sending an i18n-keyed refusal to a console sender instead of relying on the framework's sender-type validator — see `ultimenu.menu.quick-open.neg-console` in `UAT-CHECKLIST.md`; the refusal follows the `language` setting (`Only players can open menus!` under `language: en`), which before `UltiKits/UltiMenu#11` was fixed it did not | command | `/menu <name>` | ultikits.menu.use | both | player | brief | MenuCommands#onQuickOpen |
| ultimenu.menu.reload | Reload every menu definition file from disk (see `ultimenu.config.menu-definition.*` below for what "definition" covers) and report the new menu count. Gated by TWO permission checks — see this row's own Permission cell for the mechanism split | command | `/menu reload` | ultikits.menu.use + ultikits.menu.admin (hand-checked, not framework-validated) | both | admin | brief | MenuCommands#onReload |

## Item Binding

`ItemBindListener` — a single `@EventHandler` on `PlayerInteractEvent`, checked against every
loaded menu's `bind-item`/`bind-name`/`bind-lore` fields (see the `menu-definition` config rows
below) on every right-click. Main-hand is checked before off-hand; the first matching menu (in
undefined `HashMap` iteration order — `MenuServiceImpl#menus` is a plain `HashMap`, not a
`LinkedHashMap`) wins if two menus somehow declare the identical bind item/name/lore combination.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.itembind.trigger | Right-click (air or block) while holding an item matching a loaded menu's `bind-item` type, and (if configured) its `bind-name`/`bind-lore` after color-code translation and stripping, cancels the interaction and opens that menu — provided the holder passes `MenuAccess#allowOpen`, the one access rule all three open paths share: `ultikits.menu.use` plus the menu's own `permission` key when it sets one. A holder who does not pass it gets a refusal message and no menu, and the interaction is still cancelled, so the item is not used as an ordinary item either | event | right-click while holding a bound item, main hand checked first, then off hand | ultikits.menu.use (+ the target menu's own `permission` key when it sets one) | n/a | player | brief | ItemBindListener#onPlayerInteract |

## GUI Rendering

`CustomMenuGui` (`com.ultikits.plugins.menu.gui.CustomMenuGui`, package `com.ultikits.plugins.menu.gui`)
— the one class Phase 9's GUI-exclusion register removes from this module's JaCoCo `check` gate
(`.planning/phases/09-module-ecosystem-readiness-and-test-coverage/gui-exclusions/UltiMenu.md`),
and per D-19 this module's primary demonstration of the pixel evidence channel: nothing about
"does the menu look right" is answerable at the protocol layer. Every button placed in the GUI
carries its own click handler assembled from the button's own configuration (see the
`menu-definition` `buttons.*` config rows) — permission, price, player/console commands,
close-on-click, and an optional sub-menu link, all inside one `handleButtonClick` method, applied
in that fixed order: debounce → button permission → resolve and authorise the sub-menu (if the
button sets `open-menu`) → economy → player commands → console commands → navigate to the
sub-menu (returning before the close-on-click check) → close-on-click. Two distinct permission
checks appear in that list: the BUTTON's own `permission` key, which gates clicking the button,
and `MenuAccess#allowOpen` against the menu being ENTERED, which gates the navigation. **Every
refusal precedes every irreversible effect**, which is why the sub-menu is resolved and judged
before the charge rather than at the point it is opened: `EconomyUtils.withdraw`, the player
commands and the console commands cannot be undone, and each of the three refusals — button
permission, sub-menu not found, sub-menu not permitted — is decided from state the click does not
change. Before this was corrected, a button with a `price` and an `open-menu` charged for a
navigation it then refused (`UltiKits/UltiMenu#20`). The debounce is deliberately outside that
rule and stays first: consuming it on a refused click is what keeps a refusal from being
spammable.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.menu.button-click | Handle a click on a placed button: a per-GUI-instance click debounce (`click_cooldown_ms`, shared across every button in that open inventory, not per-button), a button-level permission check, then — for a button that sets `open-menu` — resolution of the target menu and the access check on it, and only then a Vault economy charge if `price` > 0, followed by dispatch of the button's configured player commands (as the clicking player) and console commands (as console, on the next tick, with `{player}` substituted first). The sub-menu step sits where it does deliberately: a charge and a command dispatch cannot be undone, so every refusal is decided before them (see this section's preamble) | gui | click a placed button inside an open custom menu | n/a | n/a | player | brief | CustomMenuGui#handleButtonClick |
| ultimenu.menu.render | (`Permission` is `n/a` here deliberately, unlike the three rows that open a menu: rendering happens only after one of those has already authorised the open, so it applies no node of its own.) Render a menu's configured buttons into the inventory at their declared slot positions when the GUI opens, with `{player}` always resolved and PlaceholderAPI placeholders resolved ONLY IF PlaceholderAPI is installed (`plugin.yml` declares it a `softdepend`, not a hard dependency — `CustomMenuGui#parsePlaceholders` catches the missing-class error and leaves any `%...%` token in the text completely unresolved, literal, when it is absent) and `&`-color codes translated in the display name and lore, and custom model data applied if set | gui | open any menu (`/menu <name>`, `/menu open <name>`, or a bound item) | n/a | n/a | player | brief | CustomMenuGui#onOpen |
| ultimenu.menu.submenu-open | A button whose `open-menu` key names another loaded menu closes the current inventory and opens the named menu on the next server tick; the SAME button's own permission, price and player/console commands still apply, in the fixed order this section's preamble states — but the sub-menu is resolved and authorised BEFORE the charge and the commands, so a button that is refused entry costs nothing and runs nothing; only the trailing `close-on-click` branch is skipped in favor of the sub-menu navigation. This is a THIRD menu-access path alongside the command path (`MenuCommands#openMenuByName`) and the bound-item path (`ItemBindListener#tryOpenMenuForItem`), and all three now apply the same rule, `MenuAccess#allowOpen`: `ultikits.menu.use` plus the target menu's own `permission` key when it sets one. The key judged is the one on the SUB-menu being entered, never the parent's — being allowed to see the parent grants nothing about the sub-menu. The check runs before the button's own charge and commands, and therefore also before `closeInventory()`, so a refused navigation costs nothing, runs nothing, and leaves the parent menu open — the same outcome a sub-menu name that does not resolve produces. Until `UltiKits/UltiMenu#15` was fixed this branch checked nothing at all, neither node, and was the weakest of the three | gui | click a button configured with `open-menu` | ultikits.menu.use (+ the SUB-menu's own `permission` key when it sets one) | n/a | player | detailed | CustomMenuGui#handleButtonClick |

## Configuration

### Framework-bound (`@ConfigEntity`/`@ConfigEntry`)

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.config.config.click_cooldown_ms | Global per-GUI-instance click debounce, shared by every button in one open menu (not one debounce timer per button) | config | `config/config.yml: click_cooldown_ms (default: 200, range 50-5000)` | n/a | n/a | admin | brief | CustomMenuGui#handleButtonClick |

### Menu-definition format (runtime-parsed, not annotation-bound)

Every key `MenuServiceImpl#parseMenuFile`/`#parseButton` reads from a menu definition file, at the
`menu-definition` format name explained in the Conventions section above. 18 rows: 7 top-level menu
keys (one of them, `command`, removed in 6.3.0 and read only to report that a file still carries it),
11 per-button keys (a menu's `buttons` map may hold any number of entries, each parsed
identically). Defaults below are this parser's own fallback when a key is absent — NOT necessarily
the shipped `menus/example.yml`'s own values, which are also given per row for the one row where
they differ meaningfully.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.config.menu-definition.bind-item | Item type (Bukkit `Material` name, case-insensitive) that, held in either hand, triggers opening this menu on right-click; an unrecognized material name is silently ignored (parsed as absent, `bindItem` stays `null`, no binding for that menu) | config | `menus/<name>.yml: bind-item (default: unset — no binding)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.bind-lore | A single string that must appear as a substring of at least one (color-stripped) lore line on the held item for the bind to trigger; unset means lore is not checked | config | `menus/<name>.yml: bind-lore (default: unset — lore not checked)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.bind-name | The held item's (color-stripped) display name must exactly equal this value for the bind to trigger; unset means display name is not checked | config | `menus/<name>.yml: bind-name (default: unset — name not checked)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.buttons.close-on-click | Whether clicking this button closes the inventory afterward — ignored when the button also sets `open-menu` AND that target menu exists, since a sub-menu link closes-then-reopens in that case regardless of this key's value; if the target menu does NOT exist, `handleButtonClick` returns after the refusal message without calling `closeInventory()` at all, so the CURRENT inventory stays open and this key's value is moot for a different reason (see `ultimenu.menu.submenu-open.neg-not-found`) | config | `menus/<name>.yml: buttons.<id>.close-on-click (default: true)` | n/a | n/a | admin | detailed | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.console-commands | Console commands dispatched on click, `{player}` replaced with the clicking player's name and PlaceholderAPI-resolved ONLY IF PlaceholderAPI is installed (softdepend, see `ultimenu.menu.render`'s own note — `{player}` always resolves regardless), run via `Bukkit#dispatchCommand` against the console sender on the main thread on the next server tick (not synchronously in the click handler) | config | `menus/<name>.yml: buttons.<id>.console-commands (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.custom-model-data | Custom model data applied to the button's item; `0` (the default) means none is applied — `ItemMeta#setCustomModelData` is only called when this value is greater than zero | config | `menus/<name>.yml: buttons.<id>.custom-model-data (default: 0 — none applied)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.item | The button's item material (Bukkit `Material` name, case-insensitive); required — a button whose `item` is missing, or names an unrecognized material, is skipped entirely (a warning is logged, no button placed for that entry) | config | `menus/<name>.yml: buttons.<id>.item (required, no default)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.lore | The button's item lore lines, `&`-color-coded and placeholder-resolved when rendered — PlaceholderAPI tokens ONLY IF PlaceholderAPI is installed (see `ultimenu.menu.render`'s own note; `{player}` always resolves regardless) | config | `menus/<name>.yml: buttons.<id>.lore (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.name | The button's item display name, `&`-color-coded and placeholder-resolved when rendered (PlaceholderAPI tokens ONLY IF installed, see `ultimenu.menu.render`); if empty or unset, the item's default (material) name shows instead | config | `menus/<name>.yml: buttons.<id>.name (default: unset)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.open-menu | Name of another loaded menu to open (closing the current one) when this button is clicked, instead of running the close-on-click behaviour; a name that does not match any loaded menu produces a refusal message and the current menu stays open | config | `menus/<name>.yml: buttons.<id>.open-menu (default: unset — no sub-menu link)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.permission | Permission node required to click this specific button; unset or empty means any viewer of the menu may click it. This is a different check from the menu-level `permission` key: this one gates clicking a button, that one gates opening a menu. Whichever of the three paths a viewer arrived by, the menu-level key has already been applied to the menu he is looking at (`MenuAccess#allowOpen`), so an unset button-level `permission` now means "anyone who was allowed into this menu may click this button" on every path alike. Before `UltiKits/UltiMenu#15` was fixed that was not true of a viewer who arrived via another menu's `open-menu` button, for whom this menu's own key had never been checked | config | `menus/<name>.yml: buttons.<id>.permission (default: unset — no button-level check)` | n/a | n/a | admin | detailed | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.player-commands | Commands dispatched as the clicking player (`Player#performCommand`), `{player}` replaced with the player's own name and PlaceholderAPI-resolved ONLY IF PlaceholderAPI is installed (see `ultimenu.menu.render`'s own note; `{player}` always resolves regardless), executed synchronously in the click handler (not deferred a tick, unlike `console-commands`) | config | `menus/<name>.yml: buttons.<id>.player-commands (default: empty list)` | n/a | n/a | admin | none | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.position | The button's inventory slot index (0-based, within the menu's total `size` slots); no bounds validation against `size` is performed by the parser itself — Bukkit's own inventory API is what would reject an out-of-range slot | config | `menus/<name>.yml: buttons.<id>.position (default: 0)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.buttons.price | Vault currency amount withdrawn from the clicking player before running the button's commands; `0` (the default) skips the economy check entirely — no Vault dependency, no balance check, for a free button | config | `menus/<name>.yml: buttons.<id>.price (default: 0.0 — no charge)` | n/a | n/a | admin | brief | MenuServiceImpl#parseButton |
| ultimenu.config.menu-definition.command | Removed in 6.3.0 (`UltiKits/UltiMenu#12`). The key used to be parsed and stored on the menu definition and read by nothing — no command was ever registered from it, so a value such as the old shipped example's `servermenu` never created a `/servermenu` command. It is no longer parsed into the menu definition, and the shipped `menus/example.yml` no longer sets it. A menu opens only through `/menu <name>`, `/menu open <name>`, a matching bound item, or another menu's `open-menu` button (see `ultimenu.menu.submenu-open`); a menu's own slash command is requested as `UltiKits/UltiMenu#23`. The parser still reads the key for one purpose: when a menu file carries it at the top level with a value, every load of that file — each start and each `/menu reload`, before the file's own validation, so also for a file that then fails to load — logs one WARNING — under `language: en` it contains `UltiMenu: 'command' in <that file's path> no longer has any effect and can be deleted from the file` (the line comes from the language catalogue, so under `language: zh` it is Chinese, still naming `'command'` and the path) — which also names where a menu is opened instead and cites `UltiKits/UltiMenu#23` and `UltiKits/UltiMenu#12`. One line per file that carries the key; nothing for a file that does not. It does not fire on `/ul reload`: that runs the framework's own reload, which never re-parses menu files, so only a start or `/menu reload` shows it. That trigger is why this report is catalogued on the key's own row rather than as a `lifecycle.removed-key-warning` row like the other modules', whose checks do run on `/ul reload`. Not seen: a key written with no value or a YAML null (`command:`, `command: null`, `command: ~`), which Bukkit's loader drops and which always read back as absent; and a `command` key nested under a button, which is not this key | config | `menus/<name>.yml: command (removed — no effect; a file still carrying it is reported at every load)` | n/a | n/a | admin | detailed | MenuServiceImpl#warnIfRemovedKeysPresent |
| ultimenu.config.menu-definition.permission | Permission required to open this menu. Read here by `MenuServiceImpl#parseMenuFile`; applied by `MenuAccess#allowOpen`, which is the single rule all three open paths call before the GUI is constructed — the command paths (`MenuCommands#openMenuByName`), the bound-item path (`ItemBindListener#tryOpenMenuForItem`), and a parent menu's `open-menu` button (`CustomMenuGui#handleButtonClick`). That rule is `ultikits.menu.use` AND this key when it is set, so an unset or empty key means "any player holding `ultikits.menu.use`", never "any player at all". Before `UltiKits/UltiMenu#14` and `UltiKits/UltiMenu#15` were fixed the three paths disagreed: only the command path required `ultikits.menu.use` (through `MenuCommands`'s class-level `@CmdExecutor`, which gates that class and nothing else), the bound-item path checked this key alone, and the `open-menu` path checked neither | config | `menus/<name>.yml: permission (default: unset — no per-menu check beyond the base node, on any path)` | n/a | n/a | admin | detailed | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.size | Inventory row count × 9, validated to be 9-54 and an exact multiple of 9; a file with an invalid size is rejected entirely (logged warning, the whole menu fails to load — no partial load) | config | `menus/<name>.yml: size (default: 27, valid range 9-54, multiple of 9)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |
| ultimenu.config.menu-definition.title | The menu's inventory title, `&`-color-coded and `{player}`-resolved before display; a PlaceholderAPI token resolves ONLY IF PlaceholderAPI is installed (see `ultimenu.menu.render`'s own note) | config | `menus/<name>.yml: title (default: the language catalogue's default title — `Menu` under `language: en`, `菜单` under `language: zh`)` | n/a | n/a | admin | brief | MenuServiceImpl#parseMenuFile |

## Language

Every chat line, the command description, the menu service's display name, the default menu title
and every console line this module writes goes through the framework's language catalogue, so it
follows the framework-wide `language` setting (`plugins/UltiTools/config.yml`). Keys are ASCII
(`menu.list.header`); two JUnit guards (`UltiMenuLanguageCatalogueTest`,
`UltiMenuCjkLiteralScopeTest`) fail the build when a key is missing from either catalogue or Chinese
text appears outside one. Text an operator writes into `menus/*.yml` (titles, button names, lore) is
the operator's own content and is shown as written.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultimenu.i18n.language | All of this module's chat, command-description and console text in the server's language: `lang/en.json` under `language: en`, `lang/zh.json` under `language: zh` | config | framework `config.yml: language` | n/a | both | admin | none | `lang/en.json`, `lang/zh.json`, every `i18n(...)` call |
