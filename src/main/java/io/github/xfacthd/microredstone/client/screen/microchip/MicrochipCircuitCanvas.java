package io.github.xfacthd.microredstone.client.screen.microchip;

import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.AbstractCircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.CircuitUtils;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MicrochipCircuitCanvas extends AbstractCircuitCanvas
{
    private final List<PartRenderState> parts = new ArrayList<>();
    private final List<WireRenderState> wires = new ArrayList<>();

    @Override
    protected void collectCanvasContent(int canvasX, int canvasY, List<PartRenderState> parts, List<WireRenderState> wires, int mouseX, int mouseY)
    {
        parts.addAll(this.parts);
        wires.addAll(this.wires);
    }

    void update(@Nullable CompoundCircuitNode rootNode)
    {
        parts.clear();
        wires.clear();

        if (rootNode == null) return;

        for (Connector connector : Utils.concatArrays(rootNode.getInputs(), rootNode.getOutputs()))
        {
            Connection connection = new Connection(connector.type());
            connection.setPos(connector.pos(), connector.port().toPartRotation());
            connection.setPortDir(connector.dir());
            parts.add(new PartRenderState(connection));
        }
        rootNode.forAllNodes(entry ->
        {
            IconConfig icon = CircuitUtils.makeIconConfig(entry.node());
            parts.add(new PartRenderState(entry.pos(), icon, entry.rotation()));
        });
        rootNode.getWires().forEach(wire ->
        {
            WireRenderState renderState = new WireRenderState(wire.getWireType(), wire.getColor(), new ArrayList<>(), new ArrayList<>());
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
            renderState.sections().addAll(sections);
            if (!renderState.isEmpty())
            {
                wires.add(renderState);
            }
        });
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
