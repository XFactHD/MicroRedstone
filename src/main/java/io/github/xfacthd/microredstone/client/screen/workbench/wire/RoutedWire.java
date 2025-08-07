package io.github.xfacthd.microredstone.client.screen.workbench.wire;

import com.google.common.base.Preconditions;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public record RoutedWire(Wire wire, List<Section> sections)
{
    public RoutedWire
    {
        sections = new ArrayList<>(sections);
    }

    public Set<Port> getBlockedDirsAt(NodePos pos)
    {
        for (Wire.Node node : wire.getNodes())
        {
            if (node.pos().equals(pos))
            {
                return switch (node)
                {
                    case Wire.Node.Branch branch -> branch.ports();
                    case Wire.Node.Connection ignored -> EnumSet.allOf(Port.class);
                    case Wire.Node.Dangling ignored -> Set.of();
                };
            }
        }
        for (Section section : sections)
        {
            if (section.intersects(pos))
            {
                if (section.isHorizontal())
                {
                    return EnumSet.of(Port.LEFT, Port.RIGHT);
                }
                else
                {
                    return EnumSet.of(Port.UP, Port.DOWN);
                }
            }
        }
        return Set.of();
    }

    public void fixNodes()
    {
        Map<NodePos, List<Wire.Node.Branch>> branchNodes = new HashMap<>();
        List<Wire.Node> nodes = wire.getNodes();
        for (Wire.Node node : nodes)
        {
            if (node instanceof Wire.Node.Branch branch)
            {
                branchNodes.computeIfAbsent(node.pos(), $ -> new ArrayList<>()).add(branch);
            }
        }
        for (List<Wire.Node.Branch> branches : branchNodes.values())
        {
            if (branches.size() > 1)
            {
                nodes.removeAll(branches);

                Wire.Node.Branch firstBranch = branches.getFirst();
                Wire.Node.Branch branch = new Wire.Node.Branch(firstBranch.pos(), firstBranch.ports(), firstBranch.neighbors());
                for (int i = 1; i < branches.size(); i++)
                {
                    Wire.Node.Branch other = branches.get(i);
                    branch.ports().addAll(other.ports());
                    branch.neighbors().addAll(other.neighbors());
                }
                nodes.add(branch);
            }
        }
    }

    public void forAllNodes(Consumer<NodePos> consumer)
    {
        wire.getNodes().forEach(node -> consumer.accept(node.pos()));
        sections.forEach(section -> section.forNonVertexNodes(consumer));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Wire.Node> T findNode(NodePos pos, Class<T> type, Predicate<T> filter)
    {
        for (Wire.Node node : wire.getNodes())
        {
            if (node.pos().equals(pos) && type.isInstance(node) && filter.test((T) node))
            {
                return (T) node;
            }
        }
        return null;
    }

    public void replaceSection(NodePos posOne, NodePos posTwoOld, NodePos posTwoNew)
    {
        for (int i = 0; i < sections.size(); i++)
        {
            Section section = sections.get(i);
            if (section.matches(posOne, posTwoOld))
            {
                sections.set(i, new Section(posOne, posTwoNew));
                break;
            }
        }
    }

    @Nullable
    public Section removeSection(NodePos posOne, NodePos posTwo)
    {
        for (int i = 0; i < sections.size(); i++)
        {
            Section section = sections.get(i);
            if (section.matches(posOne, posTwo))
            {
                return sections.remove(i);
            }
        }
        return null;
    }

    @Nullable
    public Section findIntersectedSection(NodePos pos)
    {
        for (Section section : sections)
        {
            if (section.intersects(pos))
            {
                return section;
            }
        }
        return null;
    }

    public record Section(NodePos posOne, NodePos posTwo)
    {
        public Section
        {
            Preconditions.checkArgument(posOne.x() == posTwo.x() || posOne.y() == posTwo.y());
        }

        public boolean isHorizontal()
        {
            return posOne.y() == posTwo.y();
        }

        public boolean matches(NodePos posOne, NodePos posTwo)
        {
            return (this.posOne.equals(posOne) && this.posTwo.equals(posTwo)) || (this.posOne.equals(posTwo) && this.posTwo.equals(posOne));
        }

        public boolean intersects(NodePos pos)
        {
            int minX = Math.min(posOne.x(), posTwo.x());
            int minY = Math.min(posOne.y(), posTwo.y());
            int maxX = Math.max(posOne.x(), posTwo.x());
            int maxY = Math.max(posOne.y(), posTwo.y());
            return pos.x() >= minX && pos.x() <= maxX && pos.y() >= minY && pos.y() <= maxY;
        }

        public void forNonVertexNodes(Consumer<NodePos> consumer)
        {
            int minX = Math.min(posOne.x(), posTwo.x());
            int minY = Math.min(posOne.y(), posTwo.y());
            int maxX = Math.max(posOne.x(), posTwo.x());
            int maxY = Math.max(posOne.y(), posTwo.y());
            if (maxX - minX <= 1 && maxY - minY <= 1) return;

            if (minX == maxX)
            {
                for (int y = minY + 1; y < maxY; y++)
                {
                    consumer.accept(new NodePos(minX, y));
                }
            }
            else if (minY == maxY)
            {
                for (int x = minX + 1; x < maxX; x++)
                {
                    consumer.accept(new NodePos(x, minY));
                }
            }
        }
    }
}
