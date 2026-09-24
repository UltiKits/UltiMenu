package com.ultikits.plugins.menu.i18n;

import com.ultikits.plugins.menu.commands.MenuCommands;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.plugins.menu.services.MenuService;
import com.ultikits.plugins.menu.services.MenuServiceImpl;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.Invocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Text this module shows to players and writes to the console follows the framework's
 * {@code language} setting.
 * <p>
 * The module's {@code i18n} answers from the catalogue this module really ships ({@link CatalogueText}),
 * so each test sees what an operator sees. Before the language sweep the help lines, the console-sender
 * refusal of {@code /menu <name>} and the command description had no English text (UltiKits/UltiMenu#11
 * names the last two), and the menu-file console warnings were fixed English text.
 */
@DisplayName("UltiMenu text follows the language setting")
class MenuTextLanguageTest {

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    /** The catalogue line with its arguments filled in, or a marker naming the missing key. */
    private static String text(String code, String key, Object... args) {
        String value = CatalogueText.entries(code).get(key);
        return value == null ? "<lang/" + code + " has no " + key + ">" : String.format(value, args);
    }

    private static UltiToolsPlugin pluginSpeaking(String code) {
        UltiToolsPlugin plugin = mock(UltiToolsPlugin.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer(code));
        return plugin;
    }

    private static List<String> sent(CommandSender sender) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues();
    }

    @Nested
    @DisplayName("/menu under language: en (UltiKits/UltiMenu#11)")
    class CommandsInEnglish {

        @Test
        @DisplayName("a console sender running /menu <name> is refused in English")
        void consoleQuickOpenRefusalIsEnglish() {
            MenuCommands commands = new MenuCommands(pluginSpeaking("en"), mock(MenuService.class));
            CommandSender console = mock(CommandSender.class);

            commands.onQuickOpen(console, "example");

            assertThat(sent(console)).containsExactly(ChatColor.RED + "Only players can open menus!");
        }

        @Test
        @DisplayName("the /menu command description the framework translates has English text")
        void commandDescriptionIsEnglish() {
            String description = MenuCommands.class.getAnnotation(CmdExecutor.class).description();

            assertThat(text("en", description)).isEqualTo("Menu management command");
        }

        @Test
        @DisplayName("/menu help is English")
        void helpIsEnglish() throws Exception {
            MenuCommands commands = new MenuCommands(pluginSpeaking("en"), mock(MenuService.class));
            CommandSender sender = mock(CommandSender.class);
            Method help = MenuCommands.class.getDeclaredMethod("handleHelp", CommandSender.class);
            help.setAccessible(true);

            help.invoke(commands, sender);

            assertThat(sent(sender)).containsExactly(
                    ChatColor.GOLD + "=== UltiMenu Command Help ===",
                    ChatColor.AQUA + "/menu <name>" + ChatColor.WHITE + " - Open the named menu",
                    ChatColor.AQUA + "/menu open <name>" + ChatColor.WHITE + " - Open the named menu (explicit form)",
                    ChatColor.AQUA + "/menu list" + ChatColor.WHITE + " - List all available menus",
                    ChatColor.AQUA + "/menu reload" + ChatColor.WHITE
                            + " - Reload the menu configuration (requires admin permission)");
            assertThat(sent(sender)).noneMatch(line -> CJK.matcher(line).find());
        }
    }

    @Nested
    @DisplayName("Menu loading under language: zh")
    class LoadingInChinese {

        @TempDir
        Path tempDir;

        private PluginLogger logger;
        private File menusFolder;

        private MenuServiceImpl load(String fileName, String content) throws IOException {
            UltiToolsPlugin plugin = pluginSpeaking("zh");
            logger = mock(PluginLogger.class);
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getResourceFolderPath()).thenReturn(tempDir.toFile().getAbsolutePath());
            menusFolder = new File(tempDir.toFile(), "menus");
            menusFolder.mkdirs();
            try (FileWriter writer = new FileWriter(new File(menusFolder, fileName))) {
                writer.write(content);
            }
            return new MenuServiceImpl(plugin);
        }

        private List<String> logged(String level) {
            List<String> out = new ArrayList<>();
            for (Invocation invocation : mockingDetails(logger).getInvocations()) {
                if (level.equals(invocation.getMethod().getName())) {
                    for (Object argument : invocation.getArguments()) {
                        if (argument instanceof String) {
                            out.add((String) argument);
                        }
                    }
                }
            }
            return out;
        }

        @Test
        @DisplayName("an invalid menu size is reported in Chinese")
        void invalidSize() throws IOException {
            load("bad.yml", "title: Bad\nsize: 10\nbuttons: {}");

            assertThat(logged("warn")).contains(text("zh", "menu.log.invalid_size", 10, "bad.yml"));
        }

        @Test
        @DisplayName("a button with no item is reported in Chinese")
        void buttonWithoutItem() throws IOException {
            load("m.yml", "title: M\nsize: 9\nbuttons:\n  b1:\n    position: 1\n");

            assertThat(logged("warn")).contains(text("zh", "menu.log.button_no_item", "b1", "m.yml"));
        }

        @Test
        @DisplayName("a button with an unknown material is reported in Chinese")
        void buttonWithInvalidMaterial() throws IOException {
            load("m.yml", "title: M\nsize: 9\nbuttons:\n  b1:\n    item: NOT_A_MATERIAL\n");

            assertThat(logged("warn")).contains(
                    text("zh", "menu.log.button_invalid_material", "b1", "m.yml", "NOT_A_MATERIAL"));
        }

        @Test
        @DisplayName("a leftover command key is reported in Chinese, still naming the key and the file")
        void removedCommandKey() throws IOException {
            load("legacy.yml", "title: Legacy\nsize: 9\ncommand: servermenu\nbuttons: {}");
            String path = new File(menusFolder, "legacy.yml").getPath();

            assertThat(logged("warn")).containsExactly(text("zh", "menu.log.removed_command_key", "command", path));
        }

        @Test
        @DisplayName("loaded menus and the load count are reported in Chinese")
        void loadedLines() throws IOException {
            load("m.yml", "title: M\nsize: 9\nbuttons: {}");

            assertThat(logged("info")).containsExactly(
                    text("zh", "menu.log.loaded", "m"),
                    text("zh", "menu.log.loaded_count", 1));
        }

        @Test
        @DisplayName("a menu file with no title gets the Chinese default title")
        void defaultTitle() throws IOException {
            MenuServiceImpl service = load("m.yml", "size: 9\nbuttons: {}");

            MenuDefinition menu = service.getMenu("m");
            assertThat(menu.getTitle()).isEqualTo(text("zh", "menu.gui.default_title"));
        }
    }

    @Test
    @DisplayName("the menu service's display name follows the language setting")
    void serviceNameIsTranslated(@TempDir Path tempDir) {
        UltiToolsPlugin plugin = pluginSpeaking("en");
        when(plugin.getLogger()).thenReturn(mock(PluginLogger.class));
        when(plugin.getResourceFolderPath()).thenReturn(tempDir.toFile().getAbsolutePath());

        assertThat(new MenuServiceImpl(plugin).getName()).isEqualTo("Custom Menu");
    }
}
