package io.github.xfacthd.microredstone.client.screen.microchip;

import io.github.xfacthd.microredstone.client.screen.workbench.element.LampRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.AbstractCircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.CircuitUtils;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.LampCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MicrochipCircuitCanvas extends AbstractCircuitCanvas
{
    private final List<PartRenderState> parts = new ArrayList<>();
    private final List<WireRenderState> wires = new ArrayList<>();
    private final List<LampRenderState> lamps = new ArrayList<>();

    @Override
    protected void collectCanvasContent(
            int canvasX,
            int canvasY,
            List<PartRenderState> parts,
            List<WireRenderState> wires,
            List<LampRenderState> lamps,
            int mouseX,
            int mouseY
    )
    {
        parts.addAll(this.parts);
        wires.addAll(this.wires);
        lamps.addAll(this.lamps);
    }

    void update(@Nullable CompoundCircuitNode rootNode)
    {
        parts.clear();
        wires.clear();

        if (rootNode == null) return;

        Set<NodePos> partPositions = new HashSet<>();
        for (Connector connector : Utils.concatArrays(rootNode.getInputs(), rootNode.getOutputs()))
        {
            Connection connection = new Connection(connector.type());
            connection.setPos(connector.pos(), connector.port().toPartRotation());
            connection.setPortDir(connector.dir());
            connection.setName(connector.name());
            parts.add(new PartRenderState(connection));
            partPositions.add(connector.pos());
        }
        rootNode.forAllNodes(entry ->
        {
            if (entry.node() instanceof LampCircuitNode lamp)
            {
                int inputWire = lamp.getInputWire();
                DyeColor color = lamp.getColor();
                lamps.add(new LampRenderState(entry.pos(), entry.rotation(), inputWire, color, false));
                partPositions.add(entry.pos());
                for (LampCircuitNode.ChainEntry node : lamp.getChainedNodes())
                {
                    lamps.add(new LampRenderState(node.pos(), node.rotation(), inputWire, color, true));
                }
                return;
            }

            IconConfig icon = CircuitUtils.makeIconConfig(entry.node());
            parts.add(new PartRenderState(entry.pos(), icon, entry.rotation()));
            partPositions.add(entry.pos());
        });
        rootNode.getWires().forEach(wire ->
        {
            WireRenderState renderState = new WireRenderState(wire.getWireType(), wire.getColor());
            Set<RoutedWire.Section> sections = new HashSet<>();
            for (WireNode node : wire.getNodes())
            {
                renderState.addNode(node);
                switch (node)
                {
                    case WireNode.Branch(NodePos pos, Set<Port> ignored, Set<NodePos> neighbors) ->
                    {
                        for (NodePos neighbor : neighbors)
                        {
                            sections.add(new RoutedWire.Section(pos, neighbor));
                        }
                    }
                    case WireNode.Connection(NodePos pos, Port ignored, @Nullable NodePos neighbor) ->
                    {
                        if (neighbor != null)
                        {
                            sections.add(new RoutedWire.Section(pos, neighbor));
                        }
                    }
                    case WireNode.Dangling ignored -> {}
                }
            }
            renderState.addSections(partPositions, sections);
            if (!renderState.isEmpty())
            {
                wires.add(renderState);
            }
        });
    }

    public void updateWireStates(WireStates wireStates)
    {
        for (int i = 0; i < wires.size(); i++)
        {
            WireRenderState renderState = wires.get(i);
            wires.set(i, renderState.withPowered(wireStates.get(i)));
        }
        for (int i = 0; i < lamps.size(); i++)
        {
            LampRenderState renderState = lamps.get(i);
            boolean powered = wireStates.get(renderState.inputWire());
            lamps.set(i, renderState.withPowered(powered));
        }
    }

    @Override
    public void computeWindowSize(int width, int height)
    {
        this.width = Math.min(WIDTH, width - MicrochipCircuitScreen.CANVAS_BORDER * 2);
        this.height = Math.min(HEIGHT, height - MicrochipCircuitScreen.CANVAS_BORDER * 3);
    }

    @Override
    public void computeWindowPos(int leftPos, int topPos)
    {
        x = leftPos + MicrochipCircuitScreen.CANVAS_BORDER;
        y = topPos + MicrochipCircuitScreen.CANVAS_BORDER * 2;
    }
}
