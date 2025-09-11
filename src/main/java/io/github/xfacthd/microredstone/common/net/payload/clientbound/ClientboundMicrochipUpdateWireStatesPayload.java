package io.github.xfacthd.microredstone.common.net.payload.clientbound;

import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundMicrochipUpdateWireStatesPayload(int containerId, WireStates wireStates) implements CustomPacketPayload
{
    public static final Type<ClientboundMicrochipUpdateWireStatesPayload> TYPE = Utils.payloadType("clientbound_microchip_update_wire_states");
    public static final StreamCodec<FriendlyByteBuf, ClientboundMicrochipUpdateWireStatesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundMicrochipUpdateWireStatesPayload::containerId,
            WireStates.STREAM_CODEC,
            ClientboundMicrochipUpdateWireStatesPayload::wireStates,
            ClientboundMicrochipUpdateWireStatesPayload::new
    );

    @Override
    public Type<ClientboundMicrochipUpdateWireStatesPayload> type()
    {
        return TYPE;
    }
}
