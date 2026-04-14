package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum ShareType implements StringRepresentable {
    PRIVATE,
    SHARED,
    PUBLIC,
    BUILTIN;

    private static final ShareType[] SERIALIZABLE_VALUES = { PRIVATE, SHARED, PUBLIC };
    public static final Codec<ShareType> CODEC = StringRepresentable.fromEnum(() -> SERIALIZABLE_VALUES);
    private static final IntFunction<ShareType> BY_ID = ByIdMap.continuous(ShareType::ordinal, SERIALIZABLE_VALUES, ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, ShareType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ShareType::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);
    private final Component title = Utils.translate("label", "circuit_workbench.library_browser.filter." + name);
    private final Identifier icon = Utils.rl("filter/type_" + name);

    public Component getTitle() {
        return title;
    }

    public Identifier getIcon() {
        return icon;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    MapCodec<? extends ShareInfo> getInfoCodec() {
        return switch (this) {
            case PRIVATE -> ShareInfo.Private.CODEC;
            case SHARED -> ShareInfo.Shared.CODEC;
            case PUBLIC -> ShareInfo.Public.CODEC;
            case BUILTIN -> throw new IllegalArgumentException("Built-in shares are not serializable");
        };
    }

    StreamCodec<ByteBuf, ? extends ShareInfo> getInfoStreamCodec() {
        return switch (this) {
            case PRIVATE -> ShareInfo.Private.STREAM_CODEC;
            case SHARED -> ShareInfo.Shared.STREAM_CODEC;
            case PUBLIC -> ShareInfo.Public.STREAM_CODEC;
            case BUILTIN -> throw new IllegalArgumentException("Built-in shares are not serializable");
        };
    }
}
