package io.github.xfacthd.microredstone.common.menu;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.WireStateListener;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundMicrochipChangeCircuitPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundMicrochipUpdateWireStatesPayload;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class MicrochipCircuitMenu extends AbstractContainerMenu implements WireStateListener
{
    public static final StreamCodec<ByteBuf, Optional<CompoundCircuitNode>> ROOT_NODE_CODEC = ByteBufCodecs.optional(CompoundCircuitNode.STREAM_CODEC);

    @Nullable
    private final MicrochipBlockEntity blockEntity;
    @Nullable
    private final ServerPlayer player;
    private final ContainerLevelAccess levelAccess;
    @Nullable
    private final CompoundCircuitNode initialRootNode;
    @Nullable
    private final String initialNodeClassName;
    @Nullable
    private Circuit lastCircuit;
    @Nullable
    private WireStates currStates = null;
    @Nullable
    private WireStates lastStates = null;

    public static MicrochipCircuitMenu createServer(int containerId, MicrochipBlockEntity blockEntity, ServerPlayer player)
    {
        Level level = Objects.requireNonNull(blockEntity.getLevel());
        ContainerLevelAccess levelAccess = ContainerLevelAccess.create(level, blockEntity.getBlockPos());
        return new MicrochipCircuitMenu(containerId, blockEntity, player, levelAccess, null, null);
    }

    public static MicrochipCircuitMenu createClient(int containerId, Inventory ignored, RegistryFriendlyByteBuf buffer)
    {
        ClientboundMicrochipChangeCircuitPayload payload = ClientboundMicrochipChangeCircuitPayload.STREAM_CODEC.decode(buffer);
        CompoundCircuitNode rootNode = payload.rootNode().orElse(null);
        String nodeClassName = payload.nodeClassName().orElse(null);
        return new MicrochipCircuitMenu(containerId, null, null, ContainerLevelAccess.NULL, rootNode, nodeClassName);
    }

    private MicrochipCircuitMenu(
            int containerId,
            @Nullable MicrochipBlockEntity blockEntity,
            @Nullable ServerPlayer player,
            ContainerLevelAccess levelAccess,
            @Nullable CompoundCircuitNode initialRootNode,
            @Nullable String initialNodeClassName
    )
    {
        super(MRContent.MENU_TYPE_MICROCHIP_CIRCUIT.value(), containerId);
        this.blockEntity = blockEntity;
        this.player = player;
        this.levelAccess = levelAccess;
        this.initialRootNode = initialRootNode;
        this.initialNodeClassName = initialNodeClassName;
        this.lastCircuit = blockEntity != null ? blockEntity.getCircuit() : null;
        if (blockEntity != null)
        {
            blockEntity.addWireStateListener(this);
        }
    }

    public void encodeInitialCircuit(ByteBuf buffer)
    {
        ClientboundMicrochipChangeCircuitPayload.STREAM_CODEC.encode(buffer, buildCircuitUpdate(lastCircuit));
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (blockEntity != null && player != null)
        {
            Circuit circuit = blockEntity.getCircuit();
            if (circuit != lastCircuit)
            {
                PacketDistributor.sendToPlayer(player, buildCircuitUpdate(circuit));
                lastCircuit = circuit;
                currStates = null;
                lastStates = null;
            }
            if (currStates != null && !currStates.equals(lastStates))
            {
                PacketDistributor.sendToPlayer(player, new ClientboundMicrochipUpdateWireStatesPayload(containerId, currStates));
                lastStates = currStates;
            }
        }
    }

    private ClientboundMicrochipChangeCircuitPayload buildCircuitUpdate(@Nullable Circuit circuit)
    {
        CompoundCircuitNode rootNode = circuit != null ? circuit.getSerializableRootNode() : null;
        String nodeClassName = null;
        if (!Utils.PRODUCTION && circuit != null && circuit.getRootNode() instanceof CompiledCircuitNode compiled)
        {
            nodeClassName = compiled.getClass().getSimpleName();
            // Strip unnecessary suffix appended to hidden classes
            nodeClassName = nodeClassName.substring(0, nodeClassName.indexOf('/'));
        }
        return new ClientboundMicrochipChangeCircuitPayload(containerId, rootNode, nodeClassName);
    }

    @Override
    public void handleWireStates(WireStates wireStates)
    {
        currStates = wireStates;
    }

    @Nullable
    public CompoundCircuitNode getInitialRootNode()
    {
        return initialRootNode;
    }

    @Nullable
    public String getInitialNodeClassName()
    {
        return initialNodeClassName;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(levelAccess, player, MRContent.BLOCK_MICROCHIP.value());
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (blockEntity != null)
        {
            blockEntity.removeWireStateListener(this);
        }
    }
}
