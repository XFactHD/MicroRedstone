package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum WireType implements StringRepresentable
{
    SINGLE,
    BUNDLED;

    public static final Codec<WireType> CODEC = StringRepresentable.fromEnum(WireType::values);
    private static final IntFunction<WireType> BY_ID = ByIdMap.continuous(WireType::ordinal, WireType.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, WireType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, WireType::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName()
    {
        return name;
    }
}
