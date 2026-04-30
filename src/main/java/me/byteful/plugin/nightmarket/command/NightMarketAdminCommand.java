package me.byteful.plugin.nightmarket.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import me.byteful.plugin.nightmarket.shop.player.PlayerShop;
import me.byteful.plugin.nightmarket.util.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"nightmarketadmin", "adminnightmarket", "nmarketadmin", "nightmarket-admin"})
@CommandPermission("nightmarket.admin")
public class NightMarketAdminCommand {
    @Dependency
    private NightMarketPlugin plugin;

    @Subcommand("reload")
    @Description("Reloads the NightMarket configuration.")
    public void reload(CommandSender sender) {
        this.plugin.getParsedGUI().close(this.plugin.getTextProvider());
        this.plugin.getRotateScheduleManager().getScheduler().shutdownNow();
        try {
            this.plugin.getDataStoreProvider().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ConfigUpdater.update(this.plugin);
        this.plugin.reloadConfig();
        this.plugin.reloadMessages();
        this.plugin.reloadCurrencyManager();
        this.plugin.reloadRotateSchedules();
        this.plugin.reloadAccessSchedules();
        this.plugin.reloadParsedGUI();
        this.plugin.reloadShopItems();
        this.plugin.loadDataStore();
        this.plugin.reloadGlobalPurchaseTask();
        this.plugin.reloadUpdateChecker();
        this.plugin.sendMessage(sender, null, "reload_success");
    }

    @Subcommand("forcerotate")
    @Description("Forcefully rotate provided player's NightMarket shop.")
    public void forceRotate(CommandSender sender, Player player) {
        this.plugin.getPlayerShopManager().get(player).rotate(this.plugin, this.plugin.getShopItemRegistry(), player);
        this.plugin.sendMessage(sender, null, "force_rotated", "{player}", player.getName());
    }

    @Subcommand("forceglobalrotate")
    @Description("Forcefully rotates all shops.")
    public void forceGlobalRotate(CommandSender sender) {
        this.plugin.getPlayerShopManager().rotateShops();
        this.plugin.sendMessage(sender, null, "force_rotated_all");
    }

    @Subcommand("view")
    @Description("Opens a read-only view of a player's NightMarket shop.")
    public void view(CommandSender sender, String target) {
        if (!(sender instanceof Player viewer)) {
            this.plugin.sendMessage(sender, null, "player_only");
            return;
        }

        final Player onlineTarget = Bukkit.getPlayerExact(target);
        if (onlineTarget != null) {
            final PlayerShop shop = this.plugin.getPlayerShopManager().getLoaded(onlineTarget.getUniqueId());
            if (shop == null) {
                this.plugin.sendMessage(viewer, viewer, "admin_view_no_shop", "{player}", onlineTarget.getName());
                return;
            }
            this.plugin.getScheduler().runForPlayer(viewer,
                () -> this.plugin.getParsedGUI().buildReadOnly(shop, this.plugin, viewer, onlineTarget, onlineTarget.getName()).open(viewer));
            return;
        }

        final Optional<OfflinePlayer> resolvedTarget = this.resolveOfflineTarget(target);
        if (resolvedTarget.isEmpty()) {
            this.plugin.sendMessage(viewer, viewer, "admin_view_no_shop", "{player}", target);
            return;
        }

        final OfflinePlayer offlineTarget = resolvedTarget.get();
        final String displayName = offlineTarget.getName() == null ? offlineTarget.getUniqueId().toString() : offlineTarget.getName();
        this.plugin.sendMessage(viewer, viewer, "admin_view_loading", "{player}", displayName);
        this.plugin.getScheduler().runAsync(() -> {
            final Optional<PlayerShop> loaded = this.plugin.getDataStoreProvider().getPlayerShop(offlineTarget.getUniqueId());
            this.plugin.getScheduler().runForPlayer(viewer, () -> {
                if (loaded.isEmpty()) {
                    this.plugin.sendMessage(viewer, viewer, "admin_view_no_shop", "{player}", displayName);
                    return;
                }
                this.plugin.getParsedGUI().buildReadOnly(loaded.get(), this.plugin, viewer, null, displayName).open(viewer);
            });
        });
    }

    @Subcommand("debug")
    @Description("Provides debug information to give to support.")
    public void debug(CommandSender sender) {
        this.plugin.getUpdateChecker().check();
        sender.sendMessage("NightMarket Debug Information:");
        sender.sendMessage("- Server Version: " + Bukkit.getVersion());
        sender.sendMessage("- Server Type: " + Bukkit.getBukkitVersion());
        sender.sendMessage("- Plugin Version: " + this.plugin.getDescription().getVersion());
        sender.sendMessage("- Latest Version: " + this.plugin.getUpdateChecker().getLastCheckedVersion());
        sender.sendMessage("- DataStore Type: " + this.plugin.getDataStoreProvider().getClass().getSimpleName());
        sender.sendMessage("- Buyer: %%__USER__%%");
        sender.sendMessage("- Resource ID: %%__RESOURCE__%%");
        sender.sendMessage("- MC-Market?: %%__BUILTBYBIT__%%");
        sender.sendMessage(
            "{!} Please include your configuration with this when asking for help. You MAY OMIT credentials. Please COPY AND PASTE configuration into discord server. {!}");
    }

    private Optional<OfflinePlayer> resolveOfflineTarget(String target) {
        try {
            return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(target)));
        } catch (IllegalArgumentException ignored) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(target))
                .findFirst();
        }
    }
}
