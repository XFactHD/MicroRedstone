package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.compiler.EvalMethodCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.compiler.FieldAppender;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.Arrays;
import java.util.List;

public final class CompoundCircuitNode extends RootCircuitNode
{
    public static final MapCodec<CompoundCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            NodeEntry.CODEC.listOf().fieldOf("child_nodes").forGetter(CompoundCircuitNode::getChildNodes),
            NodeEntry.codec(ClockCircuitNode.CODEC.codec()).listOf().fieldOf("clock_nodes").forGetter(node -> node.clockNodes),
            NodeEntry.codec(BufferCircuitNode.CODEC.codec()).listOf().fieldOf("buffer_nodes").forGetter(node -> node.bufferNodes),
            Wire.CODEC.listOf().fieldOf("wires").forGetter(CompoundCircuitNode::getWires),
            Connector.CODEC.listOf().fieldOf("inputs").forGetter(node -> List.of(node.inputs)),
            Connector.CODEC.listOf().fieldOf("outputs").forGetter(node -> List.of(node.outputs))
    ).apply(inst, CompoundCircuitNode::new));
    public static final StreamCodec<ByteBuf, CompoundCircuitNode> STREAM_CODEC = StreamCodec.composite(
            NodeEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            CompoundCircuitNode::getChildNodes,
            NodeEntry.streamCodec(ClockCircuitNode.STREAM_CODEC).apply(ByteBufCodecs.list()),
            node -> node.clockNodes,
            NodeEntry.streamCodec(BufferCircuitNode.STREAM_CODEC).apply(ByteBufCodecs.list()),
            node -> node.bufferNodes,
            Wire.STREAM_CODEC.apply(ByteBufCodecs.list()),
            CompoundCircuitNode::getWires,
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.inputs),
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.outputs),
            CompoundCircuitNode::new
    );

    private final List<NodeEntry<CircuitNode>> childNodes;
    private final List<NodeEntry<ClockCircuitNode>> clockNodes;
    private final List<NodeEntry<BufferCircuitNode>> bufferNodes;
    private final List<Wire> wires;
    private final EvalContext.Nested nestedContext;

    public CompoundCircuitNode(
            List<NodeEntry<CircuitNode>> childNodes,
            List<NodeEntry<ClockCircuitNode>> clockNodes,
            List<NodeEntry<BufferCircuitNode>> bufferNodes,
            List<Wire> wires,
            List<Connector> inputs,
            List<Connector> outputs
    )
    {
        super(inputs, outputs);
        this.childNodes = childNodes;
        this.clockNodes = clockNodes;
        this.bufferNodes = bufferNodes;
        this.wires = wires;
        this.nestedContext = new EvalContext.Nested(wires.size());
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        for (NodeEntry<ClockCircuitNode> clock : clockNodes)
        {
            clock.evaluate(nestedContext);
        }
        nestedContext.prepare(context, inputs);
        for (NodeEntry<CircuitNode> child : childNodes)
        {
            child.evaluate(nestedContext);
        }
        nestedContext.flush(context, outputs);
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

        // Abort when this state was written by a compiled node as it doesn't write nested node state
        // TODO: remove when nested nodes are merged into the root compiled node
        if (!bufferStates.hasNext() && !clockCounters.hasNext() && !clockStates.hasNext()) return;

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
        compiler.compileRootEval(wires.size(), childNodes, bufferNodes, outputs, (generator, selfType, fieldAppender, localWires) ->
        {
            compileContextPrepare(generator, selfType, localWires);

            for (NodeEntry<ClockCircuitNode> clock : clockNodes)
            {
                clock.node().compile(generator, fieldAppender, selfType, localWires);
            }
            for (NodeEntry<BufferCircuitNode> buffer : bufferNodes)
            {
                buffer.node().compileReadBack(generator, selfType, fieldAppender, localWires);
            }

            compileNodeEval(generator, fieldAppender, selfType, localWires);
            compileContextFlush(generator, localWires.getContextLocal());

            for (NodeEntry<BufferCircuitNode> buffer : bufferNodes)
            {
                buffer.node().compileCapture(generator, selfType, fieldAppender, localWires);
            }
        });
    }

    private void compileContextPrepare(GeneratorAdapter methodGen, Type selfType, LocalWireMapper localWires)
    {
        int contextLocal = localWires.getContextLocal();

        methodGen.loadThis();
        methodGen.getField(selfType, "nestedContext", CircuitCompiler.NESTED_EVAL_CONTEXT_TYPE);
        methodGen.dup();
        methodGen.storeLocal(contextLocal);
        methodGen.loadArg(0); // EvalContext
        methodGen.loadArg(1); // int[] inputs
        methodGen.invokeVirtual(CircuitCompiler.NESTED_EVAL_CONTEXT_TYPE, CircuitCompiler.NESTED_EVAL_CONTEXT_PREPARE_MTH);

        int[] inWires = Arrays.stream(inputs)
                .mapToInt(Connector::wire)
                .filter(localWires::isReadFromLocal)
                .toArray();
        if (inWires.length > 0)
        {
            methodGen.loadLocal(contextLocal);

            int max = inWires.length - 1;
            for (int i = 0; i <= max; i++)
            {
                if (i < max) methodGen.dup();

                int wire = inWires[i];
                methodGen.push(wire);
                methodGen.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_LOAD_MTH);
                methodGen.storeLocal(localWires.getLocal(wire));
            }
        }
    }

    private static void compileContextFlush(GeneratorAdapter methodGen, int contextLocal)
    {
        methodGen.loadLocal(contextLocal);
        methodGen.loadArg(0); // EvalContext
        methodGen.loadArg(2); // int[] outputs
        methodGen.invokeVirtual(CircuitCompiler.NESTED_EVAL_CONTEXT_TYPE, CircuitCompiler.NESTED_EVAL_CONTEXT_FLUSH_MTH);
    }

    private void compileNodeEval(GeneratorAdapter methodGen, FieldAppender fieldAppender, Type selfType, LocalWireMapper localWires)
    {
        for (NodeEntry<CircuitNode> child : childNodes)
        {
            if (child.node() instanceof PrimitiveCircuitNode primitive)
            {
                primitive.compile(methodGen, localWires);
            }
            else
            {
                compileComplexNodeEval(methodGen, fieldAppender, selfType, localWires, child);
            }
        }
    }

    // TODO: Stage 1: "Inline" nested nodes by compiling them into separate methods with associated nested contexts stored in fields
    //                instead of calling into entirely separate nodes stored in a field
    //       Stage 2: If feasible, merge contexts of the nested nodes into this node's context, pass input locals as method params and return output
    //                locals as a packed int (probably easiest to unify the root eval method by giving it the same treatment) - requires special care
    //                with respect to buffer nodes
    private static void compileComplexNodeEval(
            GeneratorAdapter methodGen,
            FieldAppender fieldAppender,
            Type selfType,
            LocalWireMapper localWires,
            NodeEntry<CircuitNode> child
    )
    {
        String fieldName = fieldAppender.addNodeField();
        int contextLocal = localWires.getContextLocal();

        methodGen.loadThis();
        methodGen.getField(selfType, fieldName, CircuitCompiler.NODE_ENTRY_TYPE);
        methodGen.loadLocal(contextLocal);
        methodGen.invokeVirtual(CircuitCompiler.NODE_ENTRY_TYPE, CircuitCompiler.NODE_ENTRY_EVAL_MTH);

        int[] outWires = Arrays.stream(child.outputs())
                .mapToInt(WirePair::external)
                .filter(localWires::isReadFromLocal)
                .toArray();

        if (outWires.length > 0)
        {
            methodGen.loadLocal(contextLocal);

            int max = outWires.length - 1;
            for (int i = 0; i <= max; i++)
            {
                if (i < max) methodGen.dup();

                int wire = outWires[i];
                methodGen.push(wire);
                methodGen.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_LOAD_MTH);
                methodGen.storeLocal(localWires.getLocal(wire));
            }
        }
    }

    public List<NodeEntry<CircuitNode>> getChildNodes()
    {
        return childNodes;
    }

    public List<Wire> getWires()
    {
        return wires;
    }

    @SuppressWarnings("unused") // Used in CircuitCompiler
    public int getWireCount()
    {
        return wires.size();
    }

    @Override
    public CompoundCircuitNode serializable()
    {
        return this;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_COMPOUND.value();
    }
}
