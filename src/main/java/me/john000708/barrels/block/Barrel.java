package me.john000708.barrels.block;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.john000708.barrels.Barrels;
import me.john000708.barrels.DisplayItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Created by John on 06.05.2016.
 * <p>
 * Ported to Slimefun United: the legacy {@code Config}-based {@link BlockTicker#tick}
 * is replaced by the {@link SlimefunBlockData} variant. All block data access goes
 * through the static {@link BlockStorage} facade, which forwards to the
 * database-backed storage in Slimefun United.
 */
public abstract class Barrel extends SimpleSlimefunItem<BlockTicker> {

    private final int capacity;

    protected Barrel(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int capacity) {
        super(itemGroup, item, recipeType, recipe);

        this.capacity = capacity;

        new BarrelsMenuPreset(this);
        addItemHandler(new BarrelsBlockHandler(this));

        addItemHandler(new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Block block = e.getBlock();
                BlockStorage.addBlockInfo(block, "owner", e.getPlayer().getUniqueId().toString());
                BlockStorage.addBlockInfo(block, "whitelist", " ");
            }
        });
    }

    public abstract String getInventoryTitle();

    @Override
    public BlockTicker getItemHandler() {
        return new BlockTicker() {

            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem slimefunItem, SlimefunBlockData data) {
                updateBarrel(block);

                if (Barrels.displayItem() && !block.isEmpty()) {
                    boolean hasRoom = block.getRelative(BlockFace.UP).isEmpty();
                    DisplayItem.updateDisplayItem(block, getCapacity(block), hasRoom);
                }
            }
        };
    }

    public int getCapacity(Block b) {
        if (BlockStorage.getLocationInfo(b.getLocation(), "capacity") == null) {
            BlockStorage.addBlockInfo(b, "capacity", String.valueOf(this.capacity));
        }

        // There's no need to box the integer.
        return Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "capacity"));
    }

    public int[] getInputSlots() {
        return new int[] { 10 };
    }

    public int[] getOutputSlots() {
        return new int[] { 16 };
    }

    private ItemStack getCapacityItem(Block b) {
        StringBuilder bar = new StringBuilder();

        // There's no need to box the integer.
        int storedItems = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "storedItems"));
        float percentage = Math.round((float) storedItems / (float) getCapacity(b) * 100.0F);

        bar.append("&8[");

        if (percentage < 25) {
            bar.append("&2");
        } else if (percentage < 50) {
            bar.append("&a");
        } else if (percentage < 75) {
            bar.append("&e");
        } else {
            bar.append("&c");
        }

        int lines = 20;

        for (int i = (int) percentage; i >= 5; i -= 5) {
            bar.append(":");
            lines--;
        }

        bar.append("&7");

        for (int i = 0; i < lines; i++) {
            bar.append(":");
        }

        bar.append("&8] &7- " + percentage + "%");

        return new CustomItemStack(Material.CAULDRON, "&7" + BlockStorage.getLocationInfo(b.getLocation(), "storedItems") + "/" + getCapacity(b), org.bukkit.ChatColor.translateAlternateColorCodes('&', bar.toString()));
    }

    /**
     * Adds the applied upgrade modules of the given barrel to the break drops,
     * so breaking a barrel never deletes installed modules.
     *
     * @param b
     *            The barrel {@link Block}
     * @param drops
     *            The drops list to append the module items to
     */
    void collectModuleDrops(Block b, java.util.List<ItemStack> drops) {
        if (BlockStorage.getLocationInfo(b.getLocation(), "explosion") != null) {
            drops.add(Barrels.explosionModule.clone());
        }

        if (BlockStorage.getLocationInfo(b.getLocation(), "STRUCT_1") != null) {
            drops.add(Barrels.structUpgrade1.clone());
        }

        if (BlockStorage.getLocationInfo(b.getLocation(), "STRUCT_2") != null) {
            drops.add(Barrels.structUpgrade2.clone());
        }

        if (BlockStorage.getLocationInfo(b.getLocation(), "STRUCT_3") != null) {
            drops.add(Barrels.structUpgrade3.clone());
        }

        if (BlockStorage.getLocationInfo(b.getLocation(), "protected") != null) {
            drops.add(Barrels.biometricProtectionModule.clone());
        }
    }

    /**
     * Adds the loose input/output slot items of the given barrel menu to the
     * break drops.
     *
     * @param b
     *            The barrel {@link Block}
     * @param inventory
     *            The barrel {@link BlockMenu}
     * @param drops
     *            The drops list to append the loose items to
     */
    void collectLooseSlotDrops(Block b, BlockMenu inventory, java.util.List<ItemStack> drops) {
        for (int slot : getInputSlots()) {
            ItemStack input = inventory.getItemInSlot(slot);

            if (input != null && input.getType() != Material.AIR) {
                drops.add(input.clone());
            }
        }

        for (int slot : getOutputSlots()) {
            ItemStack output = inventory.getItemInSlot(slot);

            if (output != null && output.getType() != Material.AIR) {
                drops.add(output.clone());
            }
        }
    }

    /**
     * Drops the virtually stored contents of the given barrel into the world,
     * one stack at a time, so breaking a filled barrel never loses items.
     *
     * @param b
     *            The barrel {@link Block}
     * @param inventory
     *            The barrel {@link BlockMenu}
     */
    void dropStoredContents(Block b, BlockMenu inventory) {
        if (BlockStorage.getLocationInfo(b.getLocation(), "storedItems") == null) {
            return;
        }

        int storedAmount = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "storedItems"));
        ItemStack output = inventory.getItemInSlot(22).clone();

        while (storedAmount > 0) {
            output.setAmount(Math.min(storedAmount, output.getMaxStackSize()));
            storedAmount -= output.getAmount();
            b.getWorld().dropItemNaturally(b.getLocation(), output.clone());
        }
    }

    void updateBarrel(Block b) {
        BlockMenu inventory = BlockStorage.getInventory(b);

        if (inventory == null) {
            return;
        }

        for (int slot : getInputSlots()) {
            if (inventory.getItemInSlot(slot) != null) {
                ItemStack input = inventory.getItemInSlot(slot);

                if (SlimefunUtils.isItemSimilar(input, inventory.getItemInSlot(22), true, false)) {
                    if (BlockStorage.getLocationInfo(b.getLocation(), "storedItems") == null) {
                        BlockStorage.addBlockInfo(b, "storedItems", "1");
                    }

                    int storedAmount = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "storedItems"));

                    if (storedAmount < getCapacity(b)) {
                        if (storedAmount + input.getAmount() > getCapacity(b)) {
                            BlockStorage.addBlockInfo(b, "storedItems", String.valueOf(getCapacity(b)));
                            inventory.replaceExistingItem(slot, decreaseItem(inventory.getItemInSlot(slot), getCapacity(b) - storedAmount), false);
                            inventory.replaceExistingItem(4, getCapacityItem(b), false);
                        } else {
                            BlockStorage.addBlockInfo(b, "storedItems", String.valueOf(storedAmount + input.getAmount()));
                            inventory.replaceExistingItem(slot, new ItemStack(Material.AIR), false);
                            inventory.replaceExistingItem(4, getCapacityItem(b), false);
                        }
                    }
                } else if (inventory.getItemInSlot(22).getType() == Material.BARRIER) {
                    BlockStorage.addBlockInfo(b, "storedItems", String.valueOf(input.getAmount()));

                    input.setAmount(input.getMaxStackSize());
                    inventory.replaceExistingItem(22, input, false);
                    inventory.replaceExistingItem(slot, new ItemStack(Material.AIR), false);
                    inventory.replaceExistingItem(4, getCapacityItem(b), false);
                }
            }
        }

        if (BlockStorage.getLocationInfo(b.getLocation(), "storedItems") == null) {
            return;
        }

        int stored = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "storedItems"));
        ItemStack output = inventory.getItemInSlot(22).clone();

        if (inventory.getItemInSlot(getOutputSlots()[0]) != null && inventory.getItemInSlot(getOutputSlots()[0]).getType() != Material.AIR) {
            if (!SlimefunUtils.isItemSimilar(inventory.getItemInSlot(getOutputSlots()[0]), output, true, false)) {
                return;
            }

            int requested = output.getMaxStackSize() - inventory.getItemInSlot(getOutputSlots()[0]).getAmount();
            output.setAmount(Math.min(stored, requested));
        } else {
            output.setAmount(Math.min(stored, output.getMaxStackSize()));
        }

        if (!inventory.fits(output, getOutputSlots())) {
            return;
        }

        BlockStorage.addBlockInfo(b, "storedItems", String.valueOf(stored - output.getAmount()));
        inventory.pushItem(output, getOutputSlots());

        if ((stored - output.getAmount()) <= 0) {
            BlockStorage.addBlockInfo(b, "storedItems", null);
            inventory.replaceExistingItem(4, new CustomItemStack(Material.BARRIER, "&7Empty"), false);
            inventory.replaceExistingItem(22, new CustomItemStack(Material.BARRIER, "&7Empty"), false);
            return;
        }

        inventory.replaceExistingItem(4, getCapacityItem(b), false);
    }

    /**
     * Decreases the size of the given {@link ItemStack} by the specified amount.
     * <p>
     * This replaces the removed CS-CoreLib {@code InvUtils.decreaseItem} helper
     * with identical semantics: the returned stack holds {@code amount} items
     * fewer, or air when nothing would remain.
     *
     * @param item
     *            The {@link ItemStack} to shrink (not modified)
     * @param amount
     *            The number of items to remove
     *
     * @return The leftover stack, or air when nothing remains
     */
    private static ItemStack decreaseItem(ItemStack item, int amount) {
        if (item == null) {
            return new ItemStack(Material.AIR);
        }

        int remaining = item.getAmount() - amount;

        if (remaining <= 0) {
            return new ItemStack(Material.AIR);
        }

        ItemStack clone = item.clone();
        clone.setAmount(remaining);
        return clone;
    }
}
