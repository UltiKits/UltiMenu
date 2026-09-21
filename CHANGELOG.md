# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- Uninstalling this module with `/upm uninstall UltiTools-Menu` now really removes its commands
  (`/menu`) and stops its bound-item listener from firing; this module has no unload work of its
  own. Previously this module's empty unload method replaced the framework's, so both stayed active
  until the server restarted (UltiKits/UltiMenu#16).
- 使用 `/upm uninstall UltiTools-Menu` 卸载本模块后，其命令（`/menu`）现在会被真正移除，其绑定物品监听器
  也不再触发；本模块自身没有卸载工作。此前本模块的空卸载方法替换了框架的卸载方法，因此两者都会一直生效到
  服务器重启（UltiKits/UltiMenu#16）。

### Security

- Opening a menu now requires `ultikits.menu.use` on every path, and a menu's own `permission`
  key is honoured on every path. Two paths did not enforce this: a bound item opened a menu that
  set no `permission` key for any player holding a matching item, including one denied
  `ultikits.menu.use` (UltiKits/UltiMenu#14), and a button's `open-menu` key opened the menu it
  named with no check of any kind, so a menu's own `permission` key did not restrict who could
  reach it from a parent menu (UltiKits/UltiMenu#15). **Operators who relied on permission-free
  bound items must now grant `ultikits.menu.use` to the players who use them.**
- 现在每条打开菜单的路径都要求 `ultikits.menu.use`，并且每条路径都会检查菜单自身的 `permission` 键。此前
  有两条路径未作检查：绑定物品会为任何持有匹配物品的玩家打开未设置 `permission` 键的菜单，即使该玩家被明确
  拒绝了 `ultikits.menu.use`（UltiKits/UltiMenu#14）；按钮的 `open-menu` 键打开其所指菜单时不做任何检查，
  因此菜单自身的 `permission` 键无法限制谁能从父菜单进入（UltiKits/UltiMenu#15）。**此前依赖免权限绑定
  物品的服主，现在必须为使用这些物品的玩家授予 `ultikits.menu.use`。**
