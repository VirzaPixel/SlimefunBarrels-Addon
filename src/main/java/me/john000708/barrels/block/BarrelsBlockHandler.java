package me.john000708.barrels.block;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import me.john000708.barrels.DisplayItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Handles breaking (and exploding) of barrels on Slimefun United.
 * <p>
 * The legacy {@code SlimefunBlockHandler}/{@code UnregisterReason} API no longer
 * exists, so this is now a {@link BlockBreakHandler} registered via
 * {@link SlimefunItem#addItemHandler}:
 * <ul>
 * <li>Player breaks: only the owner may break the barrel (legacy rule). The
 * barrel's stored contents, loose input/output items and applied upgrade
 * modules are added to the event drops so nothing is lost. Returning without
 * cancelling lets Slimefun remove the block data and drop the barrel item
 * itself afterwards.</li>
 * <li>Explosions: {@link #isExplosionAllowed(Block)} mirrors the legacy
 * explosion-protection module - a barrel carrying the {@code explosion} flag
 * can never be destroyed by an explosion. Unprotected barrels drop the same
 * contents via {@link #onExplode(Block, List)}.</li>
 * </ul>
 */
class BarrelsBlockHandler extends BlockBreakHandler {

    private final Barrel barrel;

    BarrelsBlockHandler(Barrel barrel) {
        // Legacy semantics: androids never break barrels; explosions are decided
        // per-block in isExplosionAllowed() (explosion-protection module).
        super(false, true);
        this.barrel = barrel;
    }

    @Override
    public boolean isExplosionAllowed(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "explosion") == null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
        Block b = e.getBlock();
        String owner = BlockStorage.getLocationInfo(b.getLocation(), "owner");

        // Only the Owner may break this Barrel (legacy rule).
        if (owner != null && !owner.equals(e.getPlayer().getUniqueId().toString())) {
            e.setCancelled(true);
            return;
        }

        DisplayItem.removeDisplayItem(b);

        BlockMenu inv = BlockStorage.getInventory(b);

        if (inv == null) {
            return;
        }

        barrel.collectModuleDrops(b, drops);
        barrel.collectLooseSlotDrops(b, inv, drops);
        barrel.dropStoredContents(b, inv);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onExplode(Block b, List<ItemStack> drops) {
        DisplayItem.removeDisplayItem(b);

        BlockMenu inv = BlockStorage.getInventory(b);

        if (inv == null) {
            return;
        }

        barrel.collectModuleDrops(b, drops);
        barrel.collectLooseSlotDrops(b, inv, drops);
        barrel.dropStoredContents(b, inv);
    }
}

