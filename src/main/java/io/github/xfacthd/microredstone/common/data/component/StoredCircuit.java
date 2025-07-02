package io.github.xfacthd.microredstone.common.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public record StoredCircuit(String name, @Nullable CompoundCircuitNode rootNode) implements TooltipProvider
{
    public static final Codec<StoredCircuit> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(StoredCircuit::name),
            CompoundCircuitNode.CODEC.codec().optionalFieldOf("root_node").forGetter(StoredCircuit::rootNodeForSerialization)
    ).apply(inst, StoredCircuit::of));
    public static final StreamCodec<ByteBuf, StoredCircuit> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            StoredCircuit::name,
            ByteBufCodecs.optional(CompoundCircuitNode.STREAM_CODEC),
            StoredCircuit::rootNodeForSerialization,
            StoredCircuit::of
    );
    public static final StoredCircuit EMPTY = new StoredCircuit("", null);

    private static StoredCircuit of(String name, Optional<CompoundCircuitNode> rootNode)
    {
        return new StoredCircuit(name, rootNode.orElse(null));
    }

    private Optional<CompoundCircuitNode> rootNodeForSerialization()
    {
        return Optional.ofNullable(rootNode);
    }

    @Nullable
    public Circuit toCircuit()
    {
        return rootNode != null ? new Circuit(rootNode) : null;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter)
    {
        // TODO: implement tooltip
    }
}
