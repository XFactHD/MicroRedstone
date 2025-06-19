package io.github.xfacthd.microredstone.common.circuit.node.special;

import io.github.xfacthd.microredstone.common.circuit.compiler.ClockFieldAppender;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.compiler.NodeFieldAppender;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.Arrays;
import java.util.List;

public final class CompoundCircuitNode extends CircuitNode
{
    private static final WirePair[] EMPTY_ARRAY = new WirePair[0];

    private final List<NodeEntry<CircuitNode>> childNodes;
    private final List<ClockCircuitNode> clockNodes;
    private final List<BufferCircuitNode> bufferNodes;
    private final int wireCount;
    private final EvalContext.Nested nestedContext;

    public CompoundCircuitNode(
            List<NodeEntry<CircuitNode>> childNodes,
            List<ClockCircuitNode> clockNodes,
            List<BufferCircuitNode> bufferNodes,
            int wireCount,
            List<Connector> inputs,
            List<Connector> outputs
    )
    {
        super(inputs, outputs);
        this.childNodes = childNodes;
        this.clockNodes = clockNodes;
        this.bufferNodes = bufferNodes;
        this.wireCount = wireCount;
        this.nestedContext = new EvalContext.Nested(wireCount);
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        for (ClockCircuitNode clock : clockNodes)
        {
            clock.evaluate(nestedContext, EMPTY_ARRAY, EMPTY_ARRAY);
        }
        nestedContext.prepare(context, inputs);
        for (NodeEntry<CircuitNode> child : childNodes)
        {
            child.evaluate(nestedContext);
        }
        nestedContext.flush(context, outputs);
        for (BufferCircuitNode buffer : bufferNodes)
        {
            buffer.evaluate(nestedContext, EMPTY_ARRAY, EMPTY_ARRAY);
        }
    }

    public void compile(GeneratorAdapter methodGen, ClockFieldAppender clockFieldAppender, NodeFieldAppender fieldAppender, Type selfType)
    {
        int contextLocal = methodGen.newLocal(CircuitCompiler.NESTED_EVAL_CONTEXT_TYPE);
        LocalWireMapper localWires = new LocalWireMapper(methodGen, contextLocal, wireCount, childNodes, bufferNodes, outputs);

        compileContextPrepare(methodGen, selfType, localWires);

        for (ClockCircuitNode clock : clockNodes)
        {
            clock.compile(methodGen, clockFieldAppender, selfType, localWires);
        }

        compileNodeEval(methodGen, fieldAppender, selfType, localWires);
        compileContextFlush(methodGen, contextLocal);

        for (BufferCircuitNode buffer : bufferNodes)
        {
            buffer.compile(methodGen, localWires);
        }
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

    private void compileNodeEval(GeneratorAdapter methodGen, NodeFieldAppender fieldAppender, Type selfType, LocalWireMapper localWires)
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
            NodeFieldAppender fieldAppender,
            Type selfType,
            LocalWireMapper localWires,
            NodeEntry<CircuitNode> child
    )
    {
        String fieldName = fieldAppender.addNewField();
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

    @SuppressWarnings("unused") // Used in generated CompiledCircuitNode constructors
    public List<NodeEntry<CircuitNode>> getChildNodes()
    {
        return childNodes;
    }

    @SuppressWarnings("unused") // Used in generated CompiledCircuitNode constructors
    public int getWireCount()
    {
        return wireCount;
    }
}
