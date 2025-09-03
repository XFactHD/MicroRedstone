package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

public final class Wire
{
    public static final DyeColor DEFAULT_COLOR = DyeColor.RED;
    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            WireType.CODEC.fieldOf("type").forGetter(Wire::getWireType),
            DyeColor.CODEC.optionalFieldOf("color", DEFAULT_COLOR).forGetter(Wire::getColor),
            WireNode.CODEC.listOf().fieldOf("nodes").forGetter(Wire::getNodes)
    ).apply(inst, Wire::new));
    public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
            WireType.STREAM_CODEC,
            Wire::getWireType,
            DyeColor.STREAM_CODEC,
            Wire::getColor,
            WireNode.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Wire::getNodes,
            Wire::new
    );

    private final WireType wireType;
    private final DyeColor color;
    private final List<WireNode> nodes = new ArrayList<>();

    public Wire(WireType wireType)
    {
        this(wireType, DEFAULT_COLOR);
    }

    public Wire(WireType wireType, DyeColor color)
    {
        this.wireType = wireType;
        this.color = color;
    }

    private Wire(WireType wireType, DyeColor color, List<WireNode> nodes)
    {
        this(wireType, color);
        this.nodes.addAll(nodes);
    }

    public WireType getWireType()
    {
        return wireType;
    }

    public DyeColor getColor()
    {
        return color;
    }

    public void addNodes(List<WireNode> nodes)
    {
        this.nodes.addAll(nodes);
    }

    public List<WireNode> getNodes()
    {
        return nodes;
    }

    public Wire copy()
    {
        List<WireNode> copiedNodes = new ArrayList<>(nodes.size());
        for (WireNode node : nodes)
        {
            copiedNodes.add(node.copy());
        }
        return new Wire(wireType, color, copiedNodes);
    }

    @Override
    public String toString()
    {
        return "Wire[type=" + wireType + ",color=" + color + "]";
    }
}
