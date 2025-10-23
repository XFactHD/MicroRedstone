package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.menu.MicrochipCircuitMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ClientboundMicrochipChangeCircuitPayload(int containerId, Optional<CompoundCircuitNode> rootNode, Optional<String> nodeClassName) implements CustomPacketPayload
{
    public static final Type<ClientboundMicrochipChangeCircuitPayload> TYPE = Utils.payloadType("clientbound_microchip_change_circuit");
    public static final StreamCodec<ByteBuf, ClientboundMicrochipChangeCircuitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundMicrochipChangeCircuitPayload::containerId,
            MicrochipCircuitMenu.ROOT_NODE_CODEC,
            ClientboundMicrochipChangeCircuitPayload::rootNode,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            ClientboundMicrochipChangeCircuitPayload::nodeClassName,
            ClientboundMicrochipChangeCircuitPayload::new
    );

    public ClientboundMicrochipChangeCircuitPayload(int containerId, @Nullable CompoundCircuitNode rootNode, @Nullable String nodeClassName)
    {
        this(containerId, Optional.ofNullable(rootNode), Optional.ofNullable(nodeClassName));
    }

    @Override
    public Type<ClientboundMicrochipChangeCircuitPayload> type()
    {
        return TYPE;
    }
}
