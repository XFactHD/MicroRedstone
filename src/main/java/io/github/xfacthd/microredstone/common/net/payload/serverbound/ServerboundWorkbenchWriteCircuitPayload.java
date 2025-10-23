package io.github.xfacthd.microredstone.common.net.payload.serverbound;

import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundWorkbenchWriteCircuitPayload(int containerId, CompoundCircuitNode circuitNode) implements CustomPacketPayload
{
    public static final Type<ServerboundWorkbenchWriteCircuitPayload> TYPE = Utils.payloadType("serverbound_workbench_write_circuit");
    public static final StreamCodec<ByteBuf, ServerboundWorkbenchWriteCircuitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ServerboundWorkbenchWriteCircuitPayload::containerId,
            CompoundCircuitNode.STREAM_CODEC,
            ServerboundWorkbenchWriteCircuitPayload::circuitNode,
            ServerboundWorkbenchWriteCircuitPayload::new
    );

    public void handle(IPayloadContext ctx)
    {
        if (ctx.player().containerMenu instanceof CircuitWorkbenchMenu menu && menu.containerId == containerId)
        {
            ctx.reply(menu.applyCircuitToItem(circuitNode));
        }
    }

    @Override
    public Type<ServerboundWorkbenchWriteCircuitPayload> type()
    {
        return TYPE;
    }
}
