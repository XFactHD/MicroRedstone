package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.net.MRStreamCodecs;
import io.github.xfacthd.microredstone.common.util.SerdesUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * Defines a node crossing the boundary from one circuit node into a nested circuit node
 *
 * @param external The wire index in the surrounding circuit node
 * @param internal The wire index in the nested circuit node
 */
public record WirePair(int external, int internal)
{
    public static final Codec<WirePair> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("external").forGetter(WirePair::external),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("internal").forGetter(WirePair::internal)
    ).apply(inst, WirePair::new));
    public static final Codec<WirePair[]> ARRAY_CODEC = SerdesUtils.arrayCodec(CODEC, WirePair[]::new);
    public static final StreamCodec<ByteBuf, WirePair> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WirePair::external,
            ByteBufCodecs.VAR_INT,
            WirePair::internal,
            WirePair::new
    );
    public static final StreamCodec<ByteBuf, WirePair[]> ARRAY_STREAM_CODEC = MRStreamCodecs.array(STREAM_CODEC, WirePair[]::new);
}
