package io.github.xfacthd.microredstone.common.redstone;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum RedstoneType implements StringRepresentable
{
    NONE,
    SINGLE,
    BUNDLED;

    public static final Codec<RedstoneType> CODEC = StringRepresentable.fromEnum(RedstoneType::values);

    private final String name = toString().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName()
    {
        return name;
    }

    public static RedstoneType of(@Nullable WireType wireType)
    {
        return switch (wireType)
        {
            case null -> NONE;
            case SINGLE -> SINGLE;
            case BUNDLED -> BUNDLED;
        };
    }
}
