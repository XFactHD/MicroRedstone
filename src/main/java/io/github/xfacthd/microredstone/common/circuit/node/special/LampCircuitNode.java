package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import java.util.List;
import java.util.Objects;

public final class LampCircuitNode extends CircuitNode
{
    public static final MapCodec<LampCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            DyeColor.CODEC.fieldOf("color").forGetter(node -> node.color),
            ChainEntry.CODEC.listOf().fieldOf("chained").forGetter(node -> node.chainedNodes),
            Connector.CODEC.fieldOf("input").forGetter(node -> node.getInputs()[0])
    ).apply(inst, LampCircuitNode::new));
    public static final StreamCodec<ByteBuf, LampCircuitNode> STREAM_CODEC = StreamCodec.composite(
            DyeColor.STREAM_CODEC,
            node -> node.color,
            ChainEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> node.chainedNodes,
            Connector.STREAM_CODEC,
            node -> node.getInputs()[0],
            LampCircuitNode::new
    );

    private final int inputWire;
    private final DyeColor color;
    private final List<ChainEntry> chainedNodes;

    public LampCircuitNode(DyeColor color, List<ChainEntry> chainedNodes, Connector input)
    {
        super(List.of(input), List.of());
        this.inputWire = input.wire();
        this.chainedNodes = chainedNodes;
        this.color = color;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs) { }

    public int getInputWire()
    {
        return inputWire;
    }

    public DyeColor getColor()
    {
        return color;
    }

    public List<ChainEntry> getChainedNodes()
    {
        return chainedNodes;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_LAMP.value();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof LampCircuitNode other)) return false;
        return other.color == color;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(inputWire, color);
    }

    public record ChainEntry(NodePos pos, int rotation)
    {
        static final Codec<ChainEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                NodePos.CODEC.fieldOf("pos").forGetter(ChainEntry::pos),
                Codec.intRange(0, 3).fieldOf("rotation").forGetter(ChainEntry::rotation)
        ).apply(inst, ChainEntry::new));
        static final StreamCodec<ByteBuf, ChainEntry> STREAM_CODEC = StreamCodec.composite(
                NodePos.STREAM_CODEC,
                ChainEntry::pos,
                ByteBufCodecs.VAR_INT,
                ChainEntry::rotation,
                ChainEntry::new
        );
    }
}
