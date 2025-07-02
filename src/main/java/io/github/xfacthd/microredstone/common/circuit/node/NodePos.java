package io.github.xfacthd.microredstone.common.circuit.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record NodePos(int x, int y)
{
    public static final Codec<NodePos> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("x").forGetter(NodePos::x),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("y").forGetter(NodePos::y)
    ).apply(inst, NodePos::new));
    public static final StreamCodec<ByteBuf, NodePos> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodePos::x,
            ByteBufCodecs.VAR_INT,
            NodePos::y,
            NodePos::new
    );
}
