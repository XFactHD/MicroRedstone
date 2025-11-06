package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.compiler.EvalMethodCompiler;
import io.github.xfacthd.microredstone.common.circuit.compiler.FieldAppender;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ReferencePrototypeNode;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class CompoundCircuitNode extends RootCircuitNode implements Iterable<NodeEntry<?>>
{
    public static final MapCodec<CompoundCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(CompoundCircuitNode::getName),
            NodeEntry.CODEC.listOf().fieldOf("child_nodes").forGetter(node -> node.childNodes),
            NodeEntry.codec(ClockCircuitNode.CODEC.codec()).listOf().fieldOf("clock_nodes").forGetter(node -> node.clockNodes),
            NodeEntry.codec(BufferCircuitNode.CODEC.codec()).listOf().fieldOf("buffer_nodes").forGetter(node -> node.bufferNodes),
            Wire.CODEC.listOf().fieldOf("wires").forGetter(node -> node.wires),
            Connector.CODEC.listOf().fieldOf("inputs").forGetter(node -> List.of(node.inputs)),
            Connector.CODEC.listOf().fieldOf("outputs").forGetter(node -> List.of(node.outputs))
    ).apply(inst, CompoundCircuitNode::new));
    public static final StreamCodec<ByteBuf, CompoundCircuitNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CompoundCircuitNode::getName,
            NodeEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> node.childNodes,
            NodeEntry.streamCodec(ClockCircuitNode.STREAM_CODEC).apply(ByteBufCodecs.list()),
            node -> node.clockNodes,
            NodeEntry.streamCodec(BufferCircuitNode.STREAM_CODEC).apply(ByteBufCodecs.list()),
            node -> node.bufferNodes,
            Wire.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> node.wires,
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.inputs),
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.outputs),
            CompoundCircuitNode::new
    );

    private final String name;
    private final List<NodeEntry<CircuitNode>> childNodes;
    private final List<NodeEntry<ClockCircuitNode>> clockNodes;
    private final List<NodeEntry<BufferCircuitNode>> bufferNodes;
    private final List<Wire> wires;
    private final EvalContext.Nested nestedContext;

    public CompoundCircuitNode(
            String name,
            List<NodeEntry<CircuitNode>> childNodes,
            List<NodeEntry<ClockCircuitNode>> clockNodes,
            List<NodeEntry<BufferCircuitNode>> bufferNodes,
            List<Wire> wires,
            List<Connector> inputs,
            List<Connector> outputs
    )
    {
        super(inputs, outputs);
        this.name = name;
        this.childNodes = childNodes;
        this.clockNodes = clockNodes;
        this.bufferNodes = bufferNodes;
        this.wires = wires;
        this.nestedContext = new EvalContext.Nested(wires.size());
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs, @Nullable WireStates wireStates)
    {
        nestedContext.prepare(context, inputs);
        for (NodeEntry<ClockCircuitNode> clock : clockNodes)
        {
            clock.evaluate(nestedContext);
        }
        for (NodeEntry<CircuitNode> child : childNodes)
        {
            child.evaluate(nestedContext);
        }
        nestedContext.flush(context, outputs);
        if (wireStates != null)
        {
            for (int i = 0; i < wires.size(); i++)
            {
                if (wires.get(i).getWireType() != WireType.BUNDLED)
                {
                    wireStates.set(i, nestedContext.loadInput(i));
                }
            }
        }
        for (NodeEntry<BufferCircuitNode> buffer : bufferNodes)
        {
            buffer.evaluate(nestedContext);
        }
    }

    @Override
    public CircuitState serializeState()
    {
        IntList bufferStates = new IntArrayList();
        IntList clockCounters = new IntArrayList();
        IntList clockStates = new IntArrayList();
        serializeState(bufferStates, clockCounters, clockStates);
        return new CircuitState(bufferStates.toIntArray(), clockCounters.toIntArray(), clockStates.toIntArray());
    }

    private void serializeState(IntList bufferStates, IntList clockCounters, IntList clockStates)
    {
        for (NodeEntry<BufferCircuitNode> buffer : bufferNodes)
        {
            bufferStates.add(nestedContext.loadInput(buffer.node().getOutputWire()));
        }
        for (NodeEntry<ClockCircuitNode> clockNode : clockNodes)
        {
            ClockCircuitNode clock = clockNode.node();
            clockCounters.add(clock.getCounter());
            clockStates.add(clock.getState());
        }
        for (NodeEntry<CircuitNode> node : childNodes)
        {
            if (node.node() instanceof CompoundCircuitNode compound)
            {
                compound.serializeState(bufferStates, clockCounters, clockStates);
            }
        }
    }

    @Override
    public void applyState(CircuitState state)
    {
        IntListIterator bufferStates = IntIterators.wrap(state.bufferStates());
        IntListIterator clockCounters = IntIterators.wrap(state.clockCounters());
        IntListIterator clockStates = IntIterators.wrap(state.clockStates());
        applyState(bufferStates, clockCounters, clockStates);
    }

    private void applyState(IntListIterator bufferStates, IntListIterator clockCounters, IntListIterator clockStates)
    {
        for (NodeEntry<BufferCircuitNode> buffer : bufferNodes)
        {
            nestedContext.storeOutput(buffer.node().getOutputWire(), (short) bufferStates.nextInt());
        }
        for (NodeEntry<ClockCircuitNode> clockNode : clockNodes)
        {
            clockNode.node().applyState(clockCounters.nextInt(), clockStates.nextInt());
        }

        for (NodeEntry<CircuitNode> node : childNodes)
        {
            if (node.node() instanceof CompoundCircuitNode compound)
            {
                compound.applyState(bufferStates, clockCounters, clockStates);
            }
        }
    }

    public void compile(EvalMethodCompiler compiler)
    {
        compiler.compileRootEval(this, CompoundCircuitNode::compileNodeEval);
    }

    private static void compileNodeEval(
            EvalMethodCompiler compiler,
            GeneratorAdapter generator,
            Type selfType,
            FieldAppender fieldAppender,
            LocalWireMapper localWires,
            CompoundCircuitNode compoundNode
    )
    {
        for (NodeEntry<ClockCircuitNode> clock : compoundNode.clockNodes)
        {
            clock.node().compile(generator, fieldAppender, selfType, localWires);
        }
        for (NodeEntry<BufferCircuitNode> buffer : compoundNode.bufferNodes)
        {
            buffer.node().compileReadBack(generator, selfType, fieldAppender, localWires);
        }
        for (NodeEntry<CircuitNode> child : compoundNode.childNodes)
        {
            if (child.node() instanceof PrimitiveCircuitNode primitive)
            {
                primitive.compile(generator, localWires);
            }
            else if (child.node() instanceof CompoundCircuitNode nested)
            {
                compiler.compileNestedEval(generator, localWires, nested, child.inputs(), child.outputs(), CompoundCircuitNode::compileNodeEval);
            }
        }
        for (NodeEntry<BufferCircuitNode> buffer : compoundNode.bufferNodes)
        {
            buffer.node().compileCapture(generator, selfType, fieldAppender, localWires);
        }
    }

    public String getName()
    {
        return name;
    }

    public List<Wire> getWires()
    {
        return wires;
    }

    @Override
    public int getWireCount()
    {
        return wires.size();
    }

    public void forAllNodes(Consumer<NodeEntry<? extends CircuitNode>> consumer)
    {
        childNodes.forEach(consumer);
        clockNodes.forEach(consumer);
        bufferNodes.forEach(consumer);
    }

    @Override
    public Iterator<NodeEntry<?>> iterator()
    {
        return Iterables.concat(childNodes, clockNodes, bufferNodes).iterator();
    }

    @Override
    public CompoundCircuitNode serializable()
    {
        return this;
    }

    @Override
    public void release() { }

    @Override
    public PrototypeNode disassemble()
    {
        return ReferencePrototypeNode.create(this, null);
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_COMPOUND.value();
    }

    @Override
    public boolean equals(Object obj)
    {
        // We only care about proper equality in the compilation key where this type will never be encountered
        return obj == this;
    }

    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }
}
