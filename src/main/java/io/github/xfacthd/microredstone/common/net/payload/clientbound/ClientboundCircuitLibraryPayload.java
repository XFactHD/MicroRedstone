package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record ClientboundCircuitLibraryPayload(List<CircuitLibraryEntry> entries) implements CustomPacketPayload
{
    public static final Type<ClientboundCircuitLibraryPayload> TYPE = Utils.payloadType("clientbound_circuit_library");
    public static final StreamCodec<ByteBuf, ClientboundCircuitLibraryPayload> STREAM_CODEC = StreamCodec.composite(
            CircuitLibraryEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundCircuitLibraryPayload::entries,
            ClientboundCircuitLibraryPayload::new
    );

    @Override
    public Type<ClientboundCircuitLibraryPayload> type()
    {
        return TYPE;
    }
}
