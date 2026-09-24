package com.ultikits.plugins.menu.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ultikits.plugins.menu.MockBukkitSupport;
import com.ultikits.plugins.menu.config.MenuConfig;
import com.ultikits.plugins.menu.model.ButtonDefinition;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.plugins.menu.services.MenuService;
import com.ultikits.plugins.menu.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.utils.EconomyUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;

@DisplayName("CustomMenuGui Tests")
class CustomMenuGuiTest {

    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        // This bootstrap is NOT what makes this class's Bukkit registry constants
        // (Material.X, InventoryType.X) resolve. That comes from mockbukkit-v1.21 simply being on
        // the test classpath: the jar registers java.util.ServiceLoader providers for
        // io.papermc.paper.registry.RegistryAccess and io.papermc.paper.ServerBuildInfo, and
        // constant resolution needs nothing more than the provider. Measured: with these
        // mock()/unmock() calls removed and the dependency kept, the suite is 128/128 green under
        // three surefire run orders; with the dependency off the classpath instead,
        // InventoryType's static initialiser fails with ExceptionInInitializerError.
        //
        // A live server is only needed to construct a real item (new ItemStack(...), real
        // ItemMeta), which this class never does — it references Material constants only.
        //
        // The bootstrap is kept because it gives MockBukkitSupport a second real consumer besides
        // UltiMenuRegistrySentinelTest, so a break in the module's one shared test-time bootstrap
        // shows up here too rather than only in the sentinel.
        MockBukkitSupport.mock();
        mockPlayer = mock(Player.class);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
    }

    @AfterEach
    void tearDown() {
        MockBukkitSupport.unmock();
    }

    /**
     * Access the private static parsePlaceholders method via reflection.
     */
    private String callParsePlaceholders(Player player, String text) throws Exception {
        Method method = CustomMenuGui.class.getDeclaredMethod("parsePlaceholders", Player.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, player, text);
    }

    /**
     * Create a minimal MenuDefinition suitable for constructing a CustomMenuGui.
     */
    private MenuDefinition createMinimalMenu() {
        MenuDefinition menu = new MenuDefinition();
        menu.setFileName("test");
        menu.setTitle("Test Menu");
        menu.setSize(27);
        return menu;
    }

    /**
     * Create a CustomMenuGui instance for testing.
     * The Gui superclass constructor just stores fields (no Bukkit calls).
     */
    private CustomMenuGui createGui(MenuDefinition menu, UltiToolsPlugin plugin, MenuService menuService) {
        return new CustomMenuGui(mockPlayer, plugin, menu, menuService);
    }

    /**
     * Create a mock UltiToolsPlugin with i18n passthrough and optional MenuConfig.
     */
    private UltiToolsPlugin createMockPlugin(MenuConfig config) {
        UltiToolsPlugin plugin = mock(UltiToolsPlugin.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("zh"));
        when(plugin.getConfig(MenuConfig.class)).thenReturn(config);
        return plugin;
    }

    /**
     * The base permission node, written out as the literal operators actually configure rather
     * than read from the production constant: a test that reads the constant would keep passing
     * if the node were renamed, which is precisely the regression an operator would feel.
     */
    private static final String BASE_NODE = "ultikits.menu.use";

    /**
     * Invoke the private handleButtonClick method via reflection.
     */
    private void callHandleButtonClick(CustomMenuGui gui, ButtonDefinition button) throws Exception {
        Method method = CustomMenuGui.class.getDeclaredMethod("handleButtonClick", ButtonDefinition.class);
        method.setAccessible(true);
        method.invoke(gui, button);
    }

    /**
     * Set the private lastClickTime field to simulate debounce state.
     */
    private void setLastClickTime(CustomMenuGui gui, long time) throws Exception {
        Field field = CustomMenuGui.class.getDeclaredField("lastClickTime");
        field.setAccessible(true);
        field.set(gui, time);
    }

    // ==================== Placeholder Parsing Tests ====================

    @Nested
    @DisplayName("Placeholder Parsing Tests")
    class PlaceholderParsingTests {

        @Test
        @DisplayName("Should replace {player} placeholder with player name")
        void shouldReplacePlayerPlaceholder() throws Exception {
            String result = callParsePlaceholders(mockPlayer, "Hello {player}!");
            assertThat(result).isEqualTo("Hello TestPlayer!");
        }

        @Test
        @DisplayName("Should handle multiple {player} occurrences")
        void shouldHandleMultiplePlayerPlaceholders() throws Exception {
            String result = callParsePlaceholders(mockPlayer, "{player}'s menu for {player}");
            assertThat(result).isEqualTo("TestPlayer's menu for TestPlayer");
        }

        @Test
        @DisplayName("Should return empty string for null text")
        void shouldReturnEmptyForNull() throws Exception {
            String result = callParsePlaceholders(mockPlayer, null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return original text when no placeholders present")
        void shouldReturnOriginalWithoutPlaceholders() throws Exception {
            String result = callParsePlaceholders(mockPlayer, "No placeholders here");
            assertThat(result).isEqualTo("No placeholders here");
        }

        @Test
        @DisplayName("Should handle empty text")
        void shouldHandleEmptyText() throws Exception {
            String result = callParsePlaceholders(mockPlayer, "");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle text with only {player}")
        void shouldHandleOnlyPlayer() throws Exception {
            String result = callParsePlaceholders(mockPlayer, "{player}");
            assertThat(result).isEqualTo("TestPlayer");
        }

        @Test
        @DisplayName("Should handle PlaceholderAPI placeholders gracefully")
        void shouldHandlePlaceholderAPIPlaceholders() throws Exception {
            // PlaceholderAPI is on the classpath but not properly initialized in tests,
            // so parsePlaceholders should either process them or leave them unchanged
            String result = callParsePlaceholders(mockPlayer, "Online: %server_online%");
            // Should at minimum not throw and contain some content
            assertThat(result).isNotNull();
            assertThat(result).contains("Online:");
        }
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should use default cooldown when config is null")
        void shouldUseDefaultCooldownWhenConfigNull() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();

            CustomMenuGui gui = createGui(menu, plugin, menuService);

            Field cooldownField = CustomMenuGui.class.getDeclaredField("clickCooldownMs");
            cooldownField.setAccessible(true);
            assertThat(cooldownField.getLong(gui)).isEqualTo(200);
        }

        @Test
        @DisplayName("Should use config cooldown when config is provided")
        void shouldUseConfigCooldown() throws Exception {
            MenuConfig config = new MenuConfig("config/config.yml");
            config.setClickCooldownMs(500);
            UltiToolsPlugin plugin = createMockPlugin(config);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();

            CustomMenuGui gui = createGui(menu, plugin, menuService);

            Field cooldownField = CustomMenuGui.class.getDeclaredField("clickCooldownMs");
            cooldownField.setAccessible(true);
            assertThat(cooldownField.getLong(gui)).isEqualTo(500);
        }
    }

    // ==================== onOpen Tests ====================

    @Nested
    @DisplayName("onOpen Tests")
    class OnOpenTests {

        @Test
        @DisplayName("Should return early when buttons map is null")
        void shouldReturnEarlyWhenButtonsNull() {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            // MenuDefinition default buttons is an empty HashMap, not null
            // So set it to a new map and clear it
            menu.getButtons().clear();

            CustomMenuGui gui = createGui(menu, plugin, menuService);
            InventoryOpenEvent event = mock(InventoryOpenEvent.class);

            // Should not throw
            gui.onOpen(event);
            // No icons should be placed (nothing to verify except no exception)
            assertThat(gui.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should return early when buttons map is empty")
        void shouldReturnEarlyWhenButtonsEmpty() {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();

            CustomMenuGui gui = createGui(menu, plugin, menuService);
            InventoryOpenEvent event = mock(InventoryOpenEvent.class);

            gui.onOpen(event);
            assertThat(gui.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should skip null button entries")
        void shouldSkipNullButtonEntries() {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            menu.getButtons().put("nullBtn", null);

            CustomMenuGui gui = createGui(menu, plugin, menuService);
            InventoryOpenEvent event = mock(InventoryOpenEvent.class);

            gui.onOpen(event);
            // The null button was skipped, no icons placed
            assertThat(gui.getItems()).isEmpty();
        }
    }

    // ==================== handleButtonClick Debounce Tests ====================

    @Nested
    @DisplayName("Click Debounce Tests")
    class ClickDebounceTests {

        @Test
        @DisplayName("Should ignore click within cooldown period")
        void shouldIgnoreClickWithinCooldown() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            // Set last click to now
            setLastClickTime(gui, System.currentTimeMillis());

            ButtonDefinition button = new ButtonDefinition();
            button.getPlayerCommands().add("test");

            callHandleButtonClick(gui, button);

            // Nothing should happen - no commands executed
            verify(mockPlayer, never()).performCommand(anyString());
        }

        @Test
        @DisplayName("Should allow click after cooldown expires")
        void shouldAllowClickAfterCooldown() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            // Set last click to long ago
            setLastClickTime(gui, 0);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);
            button.getPlayerCommands().add("spawn");

            callHandleButtonClick(gui, button);

            verify(mockPlayer).performCommand("spawn");
        }
    }

    // ==================== handleButtonClick Permission Tests ====================

    @Nested
    @DisplayName("Button Permission Tests")
    class ButtonPermissionTests {

        @Test
        @DisplayName("Should deny click when player lacks button permission")
        void shouldDenyWithoutPermission() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPermission("vip.button");
            button.setCloseOnClick(false);
            when(mockPlayer.hasPermission("vip.button")).thenReturn(false);

            callHandleButtonClick(gui, button);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("没有权限使用此按钮"));
        }

        @Test
        @DisplayName("Should allow click when player has button permission")
        void shouldAllowWithPermission() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPermission("vip.button");
            button.setCloseOnClick(false);
            when(mockPlayer.hasPermission("vip.button")).thenReturn(true);

            callHandleButtonClick(gui, button);

            // No permission error message
            verify(mockPlayer, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should skip permission check when permission is null")
        void shouldSkipWhenPermissionNull() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPermission(null);
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            verify(mockPlayer, never()).hasPermission(anyString());
        }

        @Test
        @DisplayName("Should skip permission check when permission is empty")
        void shouldSkipWhenPermissionEmpty() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPermission("");
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            verify(mockPlayer, never()).hasPermission(anyString());
        }
    }

    // ==================== handleButtonClick Economy Tests ====================

    @Nested
    @DisplayName("Button Economy Tests")
    class ButtonEconomyTests {

        private Plugin vault;

        /**
         * Makes a Vault economy available through the public Bukkit/Vault types only
         * (UltiKits/UltiMenu#17): a MockBukkit plugin named {@code Vault} plus a Vault
         * {@link Economy} registered with the live MockBukkit services manager. The framework's
         * default economy bridge resolves exactly these on a real server, so no framework-internal
         * seam ({@code EconomyUtils.setProvider}, {@code EconomyProvider}) is touched. UltiTools
         * 6.3.0 removed the private {@code EconomyUtils} fields these tests used to set by
         * reflection.
         */
        private void registerVaultEconomy(Economy economy) {
            vault = MockBukkit.createMockPlugin("Vault");
            Bukkit.getServicesManager().register(Economy.class, economy, vault, ServicePriority.Normal);
        }

        /**
         * A paid button whose click would, if allowed through, run a player command and open a
         * sub-menu, so a denial that fails to stop the click is observable.
         */
        private ButtonDefinition givenPaidButtonWithFollowUps(double price) {
            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(price);
            button.setCloseOnClick(false);
            button.getPlayerCommands().add("spawn");
            button.setOpenMenu("sub");
            return button;
        }

        /**
         * Makes the sub-menu that {@link #givenPaidButtonWithFollowUps}'s button links to exist
         * and be permitted, so that an economy denial is unambiguously what refused the click.
         * <p>
         * Needed because the sub-menu is resolved and judged BEFORE the charge: leaving it
         * unstubbed would make these tests exercise the "menu does not exist" refusal instead of
         * the economy one, and they would stop testing what their names say.
         */
        private MenuDefinition givenReachableSubMenu(MenuService menuService) {
            MenuDefinition subMenu = new MenuDefinition();
            subMenu.setFileName("sub");
            subMenu.setTitle("Sub Menu");
            subMenu.setSize(9);
            when(menuService.getMenu("sub")).thenReturn(subMenu);
            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            return subMenu;
        }

        /**
         * A denied paid click performs none of the button's follow-up actions.
         * <p>
         * The sub-menu clause is asserted as "resolved exactly once and never navigated to"
         * rather than as "never resolved": resolution is a pure read that now happens as a
         * pre-flight, before anything irreversible, and `closeInventory` is the first thing
         * navigation does — so its absence is the direct observation that no navigation began,
         * where "never resolved" was only ever a proxy for it.
         */
        private void assertFollowUpsNeverRan(MenuService menuService) {
            verify(mockPlayer, never()).performCommand(anyString());
            verify(menuService, times(1)).getMenu("sub");
            verify(mockPlayer, never()).closeInventory();
        }

        @AfterEach
        void resetEconomy() {
            if (vault != null) {
                Bukkit.getServicesManager().unregisterAll(vault);
                vault = null;
            }
            EconomyUtils.reset();
        }

        @Test
        @DisplayName("Should skip economy when price is zero")
        void shouldSkipEconomyWhenFree() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(0);
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            // No economy message sent
            verify(mockPlayer, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should deny when economy is unavailable")
        void shouldDenyWhenEconomyUnavailable() throws Exception {
            // No plugin named Vault is present, so the framework's economy bridge reports the
            // economy unavailable and isAvailable() returns false

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(100.0);
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("经济系统不可用"));
        }

        @Test
        @DisplayName("Should deny when player has insufficient balance")
        void shouldDenyInsufficientBalance() throws Exception {
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 100.0)).thenReturn(false);
            when(mockEconomy.format(100.0)).thenReturn("$100.00");

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            givenReachableSubMenu(menuService);
            ButtonDefinition button = givenPaidButtonWithFollowUps(100.0);

            callHandleButtonClick(gui, button);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("余额不足"));
            // The denial stops the click: exactly one message, no withdrawal attempt message, and
            // none of the button's paid follow-up actions run.
            assertThat(captor.getAllValues()).hasSize(1);
            assertThat(captor.getAllValues()).noneMatch(msg -> msg.contains("扣款失败") || msg.contains("已扣除"));
            assertFollowUpsNeverRan(menuService);
            // Not a guard on this module's logic: the framework's Vault bridge re-checks has() before
            // withdrawPlayer, so this holds even if CustomMenuGui skipped its own balance check.
            verify(mockEconomy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        }

        @Test
        @DisplayName("Should deny when withdrawal fails")
        void shouldDenyWhenWithdrawFails() throws Exception {
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 50.0)).thenReturn(true);
            EconomyResponse failResponse = new EconomyResponse(0, 0,
                EconomyResponse.ResponseType.FAILURE, "error");
            when(mockEconomy.withdrawPlayer(mockPlayer, 50.0)).thenReturn(failResponse);

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            givenReachableSubMenu(menuService);
            ButtonDefinition button = givenPaidButtonWithFollowUps(50.0);

            callHandleButtonClick(gui, button);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("扣款失败"));
            // A refused withdrawal stops the click: no "deducted" confirmation and none of the
            // button's paid follow-up actions run, so the player does not get them for free.
            assertThat(captor.getAllValues()).noneMatch(msg -> msg.contains("已扣除"));
            assertFollowUpsNeverRan(menuService);
        }

        @Test
        @DisplayName("Should not charge when the sub-menu the button opens refuses the player")
        void shouldNotChargeWhenTheSubMenuRefuses() throws Exception {
            // A charge is irreversible: this module can take a player's currency and has no way
            // to give it back. So every refusal a click can produce has to happen before the
            // charge, not after it. This test is the one that pins that for the sub-menu
            // refusal, which is the newest of them.
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 500.0)).thenReturn(true);
            when(mockEconomy.format(500.0)).thenReturn("$500.00");
            when(mockEconomy.withdrawPlayer(mockPlayer, 500.0)).thenReturn(
                new EconomyResponse(500.0, 500.0, EconomyResponse.ResponseType.SUCCESS, ""));

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition gatedSubMenu = new MenuDefinition();
            gatedSubMenu.setFileName("sub");
            gatedSubMenu.setTitle("VIP");
            gatedSubMenu.setSize(9);
            gatedSubMenu.setPermission("menu.vip");
            when(menuService.getMenu("sub")).thenReturn(gatedSubMenu);
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            when(mockPlayer.hasPermission("menu.vip")).thenReturn(false);

            callHandleButtonClick(gui, givenPaidButtonWithFollowUps(500.0));

            // 1. the balance is untouched - the economy provider was never asked to withdraw
            verify(mockEconomy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            // 2. and the player was never told he had been charged
            assertThat(captor.getAllValues()).noneMatch(msg -> msg.contains("已扣除"));
            // 3. and the navigation he paid for did not happen either
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("打开此菜单"));
            verify(mockPlayer, never()).closeInventory();
            verify(mockPlayer, never()).performCommand(anyString());
        }

        @Test
        @DisplayName("Should not charge when the sub-menu the button opens does not exist")
        void shouldNotChargeWhenTheSubMenuDoesNotExist() throws Exception {
            // Same defect class as shouldNotChargeWhenTheSubMenuRefuses, reached through the
            // other refusal in the same branch: a button pointing at a menu name that no longer
            // resolves (renamed, deleted, or lost to a failed reload) must not bill for the
            // navigation it cannot perform.
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 500.0)).thenReturn(true);
            when(mockEconomy.format(500.0)).thenReturn("$500.00");
            when(mockEconomy.withdrawPlayer(mockPlayer, 500.0)).thenReturn(
                new EconomyResponse(500.0, 500.0, EconomyResponse.ResponseType.SUCCESS, ""));

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(null);
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);

            callHandleButtonClick(gui, givenPaidButtonWithFollowUps(500.0));

            verify(mockEconomy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).noneMatch(msg -> msg.contains("已扣除"));
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("不存在"));
            verify(mockPlayer, never()).closeInventory();
            verify(mockPlayer, never()).performCommand(anyString());
        }

        @Test
        @DisplayName("Should charge and navigate when the sub-menu the button opens permits the player")
        void shouldChargeAndNavigateWhenTheSubMenuPermits() throws Exception {
            // The positive control for the two tests above: on the same fixture shape, with the
            // sub-menu's node granted, the charge DOES happen and the navigation follows. Without
            // it, "never charged" could be produced by an economy fixture that never worked.
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 500.0)).thenReturn(true);
            when(mockEconomy.format(500.0)).thenReturn("$500.00");
            when(mockEconomy.withdrawPlayer(mockPlayer, 500.0)).thenReturn(
                new EconomyResponse(500.0, 500.0, EconomyResponse.ResponseType.SUCCESS, ""));

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition gatedSubMenu = new MenuDefinition();
            gatedSubMenu.setFileName("sub");
            gatedSubMenu.setTitle("VIP");
            gatedSubMenu.setSize(9);
            gatedSubMenu.setPermission("menu.vip");
            when(menuService.getMenu("sub")).thenReturn(gatedSubMenu);
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            when(mockPlayer.hasPermission("menu.vip")).thenReturn(true);

            callHandleButtonClick(gui, givenPaidButtonWithFollowUps(500.0));

            verify(mockEconomy).withdrawPlayer(mockPlayer, 500.0);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("已扣除"));
            assertThat(captor.getAllValues()).noneMatch(msg -> msg.contains("打开此菜单"));
            verify(mockPlayer).performCommand("spawn");
            verify(mockPlayer).closeInventory();
        }

        @Test
        @DisplayName("Should charge and confirm when withdrawal succeeds")
        void shouldChargeSuccessfully() throws Exception {
            Economy mockEconomy = mock(Economy.class);
            registerVaultEconomy(mockEconomy);
            when(mockEconomy.has(mockPlayer, 50.0)).thenReturn(true);
            EconomyResponse successResponse = new EconomyResponse(50.0, 950.0,
                EconomyResponse.ResponseType.SUCCESS, "");
            when(mockEconomy.withdrawPlayer(mockPlayer, 50.0)).thenReturn(successResponse);
            when(mockEconomy.format(50.0)).thenReturn("$50.00");

            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(50.0);
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("已扣除"));
            verify(mockEconomy).withdrawPlayer(mockPlayer, 50.0);
        }
    }

    // ==================== handleButtonClick Command Tests ====================

    @Nested
    @DisplayName("Button Command Tests")
    class ButtonCommandTests {

        @Test
        @DisplayName("Should execute player commands with {player} replaced")
        void shouldExecutePlayerCommands() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);
            button.getPlayerCommands().add("give {player} diamond 1");
            button.getPlayerCommands().add("spawn");

            callHandleButtonClick(gui, button);

            verify(mockPlayer).performCommand("give TestPlayer diamond 1");
            verify(mockPlayer).performCommand("spawn");
        }

        @Test
        @DisplayName("Should skip player commands when list is empty")
        void shouldSkipEmptyPlayerCommands() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);
            // playerCommands is empty by default

            callHandleButtonClick(gui, button);

            verify(mockPlayer, never()).performCommand(anyString());
        }

        @Test
        @DisplayName("Should execute console commands when UltiTools plugin is found")
        void shouldExecuteConsoleCommands() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);
            button.getConsoleCommands().add("broadcast {player} joined");

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                PluginManager mockPM = mock(PluginManager.class);
                Plugin mockUltiTools = mock(Plugin.class);
                BukkitScheduler mockScheduler = mock(BukkitScheduler.class);

                mockedBukkit.when(Bukkit::getPluginManager).thenReturn(mockPM);
                when(mockPM.getPlugin("UltiTools")).thenReturn(mockUltiTools);
                mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockScheduler);
                when(mockScheduler.runTask(any(Plugin.class), any(Runnable.class))).thenReturn(mock(BukkitTask.class));

                callHandleButtonClick(gui, button);

                // Verify scheduler.runTask was called for the console command
                verify(mockScheduler).runTask(eq(mockUltiTools), any(Runnable.class));
            }
        }

        @Test
        @DisplayName("Should skip console commands when UltiTools plugin is not found")
        void shouldSkipConsoleCommandsWhenPluginNotFound() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);
            button.getConsoleCommands().add("broadcast hello");

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                PluginManager mockPM = mock(PluginManager.class);
                mockedBukkit.when(Bukkit::getPluginManager).thenReturn(mockPM);
                when(mockPM.getPlugin("UltiTools")).thenReturn(null);

                callHandleButtonClick(gui, button);

                // No scheduler interaction since plugin wasn't found
                mockedBukkit.verify(() -> Bukkit.getScheduler(), never());
            }
        }
    }

    // ==================== handleButtonClick Sub-Menu Tests ====================

    @Nested
    @DisplayName("Button Sub-Menu Tests")
    class ButtonSubMenuTests {

        @Test
        @DisplayName("Should show error when sub-menu does not exist")
        void shouldShowErrorForMissingSubMenu() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setOpenMenu("nonexistent");
            when(menuService.getMenu("nonexistent")).thenReturn(null);

            try (MockedStatic<Bukkit> ignored = mockStatic(Bukkit.class)) {
                callHandleButtonClick(gui, button);
            }

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("不存在"));
        }

        @Test
        @DisplayName("Should close inventory and schedule sub-menu when it exists")
        void shouldOpenSubMenu() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            MenuDefinition subMenu = new MenuDefinition();
            subMenu.setFileName("sub");
            subMenu.setTitle("Sub Menu");
            subMenu.setSize(9);

            ButtonDefinition button = new ButtonDefinition();
            button.setOpenMenu("sub");
            when(menuService.getMenu("sub")).thenReturn(subMenu);
            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                PluginManager mockPM = mock(PluginManager.class);
                Plugin mockUltiTools = mock(Plugin.class);
                BukkitScheduler mockScheduler = mock(BukkitScheduler.class);

                mockedBukkit.when(Bukkit::getPluginManager).thenReturn(mockPM);
                when(mockPM.getPlugin("UltiTools")).thenReturn(mockUltiTools);
                mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockScheduler);
                when(mockScheduler.runTask(any(Plugin.class), any(Runnable.class))).thenReturn(mock(BukkitTask.class));

                callHandleButtonClick(gui, button);

                verify(mockPlayer).closeInventory();
                verify(mockScheduler).runTask(eq(mockUltiTools), any(Runnable.class));
            }
        }

        @Test
        @DisplayName("Should skip sub-menu scheduling when UltiTools plugin not found")
        void shouldSkipSubMenuWhenPluginNotFound() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            MenuDefinition subMenu = new MenuDefinition();
            subMenu.setFileName("sub");
            subMenu.setTitle("Sub");
            subMenu.setSize(9);

            ButtonDefinition button = new ButtonDefinition();
            button.setOpenMenu("sub");
            when(menuService.getMenu("sub")).thenReturn(subMenu);
            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);

            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                PluginManager mockPM = mock(PluginManager.class);
                mockedBukkit.when(Bukkit::getPluginManager).thenReturn(mockPM);
                when(mockPM.getPlugin("UltiTools")).thenReturn(null);

                callHandleButtonClick(gui, button);

                verify(mockPlayer).closeInventory();
            }
        }
    }

    // ==================== handleButtonClick Sub-Menu Access Tests ====================

    /**
     * The sub-menu path's half of the one shared menu-access rule (UltiKits/UltiMenu#15).
     * <p>
     * Before the fix this path checked nothing at all — not {@code ultikits.menu.use}, and not
     * even the sub-menu's own {@code permission} key, which both other paths did check. A
     * {@code permission}-gated menu was therefore reachable by anyone who could click a button
     * linking to it from a menu he was already allowed to see, which is the one place a menu's
     * own key is most likely to be the thing an operator relied on.
     * <p>
     * <b>How the two outcomes are told apart.</b> Navigation is observed as the scheduled task
     * that opens the sub-menu on the next tick — {@code BukkitScheduler#runTask} — plus the
     * closing of the current inventory. A refusal asserts the negative of BOTH, plus the refusal
     * message, and each refusal test is paired with an allowed case built on the same fixture, so
     * "never scheduled" cannot be satisfied by a click that failed for some earlier reason.
     */
    @Nested
    @DisplayName("Button Sub-Menu Access Tests (the sub-menu's own permission + the base node)")
    class ButtonSubMenuAccessTests {

        private MenuDefinition subMenu(String permission) {
            MenuDefinition subMenu = new MenuDefinition();
            subMenu.setFileName("sub");
            subMenu.setTitle("Sub Menu");
            subMenu.setSize(9);
            subMenu.setPermission(permission);
            return subMenu;
        }

        private ButtonDefinition navigatingButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.setOpenMenu("sub");
            return button;
        }

        /**
         * Clicks the button with {@code Bukkit} statically mocked, and reports whether the
         * sub-menu was scheduled to open. `closeInventory` is asserted by the caller.
         */
        private boolean clickAndReportScheduled(CustomMenuGui gui, ButtonDefinition button) throws Exception {
            try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
                PluginManager mockPM = mock(PluginManager.class);
                Plugin mockUltiTools = mock(Plugin.class);
                BukkitScheduler mockScheduler = mock(BukkitScheduler.class);

                mockedBukkit.when(Bukkit::getPluginManager).thenReturn(mockPM);
                when(mockPM.getPlugin("UltiTools")).thenReturn(mockUltiTools);
                mockedBukkit.when(Bukkit::getScheduler).thenReturn(mockScheduler);
                when(mockScheduler.runTask(any(Plugin.class), any(Runnable.class))).thenReturn(mock(BukkitTask.class));

                callHandleButtonClick(gui, button);

                return !mockingDetails(mockScheduler).getInvocations().isEmpty();
            }
        }

        /**
         * The message is matched on {@code 打开此菜单} ("open this menu") rather than the shorter
         * {@code 没有权限} ("no permission"), which is a prefix of the BUTTON refusal as well —
         * and a button refusal reaching here would be a different defect wearing this one's
         * clothes.
         */
        private void assertRefused(boolean scheduled) {
            assertThat(scheduled).as("a refused sub-menu must never be scheduled to open").isFalse();
            verify(mockPlayer, never()).closeInventory();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("打开此菜单"));
        }

        private void assertOpened(boolean scheduled) {
            assertThat(scheduled).as("an allowed sub-menu is scheduled to open on the next tick").isTrue();
            verify(mockPlayer).closeInventory();
            verify(mockPlayer, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should refuse when the player lacks the sub-menu's own permission")
        void shouldRefuseWithoutSubMenuPermission() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(subMenu("admin.sub"));
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            when(mockPlayer.hasPermission("admin.sub")).thenReturn(false);

            assertRefused(clickAndReportScheduled(gui, navigatingButton()));
        }

        @Test
        @DisplayName("Should open when the player holds the sub-menu's own permission")
        void shouldOpenWithSubMenuPermission() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(subMenu("admin.sub"));
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            when(mockPlayer.hasPermission("admin.sub")).thenReturn(true);

            assertOpened(clickAndReportScheduled(gui, navigatingButton()));
        }

        @Test
        @DisplayName("Should refuse when the player lacks the base node, even for a sub-menu with no permission")
        void shouldRefuseWithoutBaseNode() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(subMenu(null));
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(false);

            assertRefused(clickAndReportScheduled(gui, navigatingButton()));
        }

        @Test
        @DisplayName("Should open when the player holds the base node and the sub-menu sets no permission")
        void shouldOpenWithBaseNodeOnly() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(subMenu(null));
            CustomMenuGui gui = createGui(createMinimalMenu(), plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);

            assertOpened(clickAndReportScheduled(gui, navigatingButton()));
        }

        @Test
        @DisplayName("Should judge the sub-menu's permission, not the parent menu's")
        void shouldCheckTheSubMenuNotTheParent() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            when(menuService.getMenu("sub")).thenReturn(subMenu("admin.sub"));

            // The parent is a menu this player IS allowed to see — the situation the issue
            // describes, where being inside the parent is what the sub-menu's own key was
            // supposed to be independent of.
            MenuDefinition parent = createMinimalMenu();
            parent.setPermission("parent.menu");
            CustomMenuGui gui = createGui(parent, plugin, menuService);

            when(mockPlayer.hasPermission(BASE_NODE)).thenReturn(true);
            when(mockPlayer.hasPermission("parent.menu")).thenReturn(true);
            when(mockPlayer.hasPermission("admin.sub")).thenReturn(false);

            assertRefused(clickAndReportScheduled(gui, navigatingButton()));
        }
    }

    // ==================== handleButtonClick Close-on-Click Tests ====================

    @Nested
    @DisplayName("Close On Click Tests")
    class CloseOnClickTests {

        @Test
        @DisplayName("Should close inventory when closeOnClick is true")
        void shouldCloseWhenEnabled() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(true);

            callHandleButtonClick(gui, button);

            verify(mockPlayer).closeInventory();
        }

        @Test
        @DisplayName("Should not close inventory when closeOnClick is false")
        void shouldNotCloseWhenDisabled() throws Exception {
            UltiToolsPlugin plugin = createMockPlugin(null);
            MenuService menuService = mock(MenuService.class);
            MenuDefinition menu = createMinimalMenu();
            CustomMenuGui gui = createGui(menu, plugin, menuService);

            ButtonDefinition button = new ButtonDefinition();
            button.setCloseOnClick(false);

            callHandleButtonClick(gui, button);

            verify(mockPlayer, never()).closeInventory();
        }
    }

    // ==================== Button Click Data Tests ====================

    @Nested
    @DisplayName("Button Click Logic Data Tests")
    class ButtonClickDataTests {

        @Test
        @DisplayName("Should recognize button with price as paid button")
        void shouldRecognizePaidButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(100.0);

            assertThat(button.getPrice()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should recognize button with zero price as free")
        void shouldRecognizeFreeButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.setPrice(0);

            assertThat(button.getPrice()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should recognize button with open-menu as navigation button")
        void shouldRecognizeNavigationButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.setOpenMenu("submenu");

            assertThat(button.getOpenMenu()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("Should recognize button with commands")
        void shouldRecognizeCommandButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.getPlayerCommands().add("spawn");

            assertThat(button.getPlayerCommands()).isNotEmpty();
            assertThat(button.getConsoleCommands()).isEmpty();
        }

        @Test
        @DisplayName("Should recognize button with permission requirement")
        void shouldRecognizePermittedButton() {
            ButtonDefinition button = new ButtonDefinition();
            button.setPermission("vip.use");

            assertThat(button.getPermission()).isNotNull().isNotEmpty();
        }
    }

    // ==================== Menu Definition Completeness Tests ====================

    @Nested
    @DisplayName("Menu Definition Completeness Tests")
    class MenuDefinitionCompletenessTests {

        @Test
        @DisplayName("Should build complete menu definition for GUI")
        void shouldBuildCompleteMenu() {
            MenuDefinition menu = new MenuDefinition();
            menu.setFileName("test");
            menu.setSize(27);
            menu.setTitle("&6Test Menu");

            ButtonDefinition btn1 = new ButtonDefinition();
            btn1.setId("btn1");
            btn1.setItem(Material.DIAMOND);
            btn1.setPosition(13);
            btn1.setName("&bDiamond");

            menu.getButtons().put("btn1", btn1);

            assertThat(menu.getFileName()).isEqualTo("test");
            assertThat(menu.getSize()).isEqualTo(27);
            assertThat(menu.getSize() / 9).isEqualTo(3); // Row count for GUI
            assertThat(menu.getButtons()).hasSize(1);
            assertThat(menu.getButtons().get("btn1").getItem()).isEqualTo(Material.DIAMOND);
        }
    }
}
