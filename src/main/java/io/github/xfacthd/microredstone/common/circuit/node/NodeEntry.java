package io.github.xfacthd.microredstone.common.circuit.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param node    The circuit node
 * @param inputs  The mapping between the internal ioPorts of the node and the ioPorts of the surrounding node for inputs
 * @param outputs The mapping between the internal ioPorts of the node and the ioPorts of the surrounding node for outputs
 */
public record NodeEntry<T extends CircuitNode>(T node, NodePos pos, int rotation, WirePair[] inputs, WirePair[] outputs)
{
    public static final Codec<NodeEntry<CircuitNode>> CODEC = codec(CircuitNode.CODEC);
    public static final StreamCodec<ByteBuf, NodeEntry<CircuitNode>> STREAM_CODEC = streamCodec(CircuitNode.STREAM_CODEC);
    private static final WirePair[] EMPTY_ARRAY = new WirePair[0];

    public NodeEntry(T node, NodePos pos, int rotation)
    {
        this(node, pos, rotation, EMPTY_ARRAY, EMPTY_ARRAY);
    }

    public void evaluate(EvalContext context)
    {
        node.evaluate(context, inputs, outputs);
    }

    public static <T extends CircuitNode> Codec<NodeEntry<T>> codec(Codec<T> nodeCodec)
    {
        return RecordCodecBuilder.create(inst -> inst.group(
                nodeCodec.fieldOf("node").forGetter(NodeEntry::node),
                NodePos.CODEC.fieldOf("pos").forGetter(NodeEntry::pos),
                Codec.intRange(0, 3).fieldOf("rotation").forGetter(NodeEntry::rotation),
                WirePair.ARRAY_CODEC.fieldOf("inputs").forGetter(NodeEntry::inputs),
                WirePair.ARRAY_CODEC.fieldOf("outputs").forGetter(NodeEntry::outputs)
        ).apply(inst, NodeEntry::new));
    }

    public static <T extends CircuitNode> StreamCodec<ByteBuf, NodeEntry<T>> streamCodec(StreamCodec<ByteBuf, T> nodeCodec)
    {
        return StreamCodec.composite(
                nodeCodec,
                NodeEntry::node,
                NodePos.STREAM_CODEC,
                NodeEntry::pos,
                ByteBufCodecs.VAR_INT,
                NodeEntry::rotation,
                WirePair.ARRAY_STREAM_CODEC,
                NodeEntry::inputs,
                WirePair.ARRAY_STREAM_CODEC,
                NodeEntry::outputs,
                NodeEntry::new
        );
    }
}
