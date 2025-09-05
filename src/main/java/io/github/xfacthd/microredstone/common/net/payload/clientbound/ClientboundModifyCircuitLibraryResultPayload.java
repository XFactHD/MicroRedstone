package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundModifyCircuitLibraryResultPayload(boolean success) implements CustomPacketPayload
{
    public static final Type<ClientboundModifyCircuitLibraryResultPayload> TYPE = Utils.payloadType("clientbound_modify_circuit_library_result");
    public static final StreamCodec<ByteBuf, ClientboundModifyCircuitLibraryResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundModifyCircuitLibraryResultPayload::success,
            ClientboundModifyCircuitLibraryResultPayload::new
    );

    @Override
    public Type<ClientboundModifyCircuitLibraryResultPayload> type()
    {
        return TYPE;
    }
}
