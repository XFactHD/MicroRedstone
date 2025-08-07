package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record Connector(NodePos pos, Port port, int wire, PortDir dir, WireType type)
{
    public static final Codec<Connector> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            NodePos.CODEC.fieldOf("pos").forGetter(Connector::pos),
            Port.CODEC.fieldOf("port").forGetter(Connector::port),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("wire").forGetter(Connector::wire),
            PortDir.CODEC.fieldOf("dir").forGetter(Connector::dir),
            WireType.CODEC.fieldOf("type").forGetter(Connector::type)
    ).apply(inst, Connector::new));
    public static final StreamCodec<ByteBuf, Connector> STREAM_CODEC = StreamCodec.composite(
            NodePos.STREAM_CODEC,
            Connector::pos,
            Port.STREAM_CODEC,
            Connector::port,
            ByteBufCodecs.VAR_INT,
            Connector::wire,
            PortDir.STREAM_CODEC,
            Connector::dir,
            WireType.STREAM_CODEC,
            Connector::type,
            Connector::new
    );
}
