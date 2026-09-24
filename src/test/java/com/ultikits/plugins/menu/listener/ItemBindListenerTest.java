package com.ultikits.plugins.menu.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.plugins.menu.services.MenuService;
import com.ultikits.plugins.menu.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("ItemBindListener Tests")
class ItemBindListenerTest {

    private UltiToolsPlugin mockPlugin;
    private MenuService mockMenuService;
    private ItemBindListener listener;

    @BeforeEach
    void setUp() {
        mockPlugin = mock(UltiToolsPlugin.class);
        mockMenuService = mock(MenuService.class);
        when(mockPlugin.i18n(anyString())).thenAnswer(CatalogueText.answer("zh"));

        listener = new ItemBindListener(mockPlugin, mockMenuService);
    }

    private PlayerInteractEvent createEvent(Action action, Player player) {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    private Player createPlayerWithMainHandItem(Material material, String displayName, java.util.List<String> lore) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);

        // Off-hand fixture: ItemBindListener only ever reads getType() on this value (to check
        // it is not AIR before deciding whether to try matching a menu against it), never
        // getItemMeta()/getDisplayName()/getLore() — see the line-59 attribution in
        // 14-LEDGER-UltiMenu.md. A mock stubbed for that one accessor is fully faithful and
        // needs no live Bukkit registry, unlike a real `new ItemStack(Material.AIR)`.
        ItemStack offHandItem = mock(ItemStack.class);
        when(offHandItem.getType()).thenReturn(Material.AIR);

        when(player.getName()).thenReturn("TestPlayer");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(item);
        when(inventory.getItemInOffHand()).thenReturn(offHandItem);
        when(item.getType()).thenReturn(material);
        when(item.getItemMeta()).thenReturn(meta);

        if (displayName != null) {
            when(meta.hasDisplayName()).thenReturn(true);
            when(meta.getDisplayName()).thenReturn(displayName);
        } else {
            when(meta.hasDisplayName()).thenReturn(false);
        }

        if (lore != null) {
            when(meta.hasLore()).thenReturn(true);
            when(meta.getLore()).thenReturn(lore);
        } else {
            when(meta.hasLore()).thenReturn(false);
        }

