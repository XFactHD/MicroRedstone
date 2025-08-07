package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
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
    private static final Port[] VALUES = Port.values();
    private static final IntFunction<Port> BY_ID = ByIdMap.continuous(Port::ordinal, VALUES, ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, Port> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Port::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);

    public Port getOpposite()
    {
        return switch (this)
        {
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case DOWN -> UP;
            case LEFT -> RIGHT;
        };
    }

    public Port rotate(int rot)
    {
        return VALUES[Mth.positiveModulo(ordinal() + rot, VALUES.length)];
    }

    public int getLengthAlong(NodePos diff)
    {
        return switch (this)
        {
            case UP -> -diff.y();
            case RIGHT -> diff.x();
            case DOWN -> diff.y();
            case LEFT -> -diff.x();
        };
    }

    public <T> T select(T ifHor, T ifVert)
    {
        return this == UP || this == DOWN ? ifVert : ifHor;
    }

    @Override
    public String getSerializedName()
    {
        return name;
    }

    public int toPartRotation()
    {
        return Mth.positiveModulo(ordinal() + 1, VALUES.length);
    }

    public static Port ofCross(double fracX, double fracY)
    {
        fracX -= .5D;
        fracY -= .5D;

        if (Math.max(Math.abs(fracX), Math.abs(fracY)) == Math.abs(fracX))
        {
            return fracX > 0 ? RIGHT : LEFT;
        }
        else
        {
            return fracY > 0 ? DOWN : UP;
        }
    }

    public static Port ofPartRotation(int rotation)
    {
        return VALUES[Mth.positiveModulo(rotation - 1, VALUES.length)];
    }
}
