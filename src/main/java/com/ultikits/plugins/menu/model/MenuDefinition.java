package com.ultikits.plugins.menu.model;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import lombok.Data;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class MenuDefinition {
    private String fileName;
    private int size = 27;
    /** The title the menu file sets, or {@code null} when it sets none (see {@link #displayTitle}). */
    private String title = null;
    private String permission = null;
    private Material bindItem = null;
    private String bindName = null;
    private String bindLore = null;
    private Map<String, ButtonDefinition> buttons = new LinkedHashMap<>();

    /**
     * The title to show: the menu file's own, or, when it sets none, the language file's default
     * title in the language in force now. Resolved each time it is shown rather than when the file is
     * read, because {@code /ul reload} can switch the language without re-reading menu files.
     *
     * @param plugin the module, whose language catalogue gives the default title
     * @return the title to display, before colour codes and placeholders are applied
     */
    public String displayTitle(UltiToolsPlugin plugin) {
        return title != null ? title : plugin.i18n("menu.gui.default_title");
    }
}
