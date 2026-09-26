# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Changed

- Language keys were renamed from Chinese sentences to ASCII keys (for example `menu.list.header`).
  An operator who edited this module's `lang/en.json` or `lang/zh.json` must re-apply those edits to
  the new keys; until then the renamed messages show the new built-in text. A server whose language
  files were never edited needs no action.
- 语言键已从中文句子改为 ASCII 键（例如 `menu.list.header`）。改过本模块 `lang/en.json` 或
  `lang/zh.json` 的运维需要把改动重新套到新键上；在此之前，这些消息显示新的内置文本。从未改过语言文件的服务器无需任何操作。

### Removed

- The per-menu `command` key never took effect and has been removed; it can be deleted from existing
  menu files. A menu definition file could set a top-level `command` key, and the shipped
  `menus/example.yml` set `command: servermenu`, but no command was ever registered from it, so
  `/servermenu` never existed. Menus open exactly as before: `/menu <name>`, `/menu open <name>`, a
  matching bound item, or another menu's `open-menu` button. The shipped example no longer sets the
  key. Removing the setting is not a rejection of the feature: a menu's own slash command is
  requested as UltiKits/UltiMenu#23 (UltiKits/UltiMenu#12).
- A menu file that still sets `command` now logs one warning each time it is loaded — at every start
  and every `/menu reload` — naming the module, the file and the key. Removing a key from the code does
  not remove it from anybody's file, so without this an operator who had set it would see no trace of
  the removal. A server first started on an earlier version still has the old example's
  `command: servermenu` line in `menus/example.yml` and will see this warning for that file until the
  line is deleted (UltiKits/UltiMenu#12).
- 每个菜单的 `command` 键从未生效，现已移除；可从现有菜单文件中删除。菜单定义文件可以设置顶层 `command` 键，
  随附的 `menus/example.yml` 也设置了 `command: servermenu`，但本模块从未根据它注册任何命令，因此 `/servermenu`
  从未存在。打开菜单的方式与以前完全相同：`/menu <名称>`、`/menu open <名称>`、匹配的绑定物品，或另一个菜单按钮的
  `open-menu`。随附示例不再设置该键。移除该设置并不代表否决这项功能：为菜单单独绑定斜杠命令的需求已记录为
  UltiKits/UltiMenu#23（UltiKits/UltiMenu#12）。
- 仍设置 `command` 的菜单文件现在每次加载时（每次启动以及每次 `/menu reload`）都会输出一条警告，点名模块、文件与
  该键。从代码中移除一个键并不会把它从任何人的文件中移除，若没有这条警告，设置过该键的服主将看不到任何移除的痕迹。
  在更早版本上首次启动的服务器，其 `menus/example.yml` 中仍保留旧示例的 `command: servermenu` 一行，在删除该行
  之前会一直看到针对该文件的这条警告（UltiKits/UltiMenu#12）。

### Fixed

- `language: en` now applies to the `/menu` command description and to the refusal a console sender
  gets from `/menu <name>`, which showed a Chinese sentence in every language because neither had an
  English entry (UltiKits/UltiMenu#11); and to the five `/menu help` lines and the menu service's
  display name, which were fixed Chinese text. `language: zh` now also applies to the title a menu
  file without `title` gets (`Menu`) and to the console lines written while menu files load, which
  were fixed English text: an invalid menu size, a button with no item or an unknown material, a
  failed copy of the example menu, and the warning about a leftover `command` key. Their English wording is
  unchanged; under `language: zh` they are Chinese, except the file name, path and offending value,
  which are printed as written.
- `language: en` 现在对 `/menu` 命令描述、以及控制台执行 `/menu <name>` 时收到的拒绝提示生效（二者都没有英文条目，
  因此任何语言下都显示中文句子，UltiKits/UltiMenu#11）；也对 `/menu help` 的五行帮助与菜单服务的显示名称（原先写死为中文）生效。
  `language: zh` 现在也对未写 `title` 的菜单文件所得的默认标题（`Menu`）以及加载菜单文件时写出的控制台日志生效（原先写死为英文）：
  菜单大小无效、按钮未指定物品或物品材质无效、复制示例菜单失败，以及残留 `command` 键的警告。
  英文措辞不变；`language: zh` 下为中文，只有文件名、路径与出错的取值按原样打印。

- Uninstalling this module with `/upm uninstall UltiTools-Menu` now really removes its commands
  (`/menu`) and stops its bound-item listener from firing; this module has no unload work of its
  own. Previously this module's empty unload method replaced the framework's, so both stayed active
  until the server restarted (UltiKits/UltiMenu#16).
- 使用 `/upm uninstall UltiTools-Menu` 卸载本模块后，其命令（`/menu`）现在会被真正移除，其绑定物品监听器
  也不再触发；本模块自身没有卸载工作。此前本模块的空卸载方法替换了框架的卸载方法，因此两者都会一直生效到
  服务器重启（UltiKits/UltiMenu#16）。
- A button carrying both a `price` and an `open-menu` key no longer charges the player when the
  navigation is then refused. The sub-menu is now resolved and checked before the charge and before
  the button's player and console commands, so a button leading to a menu that does not exist — or
  that the player may not enter — costs nothing and runs nothing. Previously the charge, the
  `Deducted …` confirmation and the commands all happened first and the refusal came afterwards, with
  no refund, repeatable on every click (UltiKits/UltiMenu#20).
- 同时设置了 `price` 与 `open-menu` 的按钮，在导航被拒绝时不再扣费。子菜单现在会在扣费以及执行该按钮的玩家
  命令和控制台命令之前完成解析与权限校验，因此指向不存在或玩家无权进入的菜单的按钮既不扣费也不执行任何命令。
  此前扣费、“已扣除”提示和命令都会先发生，拒绝提示随后出现，且不退款，每次点击都会重复
  （UltiKits/UltiMenu#20）。

### Security

- Opening a menu now requires `ultikits.menu.use` on every path, and a menu's own `permission`
  key is honoured on every path. Two paths did not enforce this: a bound item opened a menu that
  set no `permission` key for any player holding a matching item, including one denied
  `ultikits.menu.use` (UltiKits/UltiMenu#14), and a button's `open-menu` key opened the menu it
  named with no check of any kind, so a menu's own `permission` key did not restrict who could
  reach it from a parent menu (UltiKits/UltiMenu#15). **Operators who relied on permission-free
  bound items must now grant `ultikits.menu.use` to the players who use them.** Until it is granted,
  a matching bound item is swallowed on right-click — it performs no ordinary use — and its holder is
  told he lacks permission every time, which is the form this arrives in as a support question. The
  node is not declared in `plugin.yml`, so it defaults to operators only.
- 现在每条打开菜单的路径都要求 `ultikits.menu.use`，并且每条路径都会检查菜单自身的 `permission` 键。此前
  有两条路径未作检查：绑定物品会为任何持有匹配物品的玩家打开未设置 `permission` 键的菜单，即使该玩家被明确
  拒绝了 `ultikits.menu.use`（UltiKits/UltiMenu#14）；按钮的 `open-menu` 键打开其所指菜单时不做任何检查，
  因此菜单自身的 `permission` 键无法限制谁能从父菜单进入（UltiKits/UltiMenu#15）。**此前依赖免权限绑定
  物品的服主，现在必须为使用这些物品的玩家授予 `ultikits.menu.use`。** 在授予之前，匹配的绑定物品右键会被
  拦截（不会产生其原本的使用效果），持有者每次右键都会收到无权限提示——这通常就是此问题被反馈时的表现。该
  权限节点未在 `plugin.yml` 中声明，因此默认只有管理员拥有。
