package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

record CompilationKey(List<NodeKey<CompilationKey>> nestedCompoundNodes, List<NodeKey<? extends CircuitNode>> primitiveNodes, List<WireType> wires)
{
    static CompilationKey of(CompoundCircuitNode compound)
    {
        List<NodeKey<CompilationKey>> nestedCompoundNodes = new ObjectArrayList<>();
        List<NodeKey<? extends CircuitNode>> primitiveNodes = new ObjectArrayList<>();
        compound.forAllNodes(entry ->
        {
            if (entry.node() instanceof CompoundCircuitNode nestedCompound)
            {
                CompilationKey nestedKey = CompilationKey.of(nestedCompound);
                nestedCompoundNodes.add(NodeKey.of(nestedKey, entry));
            }
            else
            {
                primitiveNodes.add(NodeKey.of(entry.node(), entry));
            }
        });

        List<WireType> wires = compound.getWires()
                .stream()
                .map(Wire::getWireType)
                .collect(Collectors.toCollection(ReferenceArrayList::new));

        return new CompilationKey(nestedCompoundNodes, primitiveNodes, wires);
    }

    record NodeKey<T>(T node, LongList inputs, LongList outputs)
    {
        private static <T> NodeKey<T> of(T node, NodeEntry<?> srcEntry)
        {
            return new NodeKey<>(node, mapIO(srcEntry.inputs()), mapIO(srcEntry.outputs()));
        }

        private static LongList mapIO(WirePair[] pairs)
        {
            return Arrays.stream(pairs)
                    .mapToLong(pair -> (long) pair.external() << 32 | pair.internal())
                    .collect(LongArrayList::new, LongArrayList::add, LongArrayList::addAll);
        }
    }
}
