package io.github.xfacthd.microredstone.common.menu;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.menu.slot.CircuitSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class MicrochipMenu extends AbstractContainerMenu
{
    private static final int CIRCUIT_SLOT_X = 80;
    private static final int CIRCUIT_SLOT_Y = 35;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int CIRCUIT_SLOT_COUNT = 1;

    private final ContainerLevelAccess levelAccess;
    private final Slot circuitSlot;

    public static MicrochipMenu createServer(int containerId, Inventory inventory, MicrochipBlockEntity blockEntity)
    {
        Level level = Objects.requireNonNull(blockEntity.getLevel());
        ContainerLevelAccess levelAccess = ContainerLevelAccess.create(level, blockEntity.getBlockPos());
        return new MicrochipMenu(containerId, inventory, levelAccess, CircuitSlotFactory.server(blockEntity));
    }

    public static MicrochipMenu createClient(int containerId, Inventory inventory)
    {
        return new MicrochipMenu(containerId, inventory, ContainerLevelAccess.NULL, CircuitSlotFactory.CLIENT);
    }

    private MicrochipMenu(int containerId, Inventory inventory, ContainerLevelAccess levelAccess, CircuitSlotFactory circuitSlotFactory)
    {
        super(MRContent.MENU_TYPE_MICROCHIP.value(), containerId);
        this.levelAccess = levelAccess;
        this.circuitSlot = addSlot(circuitSlotFactory.create(CIRCUIT_SLOT_X, CIRCUIT_SLOT_Y));
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack remainder = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            remainder = stack.copy();
            if (index < CIRCUIT_SLOT_COUNT)
            {
                if (!moveItemStackTo(stack, CIRCUIT_SLOT_COUNT, slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!moveItemStackTo(stack, 0, CIRCUIT_SLOT_COUNT, false))
            {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
        }

        return remainder;
    }

    public Slot getCircuitSlot()
    {
        return circuitSlot;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(levelAccess, player, MRContent.BLOCK_MICROCHIP.value());
    }

    @FunctionalInterface
    private interface CircuitSlotFactory
    {
        CircuitSlotFactory CLIENT = (x, y) -> new Slot(new SingleItemContainer(), 0, x, y);

        static CircuitSlotFactory server(MicrochipBlockEntity blockEntity)
        {
            return (x, y) -> new CircuitSlot(blockEntity, x, y);
        }

        Slot create(int x, int y);
    }

    private static final class SingleItemContainer extends SimpleContainer
    {
        public SingleItemContainer()
        {
            super(1);
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }
}
