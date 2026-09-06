package com.ultikits.plugins.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reopen guard (TEST-03): fails the build the moment this module's tests stop being able to
 * reach a live Bukkit registry, even though the {@code mockbukkit-v1.21} dependency by itself
 * (via its {@code java.util.ServiceLoader}-registered {@code RegistryAccess} provider) makes a
 * bare registry constant resolve regardless of whether a live server was ever bootstrapped.
 * <p>
 * Every assertion below therefore depends on the <em>live server</em> path, not the
 * ServiceLoader-only path.
 * <p>
 * Deliberately bootstraps through {@link MockBukkitSupport#mock()}/{@link MockBukkitSupport#unmock()}
 * — the module's one shared test-time bootstrap entry point, also used by {@code CustomMenuGuiTest}
 * — rather than calling {@code MockBukkit.mock()} itself. A sentinel that mocks its own unrelated
 * live server stays green even after the shared bootstrap is removed or broken, which defeats the
 * reopen guard this class exists to provide.
 */
public class UltiMenuRegistrySentinelTest {

    @BeforeEach
    void setUp() {
        MockBukkitSupport.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkitSupport.unmock();
    }

    @Test
    void liveServerIsBootstrapped() {
        assertNotNull(Bukkit.getServer(), "live server bootstrap must be present");
    }

    @Test
    void unsafeValuesResolves() {
        assertNotNull(Bukkit.getUnsafe(), "UnsafeValues must resolve on a live server");
    }

    @Test
    void createProfileDoesNotSilentlyReturnNull() {
        Object profile = Bukkit.createProfile(UUID.randomUUID(), "SentinelPlayer");
        assertNotNull(profile, "createProfile must not silently return null");
    }

    @Test
    void itemStackConstructionResolvesRegistry() {
        ItemStack stack = new ItemStack(Material.DIAMOND);
        assertNotNull(stack);
        assertEquals(Material.DIAMOND, stack.getType());
    }
}
