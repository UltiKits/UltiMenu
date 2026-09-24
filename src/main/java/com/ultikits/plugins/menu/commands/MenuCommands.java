package com.ultikits.plugins.menu.commands;

import com.ultikits.plugins.menu.MenuAccess;
import com.ultikits.plugins.menu.gui.CustomMenuGui;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.plugins.menu.services.MenuService;
import com.ultikits.ultitools.abstracts.command.BaseCommandExecutor;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.annotations.command.CmdMapping;
import com.ultikits.ultitools.annotations.command.CmdParam;
import com.ultikits.ultitools.annotations.command.CmdSender;
import com.ultikits.ultitools.annotations.command.CmdTarget;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * 菜单命令处理器
 * <p>
 * Menu command executor for UltiMenu plugin
 */
@CmdExecutor(
        permission = "ultikits.menu.use",
        description = "menu.command.description",
        alias = {"menu"}
)
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
public class MenuCommands extends BaseCommandExecutor {

    private final UltiToolsPlugin plugin;
    private final MenuService menuService;

    public MenuCommands(UltiToolsPlugin plugin, MenuService menuService) {
        this.plugin = plugin;
        this.menuService = menuService;
    }

    /**
     * 快捷打开菜单: /menu <name>
     * <p>
     * Shorthand to open menu directly.
     * No @CmdTarget(PLAYER) here — the wildcard pattern would intercept
     * "list"/"reload" before the framework tries the exact-match mappings,
     * rejecting console senders. Instead we accept BOTH and check manually.
     */
    @CmdMapping(format = "<name>")
    public void onQuickOpen(
            @CmdSender CommandSender sender,
            @CmdParam(value = "name", suggest = "suggestMenuNames") String name
    ) {
        // Prevent conflict with list/reload subcommands
        if ("list".equalsIgnoreCase(name) || "reload".equalsIgnoreCase(name)
                || "open".equalsIgnoreCase(name) || "help".equalsIgnoreCase(name)) {
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + plugin.i18n("menu.error.player_only"));
            return;
        }
        openMenuByName((Player) sender, name);
    }

    /**
     * 显式打开菜单: /menu open <name>
     * <p>
     * Explicitly open a menu
     */
    @CmdMapping(format = "open <name>")
    @CmdTarget(CmdTarget.CmdTargetType.PLAYER)
    public void onOpen(
            @CmdSender Player player,
            @CmdParam(value = "name", suggest = "suggestMenuNames") String name
    ) {
        openMenuByName(player, name);
    }

    /**
     * 列出所有菜单: /menu list
     * <p>
     * List all available menus
     */
    @CmdMapping(format = "list")
    public void onList(@CmdSender CommandSender sender) {
        Collection<MenuDefinition> menus = menuService.getAllMenus();

        if (menus.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + plugin.i18n("menu.list.empty"));
            return;
        }

        sender.sendMessage(ChatColor.GOLD + plugin.i18n("menu.list.header"));
        for (MenuDefinition menu : menus) {
            String title = ChatColor.translateAlternateColorCodes('&', menu.getTitle());
            sender.sendMessage(ChatColor.AQUA + menu.getFileName() + ChatColor.WHITE + " - " + title);
        }
    }

    /**
     * 重新加载所有菜单: /menu reload
     * <p>
     * Reload all menu configurations
     */
    @CmdMapping(format = "reload")
    public void onReload(@CmdSender CommandSender sender) {
        if (!sender.hasPermission("ultikits.menu.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.i18n("menu.error.no_permission_command"));
            return;
        }

        menuService.reload();
        int count = menuService.getAllMenus().size();
        sender.sendMessage(ChatColor.GREEN + String.format(plugin.i18n("menu.reload.done"), count));
    }

    /**
     * 帮助信息: /menu 或 /menu help
     * <p>
     * Display help message
     */
    @Override
    protected void handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + plugin.i18n("menu.help.header"));
        sender.sendMessage(ChatColor.AQUA + "/menu <name>" + ChatColor.WHITE + " - " + plugin.i18n("menu.help.quick_open"));
        sender.sendMessage(ChatColor.AQUA + "/menu open <name>" + ChatColor.WHITE + " - " + plugin.i18n("menu.help.open"));
        sender.sendMessage(ChatColor.AQUA + "/menu list" + ChatColor.WHITE + " - " + plugin.i18n("menu.help.list"));
        sender.sendMessage(ChatColor.AQUA + "/menu reload" + ChatColor.WHITE + " - " + plugin.i18n("menu.help.reload"));
    }

    /**
     * 打开菜单的核心逻辑
     * <p>
     * Core logic for opening a menu. Access is decided by {@link MenuAccess#allowOpen}, the one
     * rule every menu-open path in this module shares, rather than by a check written out here —
     * three copies of the rule are what let the three paths disagree in the first place
     * (UltiKits/UltiMenu#14, UltiKits/UltiMenu#15). Routing this path through it re-checks
     * {@code ultikits.menu.use} that the framework's class-level {@code @CmdExecutor} already
     * enforces for this class, deliberately: that gate protects this class only, and a rule that
     * lives in one caller cannot be the module's rule.
     */
    private void openMenuByName(Player player, String name) {
        MenuDefinition menu = menuService.getMenu(name);

        if (menu == null) {
            player.sendMessage(ChatColor.RED + String.format(plugin.i18n("menu.error.not_found"), name));
            return;
        }

        if (!MenuAccess.allowOpen(plugin, player, menu)) {
            return;
        }

        new CustomMenuGui(player, plugin, menu, menuService).open();
    }

    /**
     * Tab 补全建议 - 菜单名称列表
     * <p>
     * Tab completion suggestions for menu names
     */
    private List<String> suggestMenuNames(Player player) {
        return menuService.getMenuNames();
    }
}
