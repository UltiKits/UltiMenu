package com.ultikits.plugins.menu.services;

import com.ultikits.plugins.menu.model.ButtonDefinition;
import com.ultikits.plugins.menu.model.MenuDefinition;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.ultikits.ultitools.annotations.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;
import java.util.stream.Collectors;

/**
 * Implementation of MenuService.
 * MenuService的实现
 */
@Service
public class MenuServiceImpl implements MenuService {
    private final UltiToolsPlugin plugin;
    private final PluginLogger logger;
    private final Map<String, MenuDefinition> menus = new HashMap<>();

    /**
     * The per-menu key removed by UltiKits/UltiMenu#12; see {@link #warnIfRemovedKeysPresent}.
     */
    private static final String REMOVED_COMMAND_KEY = "command";

    public MenuServiceImpl(UltiToolsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadMenus();
    }

    @Override
    public void loadMenus() {
        menus.clear();

        File menusFolder = new File(plugin.getResourceFolderPath(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            copyExampleMenu(menusFolder);
        }

        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.warn(plugin.i18n("menu.log.no_files"));
            return;
        }

        int loadedCount = 0;
        for (File file : files) {
            try {
                MenuDefinition menu = parseMenuFile(file);
                if (menu != null) {
                    String menuName = file.getName().replace(".yml", "").toLowerCase();
                    menu.setFileName(menuName);
                    menus.put(menuName, menu);
                    logger.info(String.format(plugin.i18n("menu.log.loaded"), menuName));
                    loadedCount++;
                }
            } catch (Exception e) {
                logger.warn(String.format(plugin.i18n("menu.log.load_failed"), file.getName(), e.getMessage()));
            }
        }

        logger.info(String.format(plugin.i18n("menu.log.loaded_count"), loadedCount));
    }

    @Override
    public void reload() {
        loadMenus();
    }

    @Nullable
    @Override
    public MenuDefinition getMenu(String name) {
        return menus.get(name.toLowerCase());
    }

    @Override
    public Collection<MenuDefinition> getAllMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    @Override
    public List<String> getMenuNames() {
        return new ArrayList<>(menus.keySet());
    }

    @Override
    public String getName() {
        return plugin.i18n("menu.service.name");
    }

    @Override
    public String getResourceFolderName() {
        return "menu";
    }

    @Override
    public String getAuthor() {
        return "wisdomme";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Copy example menu from resources to menus folder.
     * 从资源文件复制示例菜单到菜单文件夹
     */
    private void copyExampleMenu(File folder) {
        try (InputStream is = plugin.getClass().getClassLoader().getResourceAsStream("menus/example.yml")) {
            File exampleFile = new File(folder, "example.yml");
            if (is != null && !exampleFile.exists()) {
                Files.copy(is, exampleFile.toPath());
            }
        } catch (IOException e) {
            logger.warn(String.format(plugin.i18n("menu.log.copy_example_failed"), e.getMessage()));
        }
    }

    /**
     * Parse a menu definition from YAML file.
     * 从YAML文件解析菜单定义
     *
     * @param file the YAML file
     * @return parsed MenuDefinition or null if invalid
     */
    @Nullable
    private MenuDefinition parseMenuFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        // Before any validation, so a file that then fails to load is still reported.
        warnIfRemovedKeysPresent(config, file);

        // Validate size
        int size = config.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) {
            logger.warn(String.format(plugin.i18n("menu.log.invalid_size"), size, file.getName()));
            return null;
        }

        MenuDefinition menu = new MenuDefinition();
        menu.setSize(size);
        menu.setTitle(config.getString("title", plugin.i18n("menu.gui.default_title")));
        menu.setPermission(config.getString("permission"));

        // Parse bind-item
        String bindItemStr = config.getString("bind-item");
        if (bindItemStr != null) {
            Material bindMaterial = parseMaterial(bindItemStr);
            if (bindMaterial != null) {
                menu.setBindItem(bindMaterial);
            }
        }

