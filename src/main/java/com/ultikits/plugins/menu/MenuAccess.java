package com.ultikits.plugins.menu;

import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * The single rule deciding whether a player may open a menu, shared by every path that opens one.
 * <p>
 * A player may open a menu when BOTH hold:
 * <ol>
 *   <li>he holds {@code ultikits.menu.use}, this module's base access node; and</li>
 *   <li>the menu declares no {@code permission} key (absent or empty), or he holds the node it
 *       declares.</li>
 * </ol>
 * <p>
 * There are three ways into a menu — the {@code /menu}/{@code /menu open} command
 * ({@code MenuCommands#openMenuByName}), a bound item
 * ({@code ItemBindListener#tryOpenMenuForItem}), and a button's {@code open-menu} key
 * ({@code CustomMenuGui#handleButtonClick}) — and before this class each of the three carried its
 * own idea of the rule. The command path required the base node (through the framework's
 * class-level {@code @CmdExecutor(permission = ...)}) and the menu's own key; the bound-item path
 * required only the menu's own key, so a menu with no {@code permission} set was open to a player
 * holding nothing at all (UltiKits/UltiMenu#14); the sub-menu path required neither, so a
 * {@code permission}-gated menu was reachable by clicking a button that linked to it
 * (UltiKits/UltiMenu#15).
 * <p>
 * Keeping the rule in one place is the fix for both. Three copies of a rule can differ; one
 * cannot.
 *
 * @author wisdomme
 * @since 1.0.0
 */
public final class MenuAccess {

    /**
     * This module's base access node — the same node {@code MenuCommands} declares on its
     * class-level {@code @CmdExecutor}, required here by every open path rather than only by the
     * one the framework happens to gate.
     */
    public static final String BASE_PERMISSION = "ultikits.menu.use";

    private MenuAccess() {
    }

    /**
     * Decides whether {@code player} may open {@code menu}, and tells him he may not when the
     * answer is no.
     * <p>
     * The refusal message is sent from here, in the same place the decision is made, so no caller
     * can decide correctly and then refuse nothing.
     *
     * @param plugin the module instance, for {@code i18n} lookup of the refusal message
     * @param player the player trying to open the menu
     * @param menu   the menu being opened; never null — every caller resolves it from
     *               {@code MenuService} and handles a missing menu before reaching this method.
     *               A null is a programming error at a new call site and is rejected with a
     *               message saying so, rather than silently refused: refusing would look to the
     *               operator exactly like a permission problem, and the throw is fail-closed
     *               anyway, since this method then returns nothing and no menu opens
     * @return true when the menu may be opened; false when it may not, in which case the player
     *         has already been told
     * @throws NullPointerException if {@code menu} is null
     */
    public static boolean allowOpen(UltiToolsPlugin plugin, Player player, MenuDefinition menu) {
        Objects.requireNonNull(menu, "menu must not be null");

        if (!player.hasPermission(BASE_PERMISSION)) {
            deny(plugin, player);
            return false;
        }

        String permission = menu.getPermission();
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            deny(plugin, player);
            return false;
        }

        return true;
    }

    private static void deny(UltiToolsPlugin plugin, Player player) {
        player.sendMessage(ChatColor.RED + plugin.i18n("menu.error.no_permission_open"));
    }
}
