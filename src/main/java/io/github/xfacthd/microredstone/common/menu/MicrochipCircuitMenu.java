package io.github.xfacthd.microredstone.common.menu;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundMicrochipChangeCircuitPayload;
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

public final class MicrochipCircuitMenu extends AbstractContainerMenu
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
    private Circuit lastCircuit;

    public static MicrochipCircuitMenu createServer(int containerId, MicrochipBlockEntity blockEntity, ServerPlayer player)
    {
        Level level = Objects.requireNonNull(blockEntity.getLevel());
        ContainerLevelAccess levelAccess = ContainerLevelAccess.create(level, blockEntity.getBlockPos());
        return new MicrochipCircuitMenu(containerId, blockEntity, player, levelAccess, null);
    }

    public static MicrochipCircuitMenu createClient(int containerId, Inventory ignored, RegistryFriendlyByteBuf buffer)
    {
        CompoundCircuitNode rootNode = ROOT_NODE_CODEC.decode(buffer).orElse(null);
        return new MicrochipCircuitMenu(containerId, null, null, ContainerLevelAccess.NULL, rootNode);
    }

    private MicrochipCircuitMenu(
            int containerId,
            @Nullable MicrochipBlockEntity blockEntity,
            @Nullable ServerPlayer player,
            ContainerLevelAccess levelAccess,
            @Nullable CompoundCircuitNode initialRootNode
    )
    {
        super(MRContent.MENU_TYPE_MICROCHIP_CIRCUIT.value(), containerId);
        this.blockEntity = blockEntity;
        this.player = player;
        this.levelAccess = levelAccess;
        this.initialRootNode = initialRootNode;
        this.lastCircuit = blockEntity != null ? blockEntity.getCircuit() : null;
    }

    public void encodeInitialCircuit(ByteBuf buffer)
    {
        CompoundCircuitNode rootNode = lastCircuit != null ? lastCircuit.getSerializableRootNode() : null;
        MicrochipCircuitMenu.ROOT_NODE_CODEC.encode(buffer, Optional.ofNullable(rootNode));
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
                CompoundCircuitNode rootNode = circuit != null ? circuit.getSerializableRootNode() : null;
                PacketDistributor.sendToPlayer(player, new ClientboundMicrochipChangeCircuitPayload(containerId, rootNode));
                lastCircuit = circuit;
            }
        }
    }

    @Nullable
    public CompoundCircuitNode getInitialRootNode()
    {
        return initialRootNode;
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
}
