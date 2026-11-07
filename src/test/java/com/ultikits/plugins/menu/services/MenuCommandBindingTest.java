package com.ultikits.plugins.menu.services;

import com.ultikits.plugins.menu.gui.CustomMenuGui;
import com.ultikits.plugins.menu.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.PreDestroy;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuCommandBindingTest {
    @TempDir Path directory;
    private UltiToolsPlugin plugin;
    private PluginLogger logger;
    private SimpleCommandMap map;
    private MockedStatic<Bukkit> bukkit;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        plugin = mock(UltiToolsPlugin.class);
        logger = mock(PluginLogger.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getResourceFolderPath()).thenReturn(directory.toString());
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        Files.createDirectories(directory.resolve("menus"));
        map = new SimpleCommandMap(mock(org.bukkit.Server.class), new java.util.HashMap<>());
        player = mock(Player.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getCommandMap).thenReturn(map);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
    }

    @AfterEach
    void tearDown() { bukkit.close(); }

    private void write(String file, String yaml) throws Exception {
        Files.write(directory.resolve("menus").resolve(file), yaml.getBytes(StandardCharsets.UTF_8));
    }

    private MenuServiceImpl load(String yaml) throws Exception {
        write("shop.yml", yaml);
        return new MenuServiceImpl(plugin);
    }

    private void destroy(MenuServiceImpl service) throws Exception {
        boolean found = false;
        for (Method method : service.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                method.setAccessible(true);
                method.invoke(service);
                found = true;
            }
        }
        assertThat(found).as("framework lifecycle cleanup hook").isTrue();
    }

    @Test
    void registersOnlyNewOptInAndNeverWritesOperatorFile() throws Exception {
        String yaml = "open-command: Shop\ncommand: legacy\n";
        load(yaml);
        assertThat(map.getCommand("shop")).isNotNull();
        assertThat(map.getCommand("ultimenu:shop")).isSameAs(map.getCommand("shop"));
        assertThat(map.getCommand("legacy")).isNull();
        assertThat(new String(Files.readAllBytes(directory.resolve("menus/shop.yml")), StandardCharsets.UTF_8))
                .isEqualTo(yaml);
        verify(logger).warn(contains("'command'"));
        verify(player).updateCommands();
    }

    @Test
    void reloadReplacesCommandAndUnregistersDeletedOrInvalidMenus() throws Exception {
        MenuServiceImpl service = load("open-command: shop\n");
        Command old = map.getCommand("shop");
        assertThat(old).isNotNull();
        write("shop.yml", "open-command: store\n");
        service.reload();
        assertThat(map.getCommand("shop")).isNull();
        assertThat(map.getCommand("ultimenu:shop")).isNull();
        assertThat(map.getCommand("store")).isNotNull();
        old.execute(player, "shop", new String[0]);
        verify(player, never()).hasPermission(anyString());
        write("shop.yml", "size: 10\nopen-command: store\n");
        service.reload();
        assertThat(map.getCommand("store")).isNull();
        write("shop.yml", "open-command: store\n");
        service.reload();
        Files.delete(directory.resolve("menus/shop.yml"));
        service.reload();
        assertThat(map.getCommand("store")).isNull();
        assertThat(map.getCommand("ultimenu:store")).isNull();
    }

    @Test
    void reloadRemovalAndFileRenameFollowCurrentDefinition() throws Exception {
        MenuServiceImpl service = load("open-command: shop\n");
        assertThat(map.getCommand("shop")).isNotNull();
        Files.move(directory.resolve("menus/shop.yml"), directory.resolve("menus/renamed.yml"));
        service.reload();
        when(player.hasPermission("ultikits.menu.use")).thenReturn(true);
        try (MockedConstruction<CustomMenuGui> gui = mockConstruction(CustomMenuGui.class)) {
            map.getCommand("shop").execute(player, "shop", new String[0]);
            assertThat(gui.constructed()).hasSize(1);
            verify(gui.constructed().get(0)).open();
        }
        write("renamed.yml", "title: no binding\n");
        service.reload();
        assertThat(map.getCommand("shop")).isNull();
    }

    @Test
    void refusesEveryDuplicateAndNamesBothFiles() throws Exception {
        write("other.yml", "open-command: SHOP\n");
        MenuServiceImpl service = load("open-command: shop\n");
        assertThat(map.getCommand("shop")).isNull();
        assertThat(service.getAllMenus()).hasSize(2);
        verify(logger).warn(argThat((String message) -> message.contains("shop.yml") && message.contains("other.yml")));
    }

    @Test
    void refusesReservedMenuBeforeFrameworkRegistersIt() throws Exception {
        load("open-command: menu\n");
        assertThat(map.getCommand("menu")).isNull();
        verify(logger).warn(argThat((String message) -> message.contains("shop.yml") && message.contains("UltiMenu /menu")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"shop", "ultimenu:shop"})
    void neverReplacesAnotherOwnersBareOrFallbackLabel(String label) throws Exception {
        Command external = mock(Command.class);
        when(external.getName()).thenReturn("foreign");
        map.getKnownCommands().put(label, external);
        load("open-command: shop\n");
        assertThat(map.getCommand(label)).isSameAs(external);
        assertThat(map.getKnownCommands().values()).doesNotContainNull();
        verify(logger).warn(argThat((String message) -> message.contains("shop.yml") && message.contains("foreign")));
        if (!label.equals("shop")) assertThat(map.getCommand("shop")).isNull();
    }

    @Test
    void pluginCollisionNamesPluginAndExecutorAndCanRecoverOnReload() throws Exception {
        org.bukkit.command.PluginCommand external = mock(org.bukkit.command.PluginCommand.class);
        org.bukkit.plugin.Plugin owner = mock(org.bukkit.plugin.Plugin.class);
        org.bukkit.command.CommandExecutor executor = mock(org.bukkit.command.CommandExecutor.class);
        when(external.getPlugin()).thenReturn(owner);
        when(owner.getName()).thenReturn("ForeignPlugin");
        when(external.getName()).thenReturn("shop");
        when(external.getExecutor()).thenReturn(executor);
        map.getKnownCommands().put("shop", external);
        MenuServiceImpl service = load("open-command: shop\n");
        verify(logger).warn(argThat((String message) -> message.contains("ForeignPlugin")
                && message.contains(executor.getClass().getName()) && message.contains("shop.yml")));
        assertThat(map.getCommand("shop")).isSameAs(external);
        map.getKnownCommands().remove("shop");
        service.reload();
        assertThat(map.getCommand("shop")).isNotNull().isNotSameAs(external);
    }

    @Test
    void cleanupPurgesExactInstancesButPreservesAReplacement() throws Exception {
        MenuServiceImpl service = load("open-command: shop\n");
        Command original = map.getCommand("shop");
        assertThat(original).isNotNull();
        Command replacement = mock(Command.class);
        map.getKnownCommands().put("shop", replacement);
        destroy(service);
        assertThat(map.getCommand("shop")).isSameAs(replacement);
        assertThat(map.getCommand("ultimenu:shop")).isNull();
        original.execute(player, "shop", new String[0]);
        verify(player, never()).hasPermission(anyString());
        destroy(service);
    }

    @Test
    void consoleAndArgumentsAreRefusedAndTabSuggestionsAreEmpty() throws Exception {
        load("open-command: shop\n");
        Command command = map.getCommand("shop");
        assertThat(command).isNotNull();
        CommandSender console = mock(CommandSender.class);
        command.execute(console, "shop", new String[0]);
        verify(console).sendMessage(contains("Only players can open menus!"));
        command.execute(player, "ultimenu:shop", new String[]{"extra"});
        verify(player).sendMessage(contains("Usage: /ultimenu:shop"));
        verify(player, never()).hasPermission(anyString());
        assertThat(command.tabComplete(player, "shop", new String[]{""})).isEmpty();
    }

    @Test
    void sharesBaseAndMenuPermissionsAndOpensOnlyWhenBothHold() throws Exception {
        load("open-command: shop\npermission: shop.vip\n");
        Command command = map.getCommand("shop");
        assertThat(command).isNotNull();
        try (MockedConstruction<CustomMenuGui> gui = mockConstruction(CustomMenuGui.class)) {
            command.execute(player, "shop", new String[0]);
            verify(player).sendMessage(contains("need ultikits.menu.use"));
            assertThat(gui.constructed()).isEmpty();
            when(player.hasPermission("ultikits.menu.use")).thenReturn(true);
            command.execute(player, "shop", new String[0]);
            verify(player).sendMessage(contains("You don't have permission to open this menu!"));
            assertThat(gui.constructed()).isEmpty();
            when(player.hasPermission("shop.vip")).thenReturn(true);
            command.execute(player, "shop", new String[0]);
            assertThat(gui.constructed()).hasSize(1);
            verify(gui.constructed().get(0)).open();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shop", "foreign:shop", "shop args", "123", "[]", "true", "12345678901234567890123456789012345"})
    void invalidBindingsWarnWithoutDiscardingMenu(String value) throws Exception {
        MenuServiceImpl service = load("open-command: " + value + "\n");
        assertThat(service.getMenu("shop")).isNotNull();
        assertThat(map.getCommand("shop")).isNull();
        verify(logger).warn(argThat((String message) -> message.contains("open-command") && message.contains("shop.yml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "''"})
    void emptyBindingIsDisabledWithoutWarning(String value) throws Exception {
        load("open-command: " + value + "\n");
        assertThat(map.getCommand("shop")).isNull();
        verify(logger, never()).warn(anyString());
    }
}