        return player;
    }

    private MenuDefinition createBoundMenu(Material bindItem, String bindName, String bindLore) {
        MenuDefinition menu = new MenuDefinition();
        menu.setFileName("testmenu");
        menu.setBindItem(bindItem);
        menu.setBindName(bindName);
        menu.setBindLore(bindLore);
        return menu;
    }

    /**
     * The base permission node, written out as the literal operators actually configure rather
     * than read from the production constant: a test that reads the constant would keep passing
     * if the node were renamed, which is precisely the regression an operator would feel.
     */
    private static final String BASE_NODE = "ultikits.menu.use";

    /**
     * Asserts the bound item actually opened its menu.
     * <p>
     * Reaching {@code CustomMenuGui#open()} throws obliviate-invs'
     * {@code NullPointerException("Inventory API is not initialized...")}, because no live
     * {@code InventoryAPI} exists in a unit test. The message is asserted, not only the exception
     * type, so an unrelated {@code NullPointerException} — a missing stub, say — cannot stand in
     * for a menu that opened.
     */
    private void assertMenuOpened(PlayerInteractEvent event) {
        assertThatThrownBy(() -> listener.onPlayerInteract(event))
            .as("reaching CustomMenuGui#open() is what proves the bound item opened its menu")
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Inventory API is not initialized");
    }

    /**
     * Asserts the bound item was refused: the player was told so, AND the menu was never reached.
     * The second half is what stops this from passing vacuously — the absence of a message would
     * otherwise be indistinguishable from a listener that silently opened the menu.
     * <p>
     * The message is matched on {@code 打开此菜单} ("open this menu"), not on the shorter
     * {@code 没有权限} ("no permission"): that shorter string is a prefix of the BUTTON refusal
     * too, so matching it would accept the wrong refusal as though it were this one.
     */
    private void assertMenuRefused(PlayerInteractEvent event, Player player) {
        assertThatCode(() -> listener.onPlayerInteract(event))
            .as("a refused bound item must never reach CustomMenuGui#open()")
            .doesNotThrowAnyException();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("打开此菜单"));
    }

    // ==================== Action Filtering Tests ====================

    @Nested
    @DisplayName("Action Filtering Tests")
    class ActionFilteringTests {

        @Test
        @DisplayName("Should ignore left-click air events")
        void shouldIgnoreLeftClickAir() {
            Player player = mock(Player.class);
            PlayerInteractEvent event = createEvent(Action.LEFT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should ignore left-click block events")
        void shouldIgnoreLeftClickBlock() {
            Player player = mock(Player.class);
            PlayerInteractEvent event = createEvent(Action.LEFT_CLICK_BLOCK, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should ignore physical events")
        void shouldIgnorePhysical() {
            Player player = mock(Player.class);
            PlayerInteractEvent event = createEvent(Action.PHYSICAL, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should process right-click air events and cancel on match")
        void shouldProcessRightClickAir() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }

        @Test
        @DisplayName("Should process right-click block events and cancel on match")
        void shouldProcessRightClickBlock() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_BLOCK, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }
    }

    // ==================== Material Matching Tests ====================

    @Nested
    @DisplayName("Material Matching Tests")
    class MaterialMatchingTests {

        @Test
        @DisplayName("Should match when material equals bind-item")
        void shouldMatchMaterial() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }

        @Test
        @DisplayName("Should not match when material differs from bind-item")
        void shouldNotMatchDifferentMaterial() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.DIAMOND, null, null);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should skip menu with no bind-item")
        void shouldSkipNoBind() {
            MenuDefinition menu = new MenuDefinition();
            menu.setFileName("nobind");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }
    }

    // ==================== Display Name Matching Tests ====================

    @Nested
    @DisplayName("Display Name Matching Tests")
    class DisplayNameMatchingTests {

        @Test
        @DisplayName("Should match display name after color normalization")
        void shouldMatchWithColorCodes() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, "&6My Compass", null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            String coloredName = ChatColor.translateAlternateColorCodes('&', "&6My Compass");
            Player player = createPlayerWithMainHandItem(Material.COMPASS, coloredName, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }

        @Test
        @DisplayName("Should not match when display name differs")
        void shouldNotMatchDifferentName() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, "&6My Compass", null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, "Different Name", null);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should not match when item has no display name but menu requires one")
        void shouldNotMatchMissingName() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, "&6My Compass", null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }
    }

    // ==================== Lore Matching Tests ====================

    @Nested
    @DisplayName("Lore Matching Tests")
    class LoreMatchingTests {

        @Test
        @DisplayName("Should match when lore contains expected text")
        void shouldMatchLore() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, "Right click");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null,
                Arrays.asList("Some text", "Right click to open"));
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }

        @Test
        @DisplayName("Should not match when lore does not contain expected text")
        void shouldNotMatchDifferentLore() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, "Right click");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null,
                Arrays.asList("Some other text", "Nothing matches"));
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }

        @Test
        @DisplayName("Should not match when item has no lore but menu requires it")
        void shouldNotMatchMissingLore() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, "Right click");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event, never()).setCancelled(true);
        }
    }

    // ==================== Permission Tests ====================

    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {

        @Test
        @DisplayName("Should deny access when player lacks menu permission")
        void shouldDenyWithoutPermission() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            menu.setPermission("vip.menu");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission("vip.menu")).thenReturn(false);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            listener.onPlayerInteract(event);

            verify(event).setCancelled(true);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(msg -> msg.contains("打开此菜单"));
        }

        @Test
        @DisplayName("Should allow access when player has menu permission")
        void shouldAllowWithPermission() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            menu.setPermission("vip.menu");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should allow access when menu has no permission requirement and player holds the base node")
        void shouldAllowWithNoPermission() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
            verify(player, never()).sendMessage(anyString());
        }
    }

    // ==================== Base Permission Tests ====================

    /**
     * The bound-item path's half of the one shared menu-access rule (UltiKits/UltiMenu#14).
     * <p>
     * Before the fix this path consulted only the menu's own {@code permission} key, so a menu
     * that imposes no per-menu node — the shipped {@code menus/example.yml} ships
     * {@code permission: null}, which the parser reads back as absent — opened for a player
     * holding no node of this module at all, provided he could get hold of an item matching its
     * {@code bind-item}/{@code bind-name}/{@code bind-lore}. The command path never had that hole,
     * because the framework gates the command class on {@code ultikits.menu.use}; the hole was in
     * the difference between the two paths, not in either path's own code read alone.
     * <p>
     * Every test here uses a menu with NO {@code permission} key, so the only thing that can
     * refuse is the base node — the exact configuration the issue reports.
     */
    @Nested
    @DisplayName("Base Permission Tests (ultikits.menu.use)")
    class BasePermissionTests {

        @Test
        @DisplayName("Should refuse a bound item when the player lacks ultikits.menu.use and the menu sets no permission")
        void shouldRefuseWithoutBaseNode() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(false);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuRefused(event, player);

            // The interaction is still cancelled: the item matched a menu, so it must not also be
            // used as an ordinary item. Refusing the menu and swallowing the item use are separate
            // outcomes and both are asserted, because the refusal alone would be satisfied by a
            // listener that had stopped matching the item at all.
            verify(event).setCancelled(true);
        }

        @Test
        @DisplayName("Should refuse a bound item when the player holds ultikits.menu.use but not the menu's own permission")
        void shouldRefuseWithoutMenuNode() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            menu.setPermission("vip.menu");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(false);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuRefused(event, player);
        }

        @Test
        @DisplayName("Should open a bound item's menu when the player holds ultikits.menu.use and the menu sets no permission")
        void shouldOpenWithBaseNode() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);
            verify(event).setCancelled(true);
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should open a bound item's menu when the player holds both the base node and the menu's own permission")
        void shouldOpenWithBothNodes() {
            MenuDefinition menu = createBoundMenu(Material.COMPASS, null, null);
            menu.setPermission("vip.menu");
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(true);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should stop at the refused menu rather than fall through to another menu bound to the same item")
        void shouldNotFallThroughAfterRefusal() {
            // Two menus bound to the same item, the first of them gated. The listener stops at the
            // first match whether or not access is granted, so a refusal ends the interaction
            // instead of walking on to the next menu that happens to match. That is unchanged by
            // the access fix and is pinned here because nothing else asserts it: without it, a
            // refusal that fell through would open the ungated menu and no test would notice.
            //
            // Which of two menus bound to the identical item matches first is undefined on a real
            // server (MenuServiceImpl#menus is a plain HashMap, as FEATURES.md records); the
            // mocked service returns a fixed order here so the test has one.
            MenuDefinition gated = createBoundMenu(Material.COMPASS, null, null);
            gated.setFileName("gated");
            gated.setPermission("vip.menu");
            MenuDefinition ungated = createBoundMenu(Material.COMPASS, null, null);
            ungated.setFileName("ungated");
            when(mockMenuService.getAllMenus()).thenReturn(Arrays.asList(gated, ungated));

            Player player = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("vip.menu")).thenReturn(false);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuRefused(event, player);

            // Positive control on the same two-menu fixture: with the gated menu's node granted,
            // the first match does open — so "nothing opened" above is the refusal being final,
            // not the fixture failing to match anything at all.
            Player permitted = createPlayerWithMainHandItem(Material.COMPASS, null, null);
            when(permitted.hasPermission(BASE_NODE)).thenReturn(true);
            when(permitted.hasPermission("vip.menu")).thenReturn(true);
            assertMenuOpened(createEvent(Action.RIGHT_CLICK_AIR, permitted));
        }

        @Test
        @DisplayName("Should apply the same rule to an off-hand bound item")
        void shouldRefuseOffHandWithoutBaseNode() {
            MenuDefinition menu = createBoundMenu(Material.CLOCK, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = mock(Player.class);
            when(player.getName()).thenReturn("TestPlayer");
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);

            ItemStack mainItem = mock(ItemStack.class);
            when(mainItem.getType()).thenReturn(Material.DIAMOND);
            when(mainItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInMainHand()).thenReturn(mainItem);

            ItemStack offItem = mock(ItemStack.class);
            when(offItem.getType()).thenReturn(Material.CLOCK);
            when(offItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInOffHand()).thenReturn(offItem);

            when(player.hasPermission(BASE_NODE)).thenReturn(false);
            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuRefused(event, player);

            // Positive control on the identical fixture: with the node granted, the same off-hand
            // item does open its menu — so the refusal above is attributable to the node and not
            // to the off-hand fall-through failing to match at all.
            Player permitted = mock(Player.class);
            when(permitted.getName()).thenReturn("TestPlayer");
            PlayerInventory permittedInventory = mock(PlayerInventory.class);
            when(permitted.getInventory()).thenReturn(permittedInventory);
            when(permittedInventory.getItemInMainHand()).thenReturn(mainItem);
            when(permittedInventory.getItemInOffHand()).thenReturn(offItem);
            when(permitted.hasPermission(BASE_NODE)).thenReturn(true);

            assertMenuOpened(createEvent(Action.RIGHT_CLICK_AIR, permitted));
        }
    }

    // ==================== Off-hand Priority Tests ====================

    @Nested
    @DisplayName("Hand Priority Tests")
    class HandPriorityTests {

        @Test
        @DisplayName("Should check main hand before off hand")
        void shouldCheckMainHandFirst() {
            // The two menus are made distinguishable by outcome, not just by identity: the
            // off-hand's menu is permission-gated and the player does not hold its node, so if
            // the off-hand were consulted first the click would produce a REFUSAL, while the
            // main hand's ungated menu produces an OPEN. Without that asymmetry both hands lead
            // to the same observable and the test cannot tell which one won — which is what it
            // exists to tell.
            MenuDefinition compassMenu = createBoundMenu(Material.COMPASS, null, null);
            MenuDefinition clockMenu = createBoundMenu(Material.CLOCK, null, null);
            clockMenu.setPermission("off.hand.menu");
            when(mockMenuService.getAllMenus()).thenReturn(Arrays.asList(compassMenu, clockMenu));

            Player player = mock(Player.class);
            when(player.getName()).thenReturn("TestPlayer");
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);

            ItemStack mainItem = mock(ItemStack.class);
            when(mainItem.getType()).thenReturn(Material.COMPASS);
            when(mainItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInMainHand()).thenReturn(mainItem);

            ItemStack offItem = mock(ItemStack.class);
            when(offItem.getType()).thenReturn(Material.CLOCK);
            when(offItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInOffHand()).thenReturn(offItem);

            when(player.hasPermission(BASE_NODE)).thenReturn(true);
            when(player.hasPermission("off.hand.menu")).thenReturn(false);

            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
            verify(player, never()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should fall through to off-hand when main hand does not match")
        void shouldFallThroughToOffHand() {
            MenuDefinition menu = createBoundMenu(Material.CLOCK, null, null);
            when(mockMenuService.getAllMenus()).thenReturn(Collections.singletonList(menu));

            Player player = mock(Player.class);
            when(player.getName()).thenReturn("TestPlayer");
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);

            ItemStack mainItem = mock(ItemStack.class);
            when(mainItem.getType()).thenReturn(Material.DIAMOND);
            when(mainItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInMainHand()).thenReturn(mainItem);

            ItemStack offItem = mock(ItemStack.class);
            when(offItem.getType()).thenReturn(Material.CLOCK);
            when(offItem.getItemMeta()).thenReturn(null);
            when(inventory.getItemInOffHand()).thenReturn(offItem);

            when(player.hasPermission(BASE_NODE)).thenReturn(true);

            PlayerInteractEvent event = createEvent(Action.RIGHT_CLICK_AIR, player);

            assertMenuOpened(event);

            verify(event).setCancelled(true);
        }
    }

    // ==================== Normalize Text Tests ====================

    @Nested
    @DisplayName("Text Normalization Tests")
    class NormalizeTextTests {

        @Test
        @DisplayName("Should normalize null text to empty string")
        void shouldNormalizeNullToEmpty() throws Exception {
            java.lang.reflect.Method method = ItemBindListener.class.getDeclaredMethod("normalizeText", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(listener, (Object) null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should strip color codes during normalization")
        void shouldStripColorCodes() throws Exception {
            java.lang.reflect.Method method = ItemBindListener.class.getDeclaredMethod("normalizeText", String.class);
            method.setAccessible(true);

            String colored = ChatColor.translateAlternateColorCodes('&', "&6&lMy Item");
            String result = (String) method.invoke(listener, colored);
            assertThat(result).isEqualTo("My Item");
        }

        @Test
        @DisplayName("Should handle plain text without color codes")
        void shouldHandlePlainText() throws Exception {
            java.lang.reflect.Method method = ItemBindListener.class.getDeclaredMethod("normalizeText", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(listener, "Plain text");
            assertThat(result).isEqualTo("Plain text");
        }
    }
}
