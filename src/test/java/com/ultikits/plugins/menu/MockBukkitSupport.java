package com.ultikits.plugins.menu;

import org.bukkit.Bukkit;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;

/**
 * Shared test-time Bukkit server bootstrap for this module's test suite.
 * <p>
 * Named {@code MockBukkitSupport} for consistency with the equivalent class in
 * {@code UltiBackup}/{@code UltiTrade}/{@code UltiRemoteBag}/{@code UltiWorlds} — see those
 * classes' javadoc for why that name and not {@code MockBukkitHelper}, which already collides
 * with four other classes across the monorepo.
 * <p>
 * UltiMenu has no per-plugin {@code *TestHelper} class analogous to those modules', so unlike
 * their {@code MockBukkitSupport} (a defensive-cleanup layer wrapped by a helper that itself owns
 * {@code MockBukkit.mock()}), this class owns the {@code MockBukkit.mock()}/{@code unmock()} call
 * directly. {@link UltiMenuRegistrySentinelTest} and {@code CustomMenuGuiTest} both call
 * {@link #mock()}/{@link #unmock()} instead of touching {@code MockBukkit} themselves, so this is
 * the module's one shared test-time bootstrap entry point: breaking it here is visible to both,
 * and in particular to the reopen-guard sentinel that exists to fail the build if this module
 * ever loses its live-server bootstrap again.
 * <p>
 * Only the sentinel actually requires the live server. Resolving a bare registry constant
 * ({@code Material.X}, {@code InventoryType.X}) needs nothing but the {@code mockbukkit-v1.21}
 * dependency on the test classpath, through the {@code java.util.ServiceLoader} providers that
 * jar registers for {@code io.papermc.paper.registry.RegistryAccess} and
 * {@code io.papermc.paper.ServerBuildInfo}; only constructing a real item
 * ({@code new ItemStack(...)}, real {@code ItemMeta}) goes through the server. Measured on
 * mockbukkit-v1.21 4.101.0 against paper-api 1.21.11: with no server bootstrapped,
 * {@code InventoryType.CHEST} resolves and {@code new ItemStack(Material.DIAMOND)} throws; with
 * the jar off the classpath, {@code InventoryType}'s static initialiser fails outright.
 * {@code CustomMenuGuiTest} only ever does the former — see its own comment for why it still
 * routes through here.
 */
public final class MockBukkitSupport {

    private MockBukkitSupport() {
    }

    /**
     * Call in {@code @BeforeEach}. Force-clears any stale {@code MockBukkit.mock} /
     * {@code Bukkit.server} singleton left behind by a prior test's failed teardown, then boots a
     * fresh live server. Without that clear, {@code MockBukkit.mock()} fails fast with
     * {@code IllegalStateException("Already mocking")}, and {@code Bukkit.setServer} with
     * {@code UnsupportedOperationException("Cannot redefine singleton Server")}.
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

    /**
     * Clear both singletons a subsequent {@code MockBukkit.mock()} refuses to overwrite:
     * {@code MockBukkit}'s own {@code mock} field and {@code Bukkit.server}.
     * <p>
     * {@code MockBukkit.unmock()} clears both itself on its happy path, via
     * {@code setServerInstanceToNull()}, so this is a guard for the failure path: that method's
     * exception handler covers the scheduler shutdown but not the
     * {@code PluginManagerMock.disablePlugins()} call before it, so a throw there returns with
     * both fields still set and every later {@code mock()} in the run then fails.
     * <p>
     * The field name is {@code mock} and its type is {@code ServerMock} — as of
     * mockbukkit-v1.21 4.101.0 it is the only field {@code MockBukkit} declares.
     */
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
            Field mockField = MockBukkit.class.getDeclaredField("mock");
            mockField.setAccessible(true);
            mockField.set(null, null);
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
