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
