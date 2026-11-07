package com.ultikits.plugins.menu.services;

import com.ultikits.plugins.menu.MenuAccess;
import com.ultikits.plugins.menu.commands.MenuCommands;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Owns per-file Bukkit command instances. All calls follow the synchronous module lifecycle. */
final class MenuCommandRegistry {
    private final UltiToolsPlugin plugin;
    private final MenuCommands opener;
    private final List<MenuCommand> registered = new ArrayList<>();
    private CommandMap commandMap;

    MenuCommandRegistry(UltiToolsPlugin plugin, MenuService service) {
        this.plugin = plugin;
        this.opener = new MenuCommands(plugin, service);
    }

    void replace(Map<String, MenuDefinition> definitions) {
        boolean changed = clear();
        Map<String, List<String>> filesByCommand = new TreeMap<>();
        for (Map.Entry<String, MenuDefinition> entry : definitions.entrySet()) {
            String label = entry.getValue().getOpenCommand();
            if (label != null) {
                filesByCommand.computeIfAbsent(label, ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }
        for (Map.Entry<String, List<String>> entry : filesByCommand.entrySet()) {
            String label = entry.getKey();
            String files = String.join(", ", entry.getValue());
            if (entry.getValue().size() > 1) {
                warn(label, files, files);
                continue;
            }
            if ("menu".equals(label)) {
                warn(label, files, "UltiMenu /menu");
                continue;
            }
            if (commandMap == null) commandMap = Bukkit.getCommandMap();
            Command existing = commandMap.getCommand(label);
            if (existing == null) existing = commandMap.getCommand("ultimenu:" + label);
            if (existing != null) {
                warn(label, files, owner(existing));
                continue;
            }
            MenuCommand command = new MenuCommand(label, definitions.get(entry.getValue().get(0)).getFileName());
            commandMap.register("ultimenu", command);
            registered.add(command);
            changed = true;
        }
        if (changed) refreshPlayers();
    }

    private String owner(Command command) {
        if (command instanceof PluginCommand) {
            PluginCommand pluginCommand = (PluginCommand) command;
            return pluginCommand.getPlugin().getName() + " /" + command.getName()
                    + " (" + pluginCommand.getExecutor().getClass().getName() + ")";
        }
        return command.getClass().getName() + " /" + command.getName();
    }

    private void warn(String label, String file, String owner) {
        plugin.getLogger().warn(String.format(plugin.i18n("menu.log.command_collision"), label, file, owner));
    }

    void close() {
        if (clear()) refreshPlayers();
    }

    private boolean clear() {
        if (registered.isEmpty()) return false;
        for (MenuCommand command : registered) {
            command.active = false;
            // unregister alone does not remove the bare/fallback/alias entries from Bukkit's map.
            commandMap.getKnownCommands().values().removeIf(known -> known == command);
            command.unregister(commandMap);
        }
        registered.clear();
        return true;
    }

    private void refreshPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) player.updateCommands();
    }

    private final class MenuCommand extends Command {
        private final String menuName;
        private boolean active = true;

        MenuCommand(String label, String menuName) {
            super(label, plugin.i18n("menu.command.description"), "/" + label, Collections.emptyList());
            this.menuName = menuName;
            setPermission(MenuAccess.BASE_PERMISSION);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] arguments) {
            if (!active) return true;
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + plugin.i18n("menu.error.player_only"));
            } else if (arguments.length != 0) {
                sender.sendMessage(ChatColor.RED + String.format(plugin.i18n("menu.command.usage"), label));
            } else {
                opener.onOpen((Player) sender, menuName);
            }
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] arguments) {
            return Collections.emptyList();
        }
    }
}
