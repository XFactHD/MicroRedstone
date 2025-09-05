package io.github.xfacthd.microredstone.client.screen.workbench.part;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public sealed interface FloatingNode
{
    static FloatingNode of(PlaceableNode node, @Nullable NodePos lastPos)
    {
        return switch (node)
        {
            case PrototypeNode proto -> new Part(proto, lastPos, proto.getRotation());
            case Connection con -> new EdgeConnection(con, lastPos, con.getRotation());
            default -> throw new IllegalArgumentException("Invalid node: " + node);
        };
    }

    PlaceableNode node();

    @Nullable
    NodePos lastPos();

    int rotation();

    default FloatingNode rotate(int offset)
    {
        return withRotation(Mth.positiveModulo(rotation() + offset, 4));
    }

    FloatingNode withRotation(int rotation);

    boolean canPlaceAt(CircuitCanvas canvas, NodePos pos);

    void placeAt(CircuitCanvas canvas, NodePos pos, boolean revertToLast);

    void delete(CircuitCanvas canvas);

    record Part(PrototypeNode node, @Nullable NodePos lastPos, int rotation) implements FloatingNode
    {
        @Override
        public FloatingNode withRotation(int rotation)
        {
            return new Part(node, lastPos, rotation);
        }

        @Override
        public boolean canPlaceAt(CircuitCanvas canvas, NodePos pos)
        {
            return pos.equals(lastPos) || !canvas.isNodeOccupied(pos);
        }

        @Override
        public void placeAt(CircuitCanvas canvas, NodePos pos, boolean revertToLast)
        {
            boolean posMatches = pos.equals(lastPos);
            // Dragging a node doesn't actually remove it from the grid -> no action required if pos and rotation match
            if (rotation == node.getRotation() && posMatches) return;

            PartGrid partGrid = canvas.getPartGrid();
            PartSetMode mode = posMatches ? PartSetMode.ROTATE : PartSetMode.MOVE;
            if (lastPos != null)
            {
                partGrid.setPartNode(lastPos, null, 0, mode);
            }
            else
            {
                mode = PartSetMode.ADD;
            }
            partGrid.setPartNode(pos, node, rotation, mode);
        }

        @Override
        public void delete(CircuitCanvas canvas)
        {
            PartGrid partGrid = canvas.getPartGrid();
            NodePos pos = Objects.requireNonNull(lastPos);
            partGrid.setPartNode(pos, null, 0, PartSetMode.REMOVE);
        }
    }

    record EdgeConnection(Connection node, @Nullable NodePos lastPos, int rotation) implements FloatingNode
    {
        @Override
        public FloatingNode withRotation(int rotation)
        {
            return new EdgeConnection(node, lastPos, rotation);
        }

        @Override
        public boolean canPlaceAt(CircuitCanvas canvas, NodePos pos)
        {
            if (!pos.equals(lastPos) && canvas.isNodeOccupied(pos)) return false;

            Connection[] connections = canvas.getRootNode().getConnections();
            Connection existing = connections[Port.ofPartRotation(rotation).ordinal()];
            if (existing != null && existing != node)
            {
                return false;
            }

            return switch (rotation)
            {
                case 0 -> pos.x() == 0;
                case 1 -> pos.y() == 0;
                case 2 -> pos.x() == canvas.getGridWidth() - 1;
                case 3 -> pos.y() == canvas.getGridHeight() - 1;
                default -> throw new IllegalArgumentException("Invalid rotation");
            };
        }

        @Override
        public void placeAt(CircuitCanvas canvas, NodePos pos, boolean revertToLast)
        {
            // Dragging a connection doesn't actually remove it from the grid -> no action required if pos and rotation match
            if (pos.equals(lastPos) && node.getRotation() == rotation) return;

            Connection[] connections = canvas.getRootNode().getConnections();
            int placeRot = revertToLast ? node.getRotation() : rotation;
            Port prevPort = Port.ofPartRotation(node.getRotation());
            Port newPort = Port.ofPartRotation(placeRot);
            if (lastPos != null)
            {
                if (newPort != prevPort)
                {
                    connections[prevPort.ordinal()] = null;
                }
                canvas.getWireGrid().trimConnectedWires(lastPos, node);
            }
            node.setPos(pos, placeRot);
            if (lastPos == null || newPort != prevPort)
            {
                connections[newPort.ordinal()] = node;
            }
        }

        @Override
        public void delete(CircuitCanvas canvas)
        {
            Connection[] connections = canvas.getRootNode().getConnections();
            NodePos pos = Objects.requireNonNull(lastPos);
            Port port = Port.ofPartRotation(node.getRotation());
            connections[port.ordinal()] = null;
            canvas.getWireGrid().trimConnectedWires(pos, node);
        }
    }
}
