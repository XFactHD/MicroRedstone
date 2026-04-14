package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Wire {
    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            WireType.CODEC.fieldOf("type").forGetter(Wire::getWireType),
            DyeColor.CODEC.optionalFieldOf("color").forGetter(Wire::getColorForSerialization),
            WireNode.CODEC.listOf().fieldOf("nodes").forGetter(Wire::getNodes)
    ).apply(inst, Wire::deserialize));
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

    public Wire(WireType wireType, DyeColor color) {
        this.wireType = wireType;
        this.color = wireType.getColor(color);
    }

    private Wire(WireType wireType, DyeColor color, List<WireNode> nodes) {
        this(wireType, color);
        this.nodes.addAll(nodes);
    }

    private static Wire deserialize(WireType wireType, Optional<DyeColor> color, List<WireNode> nodes) {
        return new Wire(wireType, color.orElse(wireType.getDefaultColor()), nodes);
    }

    public WireType getWireType() {
        return wireType;
    }

    public DyeColor getColor() {
        return color;
    }

    private Optional<DyeColor> getColorForSerialization() {
        return color == wireType.getDefaultColor() ? Optional.empty() : Optional.of(color);
    }

    public void addNodes(List<WireNode> nodes) {
        this.nodes.addAll(nodes);
    }

    public List<WireNode> getNodes() {
        return nodes;
    }

    public Wire copy() {
        List<WireNode> copiedNodes = new ArrayList<>(nodes.size());
        for (WireNode node : nodes) {
            copiedNodes.add(node.copy());
        }
        return new Wire(wireType, color, copiedNodes);
    }

    @Override
    public String toString() {
        return "Wire[type=" + wireType + ",color=" + color + "]";
    }
}
