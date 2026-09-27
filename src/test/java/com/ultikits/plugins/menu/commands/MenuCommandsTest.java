package com.ultikits.plugins.menu.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.*;

import com.ultikits.plugins.menu.MenuAccess;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.plugins.menu.services.MenuService;
import com.ultikits.plugins.menu.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("MenuCommands Tests")
class MenuCommandsTest {

    private UltiToolsPlugin mockPlugin;
    private MenuService mockMenuService;
    private MenuCommands commands;

    @BeforeEach
    void setUp() {
        mockPlugin = mock(UltiToolsPlugin.class);
        mockMenuService = mock(MenuService.class);
        when(mockPlugin.i18n(anyString())).thenAnswer(CatalogueText.answer("zh"));

        commands = new MenuCommands(mockPlugin, mockMenuService);
    }

    /**
     * Captures all sendMessage(String) calls and asserts at least one contains the substring.
     */
    /**
     * The base permission node, written out as the literal operators actually configure rather
     * than read from the production constant: a test that reads the constant would keep passing
     * if the node were renamed, which is precisely the regression an operator would feel.
     */
    private static final String BASE_NODE = "ultikits.menu.use";

    private void assertSentMessageContaining(CommandSender sender, String substring) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues())
            .anyMatch(msg -> msg.contains(substring));
    }

    /**
     * Asserts that no sendMessage call contains the substring.
     * <p>
     * Callers pass the SHORT {@code 没有权限} here on purpose, the opposite choice from the
     * refusal assertions: a test proving a menu opened wants to exclude every refusal this module
     * can emit, not just the one it expected not to see.
     */
    private void assertNoMessageContaining(CommandSender sender, String substring) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        // Use atLeast(0) to not fail if there were no calls
        verify(sender, atLeast(0)).sendMessage(captor.capture());
        assertThat(captor.getAllValues())
            .noneMatch(msg -> msg.contains(substring));
    }

    // ==================== Quick Open Tests ====================

    @Nested
    @DisplayName("Quick Open Tests (/menu <name>)")
    class QuickOpenTests {

        @Test
        @DisplayName("Should skip 'list' as quick-open name")
        void shouldSkipListName() {
            Player player = mock(Player.class);

            commands.onQuickOpen(player, "list");

            verify(mockMenuService, never()).getMenu(anyString());
        }

        @Test
        @DisplayName("Should skip 'reload' as quick-open name")
        void shouldSkipReloadName() {
            Player player = mock(Player.class);

            commands.onQuickOpen(player, "reload");

            verify(mockMenuService, never()).getMenu(anyString());
        }

        @Test
        @DisplayName("Should skip 'LIST' case-insensitively")
        void shouldSkipListCaseInsensitive() {
            Player player = mock(Player.class);

            commands.onQuickOpen(player, "LIST");

            verify(mockMenuService, never()).getMenu(anyString());
        }

        @Test
        @DisplayName("Should send error for nonexistent menu")
        void shouldSendErrorForMissing() {
            Player player = mock(Player.class);
            when(mockMenuService.getMenu("nonexistent")).thenReturn(null);

            commands.onQuickOpen(player, "nonexistent");

            assertSentMessageContaining(player, "不存在");
        }
    }

    // ==================== Open Tests ====================

    @Nested
    @DisplayName("Open Tests (/menu open <name>)")
    class OpenTests {

        @Test
        @DisplayName("Should send error for nonexistent menu")
        void shouldSendErrorForMissing() {
            Player player = mock(Player.class);
            when(mockMenuService.getMenu("missing")).thenReturn(null);

            commands.onOpen(player, "missing");

            assertSentMessageContaining(player, "不存在");
        }

        @Test
        @DisplayName("Should check menu permission before opening")
        void shouldCheckPermission() {
            Player player = mock(Player.class);
            MenuDefinition menu = new MenuDefinition();
            menu.setPermission("vip.menu");
            when(mockMenuService.getMenu("vip")).thenReturn(menu);
            // The base node is granted so this test isolates the menu's OWN permission key: without
            // it the refusal below would also be produced by the base-node branch, and the test
            // would keep passing if the menu-permission branch were deleted.
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(false);

            commands.onOpen(player, "vip");

            assertSentMessageContaining(player, "打开此菜单");
        }
    }

    // ==================== List Tests ====================

    @Nested
    @DisplayName("List Tests (/menu list)")
    class ListTests {

        @Test
        @DisplayName("Should show empty message when no menus")
        void shouldShowEmptyMessage() {
            CommandSender sender = mock(CommandSender.class);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.emptyList());

            commands.onList(sender);

            assertSentMessageContaining(sender, "没有可用的菜单");
        }

        @Test
        @DisplayName("Should list all available menus")
        void shouldListAllMenus() {
            CommandSender sender = mock(CommandSender.class);
            MenuDefinition menu1 = new MenuDefinition();
            menu1.setFileName("spawn");
            menu1.setTitle("&6Spawn Menu");
            MenuDefinition menu2 = new MenuDefinition();
            menu2.setFileName("shop");
            menu2.setTitle("&aShop");

            when(mockMenuService.getAllMenus()).thenReturn(Arrays.asList(menu1, menu2));

            commands.onList(sender);

            // Header + 2 menu lines = 3 messages
            verify(sender, times(3)).sendMessage(anyString());
        }
    }

    // ==================== Reload Tests ====================

    @Nested
    @DisplayName("Reload Tests (/menu reload)")
    class ReloadTests {

        @Test
        @DisplayName("Should require admin permission for reload")
        void shouldRequireAdminPermission() {
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ultikits.menu.admin")).thenReturn(false);

            commands.onReload(sender);

            verify(mockMenuService, never()).reload();
            assertSentMessageContaining(sender, "执行此命令");
        }

        @Test
        @DisplayName("Should reload and report count when has permission")
        void shouldReloadWhenPermitted() {
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ultikits.menu.admin")).thenReturn(true);

            MenuDefinition m1 = new MenuDefinition();
            MenuDefinition m2 = new MenuDefinition();
            when(mockMenuService.getAllMenus()).thenReturn(Arrays.asList(m1, m2));

            commands.onReload(sender);

            verify(mockMenuService).reload();
            assertSentMessageContaining(sender, "2");
        }
    }

    // ==================== Help Tests ====================

    @Nested
    @DisplayName("Help Tests")
    class HelpTests {

        @Test
        @DisplayName("Should display help message")
        void shouldDisplayHelp() {
            CommandSender sender = mock(CommandSender.class);

            commands.handleHelp(sender);

            // At least 4 help lines (header + 4 commands)
            verify(sender, atLeast(4)).sendMessage(anyString());
        }
    }

    // ==================== Tab Completion Tests ====================

    @Nested
    @DisplayName("Tab Completion Tests")
    class TabCompletionTests {

        @Test
        @DisplayName("Should return menu names for tab completion")
        void shouldReturnMenuNames() throws Exception {
            Player player = mock(Player.class);
            when(mockMenuService.getMenuNames()).thenReturn(Arrays.asList("spawn", "shop", "rules"));

            // Access private method via reflection
            Method method = MenuCommands.class.getDeclaredMethod("suggestMenuNames", Player.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(commands, player);

            assertThat(result).containsExactly("spawn", "shop", "rules");
        }
    }

    // ==================== Menu Access Tests ====================

    /**
     * The command path's half of the one shared menu-access rule (UltiKits/UltiMenu#14,
     * UltiKits/UltiMenu#15): a player may open a menu only when he holds
     * {@code ultikits.menu.use} AND the menu either declares no {@code permission} key or he
     * holds that too.
     * <p>
     * The command path is the one path that was already gated on {@code ultikits.menu.use}, but
     * only by the framework, through {@code MenuCommands}'s class-level
     * {@code @CmdExecutor(permission = ...)} — a gate that lives outside this class and that
     * neither of the other two open paths goes through. Routing this path through the same check
     * as the other two is what stops the three from drifting apart again, and it is what these
     * tests pin: they call {@code onOpen}/{@code onQuickOpen} directly, exactly as a caller that
     * has bypassed the framework gate would.
     * <p>
     * <b>How "the menu opened" is observed.</b> Reaching {@code CustomMenuGui#open()} throws
     * {@code NullPointerException("Inventory API is not initialized...")} from obliviate-invs,
     * which has no live {@code InventoryAPI} in a unit test. That specific message is asserted
     * rather than the bare exception type, so an unrelated {@code NullPointerException} (a
     * missing stub, say) cannot masquerade as a successful open. A refusal is therefore
     * observable as the absence of that throw PLUS the refusal message — never as absence alone.
     */
    @Nested
    @DisplayName("Menu Access Tests (ultikits.menu.use + the menu's own permission)")
    class MenuAccessTests {

        private MenuDefinition givenLoadedMenu(String name, String permission) {
            MenuDefinition menu = new MenuDefinition();
            menu.setFileName(name);
            menu.setTitle("Test Menu");
            menu.setSize(27);
            menu.setPermission(permission);
            when(mockMenuService.getMenu(name)).thenReturn(menu);
            return menu;
        }

        private Player givenPlayer() {
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("TestPlayer");
            return player;
        }

        private void assertMenuOpened(Runnable invocation) {
            assertThatThrownBy(invocation::run)
                .as("reaching CustomMenuGui#open() is what proves the menu was opened")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Inventory API is not initialized");
        }

        private void assertMenuRefused(Runnable invocation, Player player) {
            assertThatCode(invocation::run)
                .as("a refused menu must never reach CustomMenuGui#open()")
                .doesNotThrowAnyException();
            assertSentMessageContaining(player, "打开此菜单");
        }

        @Test
        @DisplayName("Should refuse when the player lacks ultikits.menu.use, even for a menu with no permission key")
        void shouldRefuseWithoutBaseNode() {
            givenLoadedMenu("open-to-all", null);
            Player player = givenPlayer();
            when(player.hasPermission(BASE_NODE)).thenReturn(false);

            assertMenuRefused(() -> commands.onOpen(player, "open-to-all"), player);
        }

        @Test
        @DisplayName("Should refuse when the player holds ultikits.menu.use but not the menu's own permission")
        void shouldRefuseWithoutMenuNode() {
            givenLoadedMenu("vip", "vip.menu");
            Player player = givenPlayer();
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(false);

            assertMenuRefused(() -> commands.onOpen(player, "vip"), player);
        }

        @Test
        @DisplayName("Should open when the player holds both ultikits.menu.use and the menu's own permission")
        void shouldOpenWithBothNodes() {
            givenLoadedMenu("vip", "vip.menu");
            Player player = givenPlayer();
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(true);

            assertMenuOpened(() -> commands.onOpen(player, "vip"));
            assertNoMessageContaining(player, "没有权限");
        }

        @Test
        @DisplayName("Should treat an empty permission key as no per-menu requirement")
        void shouldOpenWhenMenuPermissionIsEmpty() {
            givenLoadedMenu("open-to-all", "");
            Player player = givenPlayer();
            when(player.hasPermission(BASE_NODE)).thenReturn(true);

            assertMenuOpened(() -> commands.onOpen(player, "open-to-all"));
            assertNoMessageContaining(player, "没有权限");
        }

        @Test
        @DisplayName("Should reject a null menu loudly rather than deciding anything about it")
        void shouldRejectNullMenu() {
            // MenuAccess is public static so that a future call site can use it, and a future
            // call site is exactly the one that can forget the null check its three current
            // callers all perform. Failing here is fail-closed either way — an exception means
            // allowOpen never returns true, so nothing opens — but it should also say what went
            // wrong rather than look like a permission problem.
            Player player = mock(Player.class);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);

            // The message is asserted exactly, and the exact text matters. Without a contract
            // check the JVM already throws NullPointerException here — measured:
            // `Cannot invoke "…MenuDefinition.getPermission()" because "menu" is null` — so an
            // assertion on the type, or on any substring that message happens to contain
            // ("menu" included), passes against code that has no contract at all. Only a
            // message this method chose for itself distinguishes the two.
            assertThatThrownBy(() -> MenuAccess.allowOpen(mockPlugin, player, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("menu must not be null");

            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should apply the same rule to the /menu <name> shorthand mapping")
        void shouldApplySameRuleToQuickOpen() {
            givenLoadedMenu("open-to-all", null);
            Player player = givenPlayer();
            when(player.hasPermission(BASE_NODE)).thenReturn(false);

            assertMenuRefused(() -> commands.onQuickOpen(player, "open-to-all"), player);

            // Positive control on the same mapping and the same fixture: with the node granted the
            // shorthand does reach the GUI, so the refusal above is attributable to the node and
            // not to the shorthand's reserved-name guard or to a missing menu.
            Player permitted = givenPlayer();
            when(permitted.hasPermission(BASE_NODE)).thenReturn(true);
            assertMenuOpened(() -> commands.onQuickOpen(permitted, "open-to-all"));
        }
    }
}
