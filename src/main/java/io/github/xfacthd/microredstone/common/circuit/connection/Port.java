package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum Port implements StringRepresentable
{
    UP,
    RIGHT,
    DOWN,
    LEFT;

    public static final Codec<Port> CODEC = StringRepresentable.fromEnum(Port::values);
    private static final IntFunction<Port> BY_ID = ByIdMap.continuous(Port::ordinal, Port.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, Port> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Port::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName()
    {
        return name;
    }
}
