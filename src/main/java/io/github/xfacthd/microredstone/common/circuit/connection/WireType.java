package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum WireType implements StringRepresentable
{
    SINGLE(DyeColor.RED),
    BUNDLED(DyeColor.WHITE);

    public static final Codec<WireType> CODEC = StringRepresentable.fromEnum(WireType::values);
    private static final IntFunction<WireType> BY_ID = ByIdMap.continuous(WireType::ordinal, WireType.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, WireType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, WireType::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);
    private final Icon icon = new Icon(Utils.rl("wire/icon_" + name));
    private final DyeColor defaultColor;

    WireType(DyeColor defaultColor)
    {
        this.defaultColor = defaultColor;
    }

    @Override
    public String getSerializedName()
    {
        return name;
    }

    public Icon getIcon()
    {
        return icon;
    }

    public DyeColor getColor(DyeColor color)
    {
        return switch (this)
        {
            case SINGLE -> color;
            case BUNDLED -> DyeColor.WHITE;
        };
    }

    public DyeColor getDefaultColor()
    {
        return defaultColor;
    }

    @Nullable
    @Contract("!null,!null->!null")
    public <T> T select(@Nullable T valueSingle, @Nullable T valueBundled)
    {
        return switch (this)
        {
            case SINGLE -> valueSingle;
            case BUNDLED -> valueBundled;
        };
    }

    public record Icon(ResourceLocation texture, int color)
    {
        private Icon(ResourceLocation texture)
        {
            this(texture, 0xFFFFFFFF);
        }

        public Icon withColor(@Nullable DyeColor color)
        {
            return withColor(color != null ? color.getTextureDiffuseColor() : 0xFFFFFFFF);
        }

        public Icon withColor(int color)
        {
            return new Icon(texture, color);
        }
    }
}
