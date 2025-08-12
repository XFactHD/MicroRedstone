package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

public sealed interface WireNode
{
    Codec<WireNode> CODEC = NodeType.CODEC.dispatch(WireNode::type, NodeType::getCodec);
    StreamCodec<ByteBuf, WireNode> STREAM_CODEC = NodeType.STREAM_CODEC.dispatch(WireNode::type, NodeType::getStreamCodec);

    NodePos pos();

    Set<NodePos> neighbors();

    WireNode withNeighbor(Port dir, NodePos neighbor);

    WireNode copy();

    NodeType type();

    record Dangling(NodePos pos) implements WireNode
    {
        private static final MapCodec<Dangling> CODEC = NodePos.CODEC.fieldOf("pos").xmap(WireNode.Dangling::new, WireNode.Dangling::pos);
        private static final StreamCodec<ByteBuf, Dangling> STREAM_CODEC = NodePos.STREAM_CODEC.map(WireNode.Dangling::new, WireNode.Dangling::pos);

        @Override
        public Set<NodePos> neighbors()
        {
            return Set.of();
        }

        @Override
        public WireNode withNeighbor(Port dir, NodePos neighbor)
        {
            return new Branch(pos, Set.of(dir), Set.of(neighbor));
        }

        @Override
        public WireNode copy()
        {
            return this;
        }

        @Override
        public NodeType type()
        {
            return NodeType.DANGLING;
        }
    }

    record Connection(NodePos pos, Port port, @Nullable NodePos neighbor) implements WireNode
    {
        private static final MapCodec<Connection> CODEC = RecordCodecBuilder.<WireNode.Connection>mapCodec(inst -> inst.group(
            NodePos.CODEC.fieldOf("pos").forGetter(WireNode::pos),
                Port.CODEC.fieldOf("port").forGetter(WireNode.Connection::port),
                NodePos.CODEC.optionalFieldOf("neighbor").forGetter(WireNode.Connection::getNeighbor)
        ).apply(inst, WireNode.Connection::new));
        private static final StreamCodec<ByteBuf, Connection> STREAM_CODEC = StreamCodec.composite(
                NodePos.STREAM_CODEC,
                WireNode.Connection::pos,
                Port.STREAM_CODEC,
                WireNode.Connection::port,
                ByteBufCodecs.optional(NodePos.STREAM_CODEC),
                WireNode.Connection::getNeighbor,
                WireNode.Connection::new
        );

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
        public WireNode withNeighbor(Port dir, NodePos neighbor)
        {
            return new Connection(pos, port, neighbor);
        }

        @Override
        public WireNode copy()
        {
            return this;
        }

        @Override
        public NodeType type()
        {
            return NodeType.CONNECTION;
        }

        private Optional<NodePos> getNeighbor()
        {
            return Optional.ofNullable(neighbor);
        }
    }

    record Branch(NodePos pos, Set<Port> ports, Set<NodePos> neighbors) implements WireNode
    {
        private static final MapCodec<Branch> CODEC = RecordCodecBuilder.<WireNode.Branch>mapCodec(inst -> inst.group(
                NodePos.CODEC.fieldOf("pos").forGetter(WireNode::pos),
                Port.CODEC.listOf().fieldOf("ports").xmap(Set::copyOf, List::copyOf).forGetter(WireNode.Branch::ports),
                NodePos.CODEC.listOf().fieldOf("neighbors").xmap(Set::copyOf, List::copyOf).forGetter(WireNode.Branch::neighbors)
        ).apply(inst, WireNode.Branch::new));
        private static final StreamCodec<ByteBuf, Branch> STREAM_CODEC = StreamCodec.composite(
                NodePos.STREAM_CODEC,
                WireNode.Branch::pos,
                Port.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
                WireNode.Branch::ports,
                NodePos.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
                WireNode.Branch::neighbors,
                WireNode.Branch::new
        );

        public Branch
        {
            ports = EnumSet.copyOf(ports);
            neighbors = new HashSet<>(neighbors);
        }

        @Override
        public WireNode withNeighbor(Port dir, NodePos neighbor)
        {
            ports.add(dir);
            neighbors.add(neighbor);
            return this;
        }

        @Override
        public WireNode copy()
        {
            return new Branch(pos, ports, neighbors);
        }

        @Override
        public NodeType type()
        {
            return NodeType.BRANCH;
        }
    }

    enum NodeType implements StringRepresentable
    {
        DANGLING(Dangling.CODEC, Dangling.STREAM_CODEC),
        CONNECTION(Connection.CODEC, Connection.STREAM_CODEC),
        BRANCH(Branch.CODEC, Branch.STREAM_CODEC),
        ;

        private static final Codec<NodeType> CODEC = StringRepresentable.fromEnum(NodeType::values);
        private static final IntFunction<NodeType> BY_ID = ByIdMap.continuous(NodeType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        private static final StreamCodec<ByteBuf, NodeType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, NodeType::ordinal);

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final MapCodec<? extends WireNode> codec;
        private final StreamCodec<ByteBuf, ? extends WireNode> streamCodec;

        NodeType(MapCodec<? extends WireNode> codec, StreamCodec<ByteBuf, ? extends WireNode> streamCodec)
        {
            this.codec = codec;
            this.streamCodec = streamCodec;
        }

        private MapCodec<? extends WireNode> getCodec()
        {
            return codec;
        }

        private StreamCodec<ByteBuf, ? extends WireNode> getStreamCodec()
        {
            return streamCodec;
        }

        @Override
        public String getSerializedName()
        {
            return name;
        }
    }
}
