package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Wire
{
    private static final Codec<Node> NODE_CODEC = makeNodeCodec();
    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            WireType.CODEC.fieldOf("type").forGetter(Wire::getWireType),
            DyeColor.CODEC.fieldOf("color").forGetter(Wire::getColor),
            NODE_CODEC.listOf().fieldOf("nodes").forGetter(Wire::getNodes)
    ).apply(inst, Wire::new));
    private static final StreamCodec<ByteBuf, Node> NODE_STREAM_CODEC = makeNodeStreamCodec();
    public static final StreamCodec<ByteBuf, Wire> STREAM_CODEC = StreamCodec.composite(
            WireType.STREAM_CODEC,
            Wire::getWireType,
            DyeColor.STREAM_CODEC,
            Wire::getColor,
            NODE_STREAM_CODEC.apply(ByteBufCodecs.list()),
            Wire::getNodes,
            Wire::new
    );

    private final WireType wireType;
    private final DyeColor color;
    private final List<Node> nodes = new ArrayList<>();

    public Wire(WireType wireType)
    {
        this(wireType, DyeColor.RED);
    }

    public Wire(WireType wireType, DyeColor color)
    {
        this.wireType = wireType;
        this.color = color;
    }

    private Wire(WireType wireType, DyeColor color, List<Node> nodes)
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

    public void addNodes(List<Node> nodes)
    {
        this.nodes.addAll(nodes);
    }

    public List<Node> getNodes()
    {
        return nodes;
    }

    @Override
    public String toString()
    {
        return "Wire[type=" + wireType + ",color=" + color + "]";
    }

    public sealed interface Node
    {
        NodePos pos();

        Set<NodePos> neighbors();

        Node withNeighbor(Port dir, NodePos neighbor);

        record Dangling(NodePos pos) implements Node
        {
            @Override
            public Set<NodePos> neighbors()
            {
                return Set.of();
            }

            @Override
            public Node withNeighbor(Port dir, NodePos neighbor)
            {
                return new Branch(pos, Set.of(dir), Set.of(neighbor));
            }
        }

        record Connection(NodePos pos, Port port, @Nullable NodePos neighbor) implements Node
        {
            private Connection(NodePos pos, Port port, Optional<NodePos> neighbor)
            {
                this(pos, port, neighbor.orElse(null));
            }

            @Override
            public Set<NodePos> neighbors()
            {
                return neighbor != null ? Set.of(neighbor) : Set.of();
            }

            @Override
            public Node withNeighbor(Port dir, NodePos neighbor)
            {
                return new Connection(pos, port, neighbor);
            }

            private Optional<NodePos> getNeighbor()
            {
                return Optional.ofNullable(neighbor);
            }
        }

        record Branch(NodePos pos, Set<Port> ports, Set<NodePos> neighbors) implements Node
        {
            public Branch
            {
                ports = EnumSet.copyOf(ports);
                neighbors = new HashSet<>(neighbors);
            }

            @Override
            public Node withNeighbor(Port dir, NodePos neighbor)
            {
                neighbors.add(neighbor);
                return this;
            }
        }
    }

    private static Codec<Node> makeNodeCodec()
    {
        MapCodec<Node.Dangling> danglingCodec = NodePos.CODEC.fieldOf("pos").xmap(Node.Dangling::new, Node.Dangling::pos);
        MapCodec<Node.Connection> connectionCodec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                NodePos.CODEC.fieldOf("pos").forGetter(Node::pos),
                Port.CODEC.fieldOf("port").forGetter(Node.Connection::port),
                NodePos.CODEC.optionalFieldOf("neighbor").forGetter(Node.Connection::getNeighbor)
        ).apply(inst, Node.Connection::new));
        MapCodec<Node.Branch> branchCodec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                NodePos.CODEC.fieldOf("pos").forGetter(Node::pos),
                Port.CODEC.listOf().fieldOf("ports").xmap(Set::copyOf, List::copyOf).forGetter(Node.Branch::ports),
                NodePos.CODEC.listOf().fieldOf("neighbors").xmap(Set::copyOf, List::copyOf).forGetter(Node.Branch::neighbors)
        ).apply(inst, Node.Branch::new));
        return Codec.STRING.dispatch(
                node -> switch (node)
                {
                    case Node.Dangling ignored -> "dangling";
                    case Node.Connection ignored -> "node";
                    case Node.Branch ignored -> "branch";
                },
                type -> switch (type)
                {
                    case "dangling" -> danglingCodec;
                    case "node" -> connectionCodec;
                    case "branch" -> branchCodec;
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }
        );
    }

    private static StreamCodec<ByteBuf, Node> makeNodeStreamCodec()
    {
        StreamCodec<ByteBuf, Node.Dangling> danglingCodec = NodePos.STREAM_CODEC.map(Node.Dangling::new, Node.Dangling::pos);
        StreamCodec<ByteBuf, Node.Connection> connectionCodec = StreamCodec.composite(
                NodePos.STREAM_CODEC,
                Node.Connection::pos,
                Port.STREAM_CODEC,
                Node.Connection::port,
                ByteBufCodecs.optional(NodePos.STREAM_CODEC),
                Node.Connection::getNeighbor,
                Node.Connection::new
        );
        StreamCodec<ByteBuf, Node.Branch> branchCodec = StreamCodec.composite(
                NodePos.STREAM_CODEC,
                Node.Branch::pos,
                Port.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
                Node.Branch::ports,
                NodePos.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
                Node.Branch::neighbors,
                Node.Branch::new
        );
        return ByteBufCodecs.STRING_UTF8.dispatch(
                node -> switch (node)
                {
                    case Node.Dangling ignored -> "dangling";
                    case Node.Connection ignored -> "node";
                    case Node.Branch ignored -> "branch";
                },
                type -> switch (type)
                {
                    case "dangling" -> danglingCodec;
                    case "node" -> connectionCodec;
                    case "branch" -> branchCodec;
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                }
        );
    }
}
