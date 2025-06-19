package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.Arrays;
import java.util.List;

public final class LocalWireMapper
{
    private final GeneratorAdapter methodGen;
    private final int contextLocal;
    private final int[] wireLocals;
    private final boolean[] fromLocal;
    private final boolean[] fromContext;

    public LocalWireMapper(
            GeneratorAdapter methodGen,
            int contextLocal,
            int wireCount,
            List<NodeEntry<CircuitNode>> childNodes,
            List<BufferCircuitNode> bufferNodes,
            Connector[] outputs
    )
    {
        this.methodGen = methodGen;
        this.contextLocal = contextLocal;
        this.wireLocals = new int[wireCount];
        Arrays.fill(wireLocals, -1);
        this.fromLocal = new boolean[wireCount];
        this.fromContext = new boolean[wireCount];
        // Determine which ioPorts are read from a local var and which ioPorts are read from the context.
        // This is used to decide where results of inlined nodes need to be written and whether results from complex
        // nodes also need to be copied to local variables
        for (NodeEntry<CircuitNode> child : childNodes)
        {
            boolean[] array = child.node() instanceof PrimitiveCircuitNode ? fromLocal : fromContext;
            for (WirePair input : child.inputs())
            {
                array[input.external()] = true;
            }
            // Special-case the bundle packer since it does a read-modify-write on its output
            if (child.node() instanceof BundlePackerCircuitNode)
            {
                array[child.outputs()[0].external()] = true;
            }
        }
        for (BufferCircuitNode buffer : bufferNodes)
        {
            fromLocal[buffer.getInputWire()] = true;
            fromContext[buffer.getOutputWire()] = true; // Buffer output must always be read from context
        }
        for (Connector out : outputs)
        {
            fromContext[out.wire()] = true;
        }
    }

    public int getContextLocal()
    {
        return contextLocal;
    }

    public boolean hasLocal(int wire)
    {
        return wireLocals[wire] != -1;
    }

    public int getLocal(int wire)
    {
        int local = wireLocals[wire];
        if (local == -1)
        {
            local = wireLocals[wire] = methodGen.newLocal(Type.SHORT_TYPE);
        }
        return local;
    }

    public boolean isReadFromLocal(int wire)
    {
        return fromLocal[wire];
    }

    public boolean isReadFromContext(int wire)
    {
        return fromContext[wire];
    }

    /**
     * Generates instructions for storing an on-stack value to a local variable and/or to the context
     */
    public void generateStore(int outputWire)
    {
        boolean writeToLocal = isReadFromLocal(outputWire);
        boolean writeToContext = isReadFromContext(outputWire);
        if (writeToLocal && writeToContext)
        {
            methodGen.dup();
        }
        if (writeToLocal)
        {
            int outputLocal = getLocal(outputWire);
            methodGen.storeLocal(outputLocal);
        }
        if (writeToContext)
        {
            methodGen.loadLocal(contextLocal);
            methodGen.swap();
            methodGen.push(outputWire);
            methodGen.swap();
            methodGen.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_STORE_MTH);
        }
    }
}
