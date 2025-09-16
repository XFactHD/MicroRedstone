package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartSetMode;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnspecifiedConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.LampCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class LampPrototypeNode extends PrototypeNode
{
    private static final PortConfig PORT_CONFIG = PortConfig.builder()
            .addPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
            .build();
    public static final IconConfig ICON = new IconConfig(Utils.rl("part/lamp"), Utils.rl("port/left_single"), false);
    public static final IconConfig ICON_BG = new IconConfig(Utils.rl("part/untyped"), Utils.rl("port/left_single"), false);
    public static final IconConfig ICON_FG = new IconConfig(Utils.rl("part/lamp_overlay"), null, false);
    public static final IconConfig ICON_CHAIN = new IconConfig(Utils.rl("part/lamp_chain"), null);

    private final Set<LampPrototypeNode> chainedToThis = new ReferenceOpenHashSet<>();
    @Nullable
    private LampPrototypeNode chainedTo = null;
    private DyeColor color = DyeColor.RED;

    public LampPrototypeNode()
    {
        super(PORT_CONFIG, ICON);
    }

    public DyeColor getColor()
    {
        return color;
    }

    public void setColor(DyeColor color)
    {
        this.color = color;
        for (LampPrototypeNode node : chainedToThis)
        {
            node.setColor(color);
        }
    }

    @Nullable
    public LampPrototypeNode getNodeChainedTo()
    {
        return chainedTo;
    }

    public LampPrototypeNode getChainRoot()
    {
        LampPrototypeNode root = this;
        while (root.chainedTo != null)
        {
            root = root.chainedTo;
        }
        return root;
    }

    public void chain(LampPrototypeNode target)
    {
        chainedTo = target;
        target.chainedToThis.add(this);
    }

    public void unchain()
    {
        if (chainedTo != null)
        {
            chainedTo.chainedToThis.remove(this);
            chainedTo = null;
        }
    }

    public void unchainOnMove(PartSetMode mode)
    {
        switch (mode)
        {
            case ROTATE ->
            {
                unchain();

                Port newPort = Port.LEFT.rotate(getRotation());
                for (LampPrototypeNode node : Set.copyOf(chainedToThis))
                {
                    if (getPos().offset(newPort).equals(node.getPos()))
                    {
                        node.unchain();
                    }
                }
            }
            case MOVE ->
            {
                unchain();
                chainedToThis.forEach(LampPrototypeNode::unchain);
            }
        }
    }

    public void unchainOnDelete()
    {
        unchain();
        chainedToThis.forEach(LampPrototypeNode::unchain);
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (chainedTo == null && !isConnectedNormalized(Port.LEFT))
        {
            reporter.report(UnspecifiedConnectionProblem.input(Port.LEFT));
        }
    }

    @Override
    @Nullable
    public CircuitNode assemble(WireMapper wireMapper)
    {
        if (chainedTo != null) return null;

        List<LampCircuitNode.ChainEntry> chainedNodes = new ArrayList<>();
        collectChainedNodes(chainedNodes);
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        Connector inCon = new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, WireType.SINGLE);
        return new LampCircuitNode(color, chainedNodes, inCon);
    }

    private void collectChainedNodes(List<LampCircuitNode.ChainEntry> chainedNodes)
    {
        for (LampPrototypeNode node : chainedToThis)
        {
            chainedNodes.add(new LampCircuitNode.ChainEntry(node.getPos(), node.getRotation()));
            node.collectChainedNodes(chainedNodes);
        }
    }

    @Override
    public NodePos nudgePlacementPos(CircuitCanvasAccess canvas, NodePos newPos, int newRotation, int mouseX, int mouseY)
    {
        if (!(canvas.getPartNode(newPos) instanceof LampPrototypeNode lamp)) return newPos;
        if (lamp == this) return newPos;

        ExactNodePos exactPos = canvas.getExactNodePos(mouseX, mouseY);
        if (exactPos == null) return newPos;

        Port hoveredPort = Port.ofCross(exactPos.fracX(), exactPos.fracY());
        if (Port.LEFT.rotate(lamp.getRotation()) == hoveredPort) return newPos;
        if (Port.LEFT.rotate(newRotation) != hoveredPort.getOpposite()) return newPos;

        NodePos lampPos = newPos.offset(hoveredPort);
        return canvas.isValidPos(lampPos) ? lampPos : newPos;
    }

    @Override
    public void performPostPlaceAction(CircuitCanvasAccess canvas, int mouseX, int mouseY, @Nullable PartSetMode mode, boolean revertToLast)
    {
        if (mode != null && mode != PartSetMode.ADD)
        {
            unchainOnMove(mode);
        }
        if (!revertToLast)
        {
            NodePos pos = canvas.getNodePos(mouseX, mouseY);
            if (pos != null && !pos.equals(getPos()) && canvas.getPartNode(pos) instanceof LampPrototypeNode lamp)
            {
                chain(lamp);
            }
        }
    }

    @Override
    public void performPreRemoveAction(CircuitCanvasAccess canvas)
    {
        unchainOnDelete();
    }
}
