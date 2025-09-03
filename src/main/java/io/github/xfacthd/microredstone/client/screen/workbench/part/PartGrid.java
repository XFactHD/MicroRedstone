package io.github.xfacthd.microredstone.client.screen.workbench.part;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

public final class PartGrid
{
    private final CircuitCanvas canvas;
    private final PrototypeNode[] partGrid = new PrototypeNode[CircuitCanvas.PART_COUNT];

    public PartGrid(CircuitCanvas canvas)
    {
        this.canvas = canvas;
    }

    @Nullable
    public PlaceableNode getPartNode(NodePos pos)
    {
        PrototypeNode node = partGrid[index(pos)];
        if (node != null) return node;

        for (Connection con : canvas.getRootNode().getConnections())
        {
            if (con != null && con.getPos().equals(pos))
            {
                return con;
            }
        }
        return null;
    }

    public void setPartNode(NodePos pos, @Nullable PrototypeNode node, int rotation, PartSetMode mode)
    {
        int idx = index(pos);
        CompoundPrototypeNode circuit = canvas.getRootNode();
        if (node == null)
        {
            PrototypeNode oldNode = partGrid[idx];
            if (oldNode != null)
            {
                if (mode.addOrRemove())
                {
                    circuit.removeChild(oldNode);
                }
                canvas.getWireGrid().trimConnectedWires(pos, oldNode);
                oldNode.clearWires();
            }
        }
        if (mode.writeGrid())
        {
            partGrid[idx] = node;
        }
        if (node != null)
        {
            node.setPos(pos);
            node.setRotation(rotation);
            if (mode.addOrRemove())
            {
                circuit.addChild(node);
            }
        }
    }

    public void forEach(Consumer<PrototypeNode> consumer)
    {
        for (PrototypeNode node : partGrid)
        {
            if (node != null)
            {
                consumer.accept(node);
            }
        }
    }

    public void clear()
    {
        Arrays.fill(partGrid, null);
    }

    private static int index(NodePos pos)
    {
        return pos.y() * CircuitCanvas.PART_COUNT_X + pos.x();
    }
}
