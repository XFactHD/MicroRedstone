package io.github.xfacthd.microredstone.common.menu.slot;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class CircuitSlot extends Slot
{
    public CircuitSlot(MicrochipBlockEntity blockEntity, int x, int y)
    {
        super(new CircuitContainer(blockEntity), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        if (stack.is(MRContent.ITEM_INTEGRATED_CIRCUIT))
        {
            StoredCircuit circuit = stack.get(MRContent.DC_TYPE_CIRCUIT);
            return circuit != null && circuit.rootNode() != null;
        }
        return false;
    }

    private static final class CircuitContainer implements Container
    {
        private final MicrochipBlockEntity blockEntity;
        @Nullable
        private Circuit lastCircuit = null;
        @Nullable
        private ItemStack cachedStack = null;

        public CircuitContainer(MicrochipBlockEntity blockEntity)
        {
            this.blockEntity = blockEntity;
            getCircuitStack();
        }

        private ItemStack getCircuitStack()
        {
            Circuit circuit = blockEntity.getCircuit();
            if (circuit != lastCircuit)
            {
                lastCircuit = circuit;
                cachedStack = null;
            }
            if (cachedStack == null)
            {
                if (circuit != null)
                {
                    ItemStack stack = new ItemStack(MRContent.ITEM_INTEGRATED_CIRCUIT);
                    CompoundCircuitNode rootNode = circuit.getSerializableRootNode();
                    stack.set(MRContent.DC_TYPE_CIRCUIT, new StoredCircuit(rootNode));
                    cachedStack = stack;
                }
                else
                {
                    cachedStack = ItemStack.EMPTY;
                }
            }
            return cachedStack;
        }

        @Override
        public ItemStack getItem(int slot)
        {
            Objects.checkIndex(slot, 1);
            return getCircuitStack();
        }

        @Override
        public void setItem(int slot, ItemStack stack)
        {
            Objects.checkIndex(slot, 1);
            cachedStack = stack.copy();
            blockEntity.setCircuit(stack.getOrDefault(MRContent.DC_TYPE_CIRCUIT, StoredCircuit.EMPTY).toCircuit());
        }

        @Override
        public ItemStack removeItem(int slot, int amount)
        {
            Objects.checkIndex(slot, 1);
            if (amount <= 0) return ItemStack.EMPTY;

            ItemStack stack = getCircuitStack();
            if (!stack.isEmpty())
            {
                setItem(0, ItemStack.EMPTY);
                return stack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot)
        {
            Objects.checkIndex(slot, 1);

            ItemStack stack = getCircuitStack();
            setItem(0, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public boolean isEmpty()
        {
            return getCircuitStack().isEmpty();
        }

        @Override
        public int getContainerSize()
        {
            return 1;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }

        @Override
        public void setChanged() { }

        @Override
        public boolean stillValid(Player player)
        {
            return false;
        }

        @Override
        public void clearContent()
        {
            setItem(0, ItemStack.EMPTY);
        }
    }
}
