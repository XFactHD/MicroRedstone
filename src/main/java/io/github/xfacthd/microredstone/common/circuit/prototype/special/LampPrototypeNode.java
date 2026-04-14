package io.github.xfacthd.microredstone.common.circuit.prototype.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartSetMode;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.LampCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CircuitCanvasAccess;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.SerdesUtils;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class LampPrototypeNode extends PrototypeNode {
    private static final PortConfig PORT_CONFIG = PortConfig.<LampPrototypeNode>builder()
            .addPredicatedPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT, node -> node.chainedTo == null)
            .build();
    public static final IconConfig ICON = IconConfig.of(Utils.id("part/lamp"), PORT_CONFIG, false);
    public static final IconConfig ICON_BG = IconConfig.of(Utils.id("part/untyped"), PORT_CONFIG, false);
    public static final IconConfig ICON_FG = new IconConfig(Utils.id("part/lamp_overlay"), null, false);
    public static final IconConfig ICON_CHAIN = new IconConfig(Utils.id("part/lamp_chain"), null, true);

    private final Set<LampPrototypeNode> chainedToThis = new ReferenceOpenHashSet<>();
    @Nullable
    private LampPrototypeNode chainedTo = null;
    private DyeColor color = DyeColor.RED;
    @Nullable
    private LampChain chainToResolve = null;

    public LampPrototypeNode() {
        super(PORT_CONFIG, ICON);
    }

    private LampPrototypeNode(LampChain chain, DyeColor color) {
        this();
        this.chainToResolve = chain;
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
        for (LampPrototypeNode node : chainedToThis) {
            node.setColor(color);
        }
    }

    public @Nullable LampPrototypeNode getNodeChainedTo() {
        return chainedTo;
    }

    public LampPrototypeNode getChainRoot() {
        LampPrototypeNode root = this;
        while (root.chainedTo != null) {
            root = root.chainedTo;
        }
        return root;
    }

    public void chain(LampPrototypeNode target) {
        chainedTo = target;
        target.chainedToThis.add(this);
    }

    public void unchain() {
        if (chainedTo != null) {
            chainedTo.chainedToThis.remove(this);
            chainedTo = null;
        }
    }

    public void unchainOnMove(PartSetMode mode) {
        switch (mode) {
            case ROTATE -> {
                unchain();

                Port newPort = Port.LEFT.rotate(getRotation());
                for (LampPrototypeNode node : Set.copyOf(chainedToThis)) {
                    if (getPos().offset(newPort).equals(node.getPos())) {
                        node.unchain();
                    }
                }
            }
            case MOVE -> {
                unchain();
                chainedToThis.forEach(LampPrototypeNode::unchain);
            }
        }
    }

    public void unchainOnDelete() {
        unchain();
        chainedToThis.forEach(LampPrototypeNode::unchain);
    }

    @Override
    public @Nullable CircuitNode assemble(WireMapper wireMapper) {
        if (chainedTo != null) {
            return null;
        }

        List<LampCircuitNode.ChainEntry> chainedNodes = new ArrayList<>();
        collectChainedNodes(chainedNodes);
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        Connector inCon = new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, WireType.SINGLE);
        return new LampCircuitNode(color, chainedNodes, inCon);
    }

    private void collectChainedNodes(List<LampCircuitNode.ChainEntry> chainedNodes) {
        for (LampPrototypeNode node : chainedToThis) {
            chainedNodes.add(new LampCircuitNode.ChainEntry(node.getPos(), node.getRotation()));
            node.collectChainedNodes(chainedNodes);
        }
    }

    @Override
    public NodePos nudgePlacementPos(CircuitCanvasAccess canvas, NodePos newPos, int newRotation, int mouseX, int mouseY) {
        if (!(canvas.getPartNode(newPos) instanceof LampPrototypeNode lamp) || lamp == this) {
            return newPos;
        }

        ExactNodePos exactPos = canvas.getExactNodePos(mouseX, mouseY);
        if (exactPos == null) {
            return newPos;
        }

        Port hoveredPort = Port.ofCross(exactPos.fracX(), exactPos.fracY());
        if (Port.LEFT.rotate(lamp.getRotation()) == hoveredPort) {
            return newPos;
        }
        if (Port.LEFT.rotate(newRotation) != hoveredPort.getOpposite()) {
            return newPos;
        }

        NodePos lampPos = newPos.offset(hoveredPort);
        return canvas.isValidPos(lampPos) ? lampPos : newPos;
    }

    @Override
    public void performPostPlaceAction(CircuitCanvasAccess canvas, int mouseX, int mouseY, @Nullable PartSetMode mode, boolean revertToLast) {
        if (mode != null && mode != PartSetMode.ADD) {
            unchainOnMove(mode);
        }
        if (!revertToLast) {
            NodePos pos = canvas.getNodePos(mouseX, mouseY);
            if (pos != null && !pos.equals(getPos()) && canvas.getPartNode(pos) instanceof LampPrototypeNode lamp) {
                chain(lamp);
            }
        }
    }

    @Override
    public void performPreRemoveAction(CircuitCanvasAccess canvas) {
        unchainOnDelete();
    }

    @Override
    public Serializable serialize(List<Wire> wires) {
        Set<NodePos> chainedToThis = this.chainedToThis.stream()
                .map(LampPrototypeNode::getPos)
                .collect(Collectors.toSet());
        Optional<NodePos> chainedTo = Optional.ofNullable(this.chainedTo).map(LampPrototypeNode::getPos);
        return new Serializable(this, wires, new LampChain(chainedToThis, chainedTo), color);
    }

    @Override
    public void finishDeserialization(List<PrototypeNode> nodes) {
        if (chainToResolve != null && !chainToResolve.isEmpty()) {
            chainToResolve.chainedToThis.stream()
                    .map(pos -> findNode(nodes, pos))
                    .filter(LampPrototypeNode.class::isInstance)
                    .map(LampPrototypeNode.class::cast)
                    .forEach(chainedToThis::add);
            chainedTo = chainToResolve.chainedTo
                    .map(pos -> findNode(nodes, pos))
                    .filter(LampPrototypeNode.class::isInstance)
                    .map(LampPrototypeNode.class::cast)
                    .orElse(null);
        }
        chainToResolve = null;
    }

    private static @Nullable PrototypeNode findNode(List<PrototypeNode> nodes, NodePos pos) {
        return nodes.stream()
                .filter(node -> node.getPos().equals(pos))
                .findFirst()
                .orElse(null);
    }

    public static final class Serializable extends PrototypeNode.Serializable {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                LampChain.CODEC.fieldOf("chain").forGetter(node -> node.chain),
                DyeColor.CODEC.fieldOf("color").forGetter(node -> node.color)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final LampChain chain;
        private final DyeColor color;

        private Serializable(PrototypeNode node, List<Wire> wires, LampChain chain, DyeColor color) {
            super(node, wires);
            this.chain = chain;
            this.color = color;
        }

        private Serializable(LampChain chain, DyeColor color, Map<Port, Integer> connectedWires, NodePos pos, int rotation) {
            super(connectedWires, pos, rotation);
            this.chain = chain;
            this.color = color;
        }

        @Override
        protected PrototypeNode buildInternal() {
            return new LampPrototypeNode(chain, color);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type() {
            return MRContent.PROTO_TYPE_LAMP.value();
        }
    }

    private record LampChain(Set<NodePos> chainedToThis, Optional<NodePos> chainedTo) {
        public static final Codec<LampChain> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                SerdesUtils.setCodec(NodePos.CODEC).fieldOf("chained_to_this").forGetter(LampChain::chainedToThis),
                NodePos.CODEC.optionalFieldOf("chained_to").forGetter(LampChain::chainedTo)
        ).apply(inst, LampChain::new));

        private boolean isEmpty() {
            return chainedToThis.isEmpty() && chainedTo.isEmpty();
        }
    }
}
