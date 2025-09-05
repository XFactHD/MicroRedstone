package io.github.xfacthd.microredstone.common.menu;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitValidator;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.menu.slot.ToggleableSlot;
import io.github.xfacthd.microredstone.common.menu.slot.WorkbenchCircuitSlot;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundWorkbenchWriteCircuitResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CircuitWorkbenchMenu extends AbstractContainerMenu
{
    private final ContainerLevelAccess levelAccess;
    private final Slot circuitSlot;

    public static CircuitWorkbenchMenu createServer(int containerId, Inventory inventory, BlockPos pos)
    {
        ContainerLevelAccess levelAccess = ContainerLevelAccess.create(inventory.player.level(), pos);
        return new CircuitWorkbenchMenu(containerId, inventory, levelAccess);
    }

    public static CircuitWorkbenchMenu createClient(int containerId, Inventory inventory)
    {
        return new CircuitWorkbenchMenu(containerId, inventory, ContainerLevelAccess.NULL);
    }

    private CircuitWorkbenchMenu(int containerId, Inventory inventory, ContainerLevelAccess levelAccess)
    {
        super(MRContent.MENU_TYPE_CIRCUIT_WORKBENCH.value(), containerId);
        this.levelAccess = levelAccess;
        // Slot positions are configured by the screen
        this.circuitSlot = addSlot(new WorkbenchCircuitSlot(new SimpleContainer(1), 0, 0, 0));
        for (int idx = 0; idx < 4 * 9; idx++)
        {
            addSlot(new ToggleableSlot(inventory, idx, 0, 0));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return null;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(levelAccess, player, MRContent.BLOCK_CIRCUIT_WORKBENCH.value());
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        levelAccess.execute((level, pos) -> clearContainer(player, circuitSlot.container));
    }

    public Slot getCircuitSlot()
    {
        return circuitSlot;
    }

    public ClientboundWorkbenchWriteCircuitResultPayload applyCircuitToItem(String name, CompoundCircuitNode circuitNode)
    {
        if (!CircuitValidator.validate(circuitNode))
        {
            return result(false);
        }
        if (!circuitSlot.hasItem())
        {
            return result(false);
        }

        StoredCircuit circuit = new StoredCircuit(name, circuitNode);
        circuitSlot.getItem().set(MRContent.DC_TYPE_CIRCUIT, circuit);
        return result(true);
    }

    private ClientboundWorkbenchWriteCircuitResultPayload result(boolean success)
    {
        return new ClientboundWorkbenchWriteCircuitResultPayload(containerId, success);
    }
}