        menu.setBindName(config.getString("bind-name"));
        menu.setBindLore(config.getString("bind-lore"));

        // Parse buttons
        ConfigurationSection buttonsSection = config.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            Map<String, ButtonDefinition> buttons = new HashMap<>();
            for (String buttonId : buttonsSection.getKeys(false)) {
                ButtonDefinition button = parseButton(buttonsSection.getConfigurationSection(buttonId), buttonId, file.getName());
                if (button != null) {
                    buttons.put(buttonId, button);
                }
            }
            menu.setButtons(buttons);
        } else {
            menu.setButtons(new HashMap<>());
        }

        return menu;
    }

    /**
     * Warns about a top-level key this module used to parse from a menu definition file and no
     * longer reads, when the operator's file still carries it.
     *
     * <p>Menu definition files are operator-authored: every {@code .yml} file directly inside this
     * module's {@code menus/} folder is its own menu, named by the operator. So there is no single
     * file to check once. The check runs on exactly the files {@link #loadMenus()} parses, at the
     * moment each is parsed &mdash; on every start and every {@code /menu reload} &mdash; and each
     * line names the file it was found in. It runs before validation, so a file that then fails to
     * load is reported too.
     *
     * <p>{@code command} was parsed and stored and read by nothing: no command was ever registered
     * from it (UltiKits/UltiMenu#12). Only the top-level key is reported, because only the
     * top-level key was ever read; a {@code command} key nested under a button is not this key.
     * A key written with no value or a YAML null ({@code command:}, {@code command: null},
     * {@code command: ~}) is not seen, because Bukkit's YAML loader drops such a key; it had
     * nothing to lose either, since it always read back exactly as an absent key.
     *
     * @param config the file as parsed
     * @param file   the file it was parsed from, named in the warning
     */
    private void warnIfRemovedKeysPresent(YamlConfiguration config, File file) {
        if (config.contains(REMOVED_COMMAND_KEY)) {
            logger.warn(String.format(plugin.i18n("menu.log.removed_command_key"), REMOVED_COMMAND_KEY, file.getPath()));
        }
    }

    /**
     * Parse a button definition from configuration section.
     * 从配置节解析按钮定义
     *
     * @param section the configuration section
     * @param buttonId the button ID
     * @param fileName the source file name (for logging)
     * @return parsed ButtonDefinition or null if invalid
     */
    @Nullable
    private ButtonDefinition parseButton(ConfigurationSection section, String buttonId, String fileName) {
        if (section == null) {
            return null;
        }

        // Parse item material (required)
        String itemStr = section.getString("item");
        if (itemStr == null) {
            logger.warn(String.format(plugin.i18n("menu.log.button_no_item"), buttonId, fileName));
            return null;
        }

        Material material = parseMaterial(itemStr);
        if (material == null) {
            logger.warn(String.format(plugin.i18n("menu.log.button_invalid_material"), buttonId, fileName, itemStr));
            return null;
        }

        ButtonDefinition button = new ButtonDefinition();
        button.setId(buttonId);
        button.setItem(material);
        button.setPosition(section.getInt("position", 0));
        button.setName(section.getString("name"));
        button.setLore(section.getStringList("lore"));
        button.setPlayerCommands(section.getStringList("player-commands"));
        button.setConsoleCommands(section.getStringList("console-commands"));
        button.setPrice(section.getDouble("price", 0.0));
        button.setOpenMenu(section.getString("open-menu"));
        button.setCloseOnClick(section.getBoolean("close-on-click", true));
        button.setPermission(section.getString("permission"));
        button.setCustomModelData(section.getInt("custom-model-data", 0));

        return button;
    }

    /**
     * Parse a Material from string, handling legacy names if needed.
     * 从字符串解析Material，必要时处理旧版名称
     *
     * @param materialName the material name
     * @return the Material or null if invalid
     */
    @Nullable
    private Material parseMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
