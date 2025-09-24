package io.github.xfacthd.microredstone.common.circuit.assembler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.pathelement.RootNodePathElement;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.DirectCyclicConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.IndirectCyclicConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnexpectedErrorProblem;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.WireCountMismatchProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.util.CountingProblemReporter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.ProblemReporter;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

@SuppressWarnings("UnstableApiUsage")
public final class CircuitAssembler
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Port[] PORTS = Port.values();

    @Nullable
    public static CompoundCircuitNode assemble(CompoundPrototypeNode node, ProblemReporter problemReporter)
    {
        CountingProblemReporter reporter = CountingProblemReporter.of(problemReporter);

        try
        {
            node.validate(reporter.forChild(new RootNodePathElement(node)));
        }
        catch (Throwable t)
        {
            reporter.report(new UnexpectedErrorProblem(t));
            LOGGER.error("Encountered an unexpected error validating the circuit prototype. This is a bug!", t);
        }
        if (reporter.hasIssues()) return null;

        List<ClockPrototypeNode> clockProtoNodes = new ArrayList<>();
        List<BufferPrototypeNode> bufferProtoNodes = new ArrayList<>();
        Graph<PrototypeNode> nodeGraph = buildNodeGraph(node, clockProtoNodes, bufferProtoNodes, reporter);
        if (nodeGraph == null) return null;

        List<PrototypeNode> childProtoNodes = buildSortedNodeList(nodeGraph, reporter);
        if (reporter.hasIssues()) return null;

        WireMapper wireMapper = new WireMapper();

        List<NodeEntry<ClockCircuitNode>> clockNodes = new ArrayList<>(clockProtoNodes.size());
        for (ClockPrototypeNode clock : clockProtoNodes)
        {
            buildPrimitiveNode(clockNodes, clock, wireMapper, ClockPrototypeNode::assemble);
        }
        List<NodeEntry<BufferCircuitNode>> bufferNodes = new ArrayList<>(bufferProtoNodes.size());
        for (BufferPrototypeNode buffer : bufferProtoNodes)
        {
            buildPrimitiveNode(bufferNodes, buffer, wireMapper, BufferPrototypeNode::assemble);
        }

        List<NodeEntry<CircuitNode>> childNodes = new ArrayList<>();
        for (PrototypeNode childNode : childProtoNodes)
        {
            switch (childNode)
            {
                case ClockPrototypeNode ignored -> throw new IllegalStateException();
                case BufferPrototypeNode ignored -> throw new IllegalStateException();
                case ReferencePrototypeNode reference ->
                {
                    CompoundCircuitNode assembled = reference.assemble(wireMapper);
                    WirePair[] inputs = Arrays.stream(assembled.getInputs())
                            .map(con ->
                            {
                                Wire wire = reference.getWireOrThrow(con.port());
                                int resolved = wireMapper.resolveWire(wire);
                                return new WirePair(resolved, con.wire());
                            })
                            .toArray(WirePair[]::new);
                    WirePair[] outputs = Arrays.stream(assembled.getOutputs())
                            .map(con ->
                            {
                                Wire wire = reference.getWireOrThrow(con.port());
                                int resolved = wireMapper.resolveWire(wire);
                                return new WirePair(resolved, con.wire());
                            })
                            .toArray(WirePair[]::new);
                    childNodes.add(new NodeEntry<>(assembled, reference.getPos(), reference.getRotation(), inputs, outputs));
                }
                default -> buildPrimitiveNode(childNodes, childNode, wireMapper, PrototypeNode::assemble);
            }
        }

        Int2ObjectMap<List<BundlePackerCircuitNode>> packersPerWire = new Int2ObjectOpenHashMap<>();
        for (NodeEntry<? extends CircuitNode> childNode : childNodes)
        {
            if (childNode.node() instanceof BundlePackerCircuitNode packer)
            {
                int wire = packer.getOutputs()[0].wire();
                packersPerWire.computeIfAbsent(wire, $ -> new ArrayList<>()).add(packer);
            }
        }

        List<Connector> inputs = new ArrayList<>();
        List<Connector> outputs = new ArrayList<>();
        Connection[] connections = node.getConnections();
        for (Port port : PORTS)
        {
            Connection connection = connections[port.ordinal()];
            if (connection == null) continue;

            Connector connector = connection.toConnector(port, wireMapper);
            switch (connection.getPortDir())
            {
                case INPUT -> inputs.add(connector);
                case OUTPUT -> outputs.add(connector);
            }
        }
        if (wireMapper.size() != node.getWireCount())
        {
            reporter.report(new WireCountMismatchProblem(node.getWireCount(), wireMapper.size()));
            return null;
        }

        List<Wire> wires = wireMapper.getSortedWireCopies();
        return new CompoundCircuitNode(childNodes, clockNodes, bufferNodes, wires, inputs, outputs);
    }

    private static <P extends PrototypeNode, C extends CircuitNode> void buildPrimitiveNode(
            List<NodeEntry<C>> output, P protoNode, WireMapper wireMapper, BiFunction<P, WireMapper, @Nullable C> assembler
    )
    {
        C assembled = assembler.apply(protoNode, wireMapper);
        if (assembled == null) return;

        WirePair[] inputs = Arrays.stream(assembled.getInputs())
                .map(con -> new WirePair(con.wire(), con.port().ordinal()))
                .toArray(WirePair[]::new);
        WirePair[] outputs = Arrays.stream(assembled.getOutputs())
                .map(con -> new WirePair(con.wire(), con.port().ordinal()))
                .toArray(WirePair[]::new);
        output.add(new NodeEntry<>(assembled, protoNode.getPos(), protoNode.getRotation(), inputs, outputs));
    }

    @Nullable
    private static Graph<PrototypeNode> buildNodeGraph(
            CompoundPrototypeNode node,
            List<ClockPrototypeNode> clockProtoNodes,
            List<BufferPrototypeNode> bufferProtoNodes,
            ProblemReporter reporter
    )
    {
        MutableGraph<PrototypeNode> graph = GraphBuilder.directed().nodeOrder(ElementOrder.insertion()).build();
        Map<Wire, PrototypeNode> drivers = new IdentityHashMap<>();
        Multimap<Wire, PrototypeNode> readers = HashMultimap.create();

        for (PrototypeNode childNode : node.getChildNodes())
        {
            if (childNode instanceof ClockPrototypeNode clock)
            {
                clockProtoNodes.add(clock);
                continue;
            }
            if (childNode instanceof BufferPrototypeNode buffer)
            {
                bufferProtoNodes.add(buffer);
                continue;
            }

            graph.addNode(childNode);
            for (Wire input : childNode.getConnectedWires(PortDir.INPUT))
            {
                readers.put(input, childNode);
            }
            for (Wire output : childNode.getConnectedWires(PortDir.OUTPUT))
            {
                drivers.put(output, childNode);
            }
        }

        for (Map.Entry<Wire, PrototypeNode> driver : drivers.entrySet())
        {
            Wire wire = driver.getKey();
            PrototypeNode driverNode = driver.getValue();
            for (PrototypeNode readerNode : readers.get(wire))
            {
                if (driverNode == readerNode)
                {
                    reporter.report(new DirectCyclicConnectionProblem(driverNode));
                    return null;
                }
                graph.putEdge(driverNode, Objects.requireNonNull(readerNode));
            }
        }
        return graph;
    }

    private static List<PrototypeNode> buildSortedNodeList(Graph<PrototypeNode> nodeGraph, ProblemReporter reporter)
    {
        try
        {
            return TopologicalSort.topologicalSort(nodeGraph, null);
        }
        catch (CyclePresentException e)
        {
            reporter.report(new IndirectCyclicConnectionProblem(e.getCycles()));
            return List.of();
        }
    }

    private CircuitAssembler() { }
}
