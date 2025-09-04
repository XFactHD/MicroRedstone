package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record ClientboundCircuitLibraryUpdatePayload(List<CircuitLibraryEntry> addedOrModified, List<UUID> removed) implements CustomPacketPayload
{
    public static final Type<ClientboundCircuitLibraryUpdatePayload> TYPE = Utils.payloadType("clientbound_circuit_library_update");
    public static final StreamCodec<ByteBuf, ClientboundCircuitLibraryUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            CircuitLibraryEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundCircuitLibraryUpdatePayload::addedOrModified,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundCircuitLibraryUpdatePayload::removed,
            ClientboundCircuitLibraryUpdatePayload::new
    );

    @Override
    public Type<ClientboundCircuitLibraryUpdatePayload> type()
    {
        return TYPE;
    }
}
