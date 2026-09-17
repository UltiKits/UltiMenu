package com.ultikits.plugins.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the lifecycle template-method contract of the module main class.
 */
@DisplayName("PluginMain lifecycle template methods (UltiKits/UltiMenu#16)")
class PluginMainTest {

    /**
     * UltiTools 6.3.0 makes {@code unregisterSelf()} and {@code reloadSelf()} final template
     * methods that always run the framework's own steps (command and listener unregistration on
     * unload; config reload and language refresh on reload) around the module's
     * {@code onUnregister()}/{@code onReload()} hooks. This module's former
     * {@code unregisterSelf()} override was empty and never called {@code super}, so the
     * framework's command unregistration never ran for it (UltiKits/UltiMenu#16). It is deleted
     * outright; this test pins that neither template method is declared again.
     */
    @Test
    @DisplayName("PluginMain declares neither framework template method")
    void declaresNeitherTemplateMethod() {
        List<String> declared = new ArrayList<>();
        for (Method method : PluginMain.class.getDeclaredMethods()) {
            declared.add(method.getName());
        }

        assertThat(declared).doesNotContain("unregisterSelf", "reloadSelf");
    }
}
