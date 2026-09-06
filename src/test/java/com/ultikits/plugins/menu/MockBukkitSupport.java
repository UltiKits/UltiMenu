package com.ultikits.plugins.menu;

import org.bukkit.Bukkit;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;

/**
 * Shared test-time Bukkit server bootstrap for this module's test suite (TEST-03 reopen guard).
 * <p>
 * Named {@code MockBukkitSupport} for consistency with the equivalent class in
 * {@code UltiBackup}/{@code UltiTrade}/{@code UltiRemoteBag}/{@code UltiWorlds} — see those
 * classes' javadoc for why that name and not {@code MockBukkitHelper}, which already collides
 * with four other classes across the monorepo.
 * <p>
 * UltiMenu has no per-plugin {@code *TestHelper} class analogous to those modules', so unlike
 * their {@code MockBukkitSupport} (a defensive-cleanup layer wrapped by a helper that itself owns
 * {@code MockBukkit.mock()}), this class owns the {@code MockBukkit.mock()}/{@code unmock()} call
 * directly. {@link UltiMenuRegistrySentinelTest} and {@code CustomMenuGuiTest} — the module's two
 * test classes that need a live Bukkit server — both call {@link #mock()}/{@link #unmock()}
 * instead of touching {@code MockBukkit} themselves, so this is the module's one shared
 * test-time bootstrap entry point: breaking it here is visible to both, and in particular to the
 * reopen-guard sentinel.
 */
public final class MockBukkitSupport {

    private MockBukkitSupport() {
    }

    /**
     * Call in {@code @BeforeEach}. Force-clears any stale MockBukkit/Bukkit singleton left behind
     * by a prior test's failed teardown, then boots a fresh live server.
     */
    public static ServerMock mock() {
        ensureCleanState();
        return MockBukkit.mock();
    }

    /**
     * Call in {@code @AfterEach}. Unmocks, tolerating exceptions, then force-clears again so the
     * next test class starts from a known-clean state regardless of how this test's teardown went.
     */
    public static void unmock() {
        try {
            MockBukkit.unmock();
        } catch (Exception ignored) {
            // best-effort cleanup only
        }
        ensureCleanState();
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration") // test helper requires reflection for singleton cleanup
    private static void ensureCleanState() {
        try {
            if (MockBukkit.isMocked()) {
                MockBukkit.unmock();
            }
        } catch (Exception ignored) {
            // best-effort cleanup only
        }

        try {
            Field mockedField = MockBukkit.class.getDeclaredField("mocked");
            mockedField.setAccessible(true);
            mockedField.setBoolean(null, false);
        } catch (Exception ignored) {
            // best-effort cleanup only
        }

        if (Bukkit.getServer() != null) {
            try {
                Field serverField = Bukkit.class.getDeclaredField("server");
                serverField.setAccessible(true);
                serverField.set(null, null);
            } catch (Exception ignored) {
                // best-effort cleanup only
            }
        }
    }
}
