package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundWorkbenchWriteCircuitResultPayload(int containerId, boolean success) implements CustomPacketPayload
{
    public static final Type<ClientboundWorkbenchWriteCircuitResultPayload> TYPE = Utils.payloadType("clientbound_workbench_write_result");
    public static final StreamCodec<ByteBuf, ClientboundWorkbenchWriteCircuitResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundWorkbenchWriteCircuitResultPayload::containerId,
            ByteBufCodecs.BOOL,
            ClientboundWorkbenchWriteCircuitResultPayload::success,
            ClientboundWorkbenchWriteCircuitResultPayload::new
    );

    @Override
    public Type<ClientboundWorkbenchWriteCircuitResultPayload> type()
    {
        return TYPE;
    }
}
