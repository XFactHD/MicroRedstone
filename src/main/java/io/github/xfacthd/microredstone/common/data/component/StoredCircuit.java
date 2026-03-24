package io.github.xfacthd.microredstone.common.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public record StoredCircuit(@Nullable CompoundCircuitNode rootNode) implements TooltipProvider
{
    public static final Codec<StoredCircuit> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            CompoundCircuitNode.CODEC.codec().optionalFieldOf("root_node").forGetter(StoredCircuit::rootNodeForSerialization)
    ).apply(inst, StoredCircuit::of));
    public static final StreamCodec<ByteBuf, StoredCircuit> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(CompoundCircuitNode.STREAM_CODEC),
            StoredCircuit::rootNodeForSerialization,
            StoredCircuit::of
    );
    public static final StoredCircuit EMPTY = new StoredCircuit(null);
    public static final String LABEL_CIRCUIT = Utils.translationKey("label", "stored_circuit");

    private static StoredCircuit of(Optional<CompoundCircuitNode> rootNode)
    {
        return new StoredCircuit(rootNode.orElse(null));
    }

    @SuppressWarnings("NullableProblems")
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
        if (rootNode != null)
        {
            tooltipAdder.accept(Component.translatable(LABEL_CIRCUIT, rootNode.getName()));
        }
    }

    public static boolean isPresent(ItemStack stack)
    {
        StoredCircuit circuit = stack.get(MRContent.DC_TYPE_CIRCUIT);
        return circuit != null && !circuit.equals(EMPTY);
    }
}
