package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.function.IntFunction;

public enum PortDir implements StringRepresentable {
    INPUT,
    OUTPUT;

    public static final Codec<PortDir> CODEC = StringRepresentable.fromEnum(PortDir::values);
    private static final IntFunction<PortDir> BY_ID = ByIdMap.continuous(PortDir::ordinal, PortDir.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PortDir> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, PortDir::ordinal);

    private final String name = toString().toLowerCase(Locale.ROOT);
    private final String description = StringUtils.capitalize(name);
    private final Component title = Utils.translate("label", "port_dir." + name);

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Component getTitle() {
        return title;
    }
}
