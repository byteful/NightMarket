package me.byteful.plugin.nightmarket.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NightMarketGui implements Listener {
    private static final int COLUMNS = 9;

    @NotNull
    private final NightMarketPlugin plugin;
    @NotNull
    private final Inventory inventory;
    @NotNull
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();
    private final int rows;
    @Nullable
    private Consumer<InventoryCloseEvent> closeHandler;
    @Nullable
    private Consumer<InventoryOpenEvent> openHandler;

    public NightMarketGui(@NotNull NightMarketPlugin plugin, int rows, @NotNull String title) {
        this.plugin = plugin;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(null, rows * COLUMNS, title);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setItem(int slot, @NotNull ItemStack item) {
        this.setItem(slot, item, null);
    }

    public void setItem(int slot, @NotNull ItemStack item, @Nullable Consumer<InventoryClickEvent> clickHandler) {
        this.validateSlot(slot);
        this.inventory.setItem(slot, item);
        if (clickHandler == null) {
            this.clickHandlers.remove(slot);
            return;
        }
        this.clickHandlers.put(slot, clickHandler);
    }

    public void clear() {
        this.inventory.clear();
        this.clickHandlers.clear();
    }

    public void update() {
    }

    public void open(@NotNull Player player) {
        this.plugin.getScheduler().runForPlayer(player, () -> player.openInventory(this.inventory));
    }

    public int getViewerCount() {
        return this.inventory.getViewers().size();
    }

    public void setOnClose(@Nullable Consumer<InventoryCloseEvent> closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void setOnOpen(@Nullable Consumer<InventoryOpenEvent> openHandler) {
        this.openHandler = openHandler;
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!this.isOwnInventory(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= this.inventory.getSize()) {
            return;
        }

        final Consumer<InventoryClickEvent> clickHandler = this.clickHandlers.get(event.getRawSlot());
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (this.isOwnInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!this.isOwnInventory(event.getView().getTopInventory())) {
            return;
        }

        if (this.closeHandler != null) {
            this.closeHandler.accept(event);
        }

        if (this.inventory.getViewers().size() <= 1) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (!this.isOwnInventory(event.getView().getTopInventory()) || this.openHandler == null) {
            return;
        }

        this.openHandler.accept(event);
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= this.rows * COLUMNS) {
            throw new IllegalArgumentException("GUI slot must be inside the inventory: " + slot);
        }
    }

    private boolean isOwnInventory(@NotNull Inventory inventory) {
        return inventory == this.inventory;
    }
}
