package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public final class BundlePackerCircuitNode extends PrimitiveCircuitNode
{
    private final int bitIndex;
    private final int invBitMask;
    private final int inputWire;
    private final int outputWire;
    private boolean lastPacker = false;

    public BundlePackerCircuitNode(int bitIndex, Connector input, Connector output)
    {
        super(List.of(input), List.of(output));
        this.bitIndex = bitIndex;
        this.invBitMask = ~(1 << bitIndex);
        this.inputWire = input.wire();
        this.outputWire = output.wire();
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short input = context.loadInput(inputWire);
        short existing = context.loadInput(outputWire);
        short output = (short) (input << bitIndex);
        context.storeOutput(outputWire, (short) ((existing & invBitMask) | output));
    }

    @Override
    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        boolean outputPreviouslyWritten = localWires.hasLocal(outputWire);
        int inputLocal = localWires.getLocal(inputWire);
        int outputLocal = localWires.getLocal(outputWire);

        // Multiple bundle packers can write to the same bundled wire, so the packer can't just overwrite the output
        if (!outputPreviouslyWritten)
        {
            methodGen.loadLocal(localWires.getContextLocal());
            methodGen.push(outputWire);
            methodGen.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_LOAD_MTH);
            methodGen.storeLocal(outputLocal);
        }
        methodGen.loadLocal(inputLocal);
        methodGen.push(bitIndex);
        methodGen.math(GeneratorAdapter.SHL, Type.SHORT_TYPE);
        methodGen.loadLocal(outputLocal);
        methodGen.push(invBitMask);
        methodGen.math(GeneratorAdapter.AND, Type.SHORT_TYPE);
        methodGen.math(GeneratorAdapter.OR, Type.SHORT_TYPE);
        // Optimize out unnecessary context writes. This is safe because all nodes reading the
        // packer's output depend on all packers and are therefore sorted after all of them
        if (lastPacker)
        {
            localWires.generateStore(outputWire);
        }
        else
        {
            methodGen.storeLocal(outputLocal);
        }
    }

    public void markAsLast()
    {
        lastPacker = true;
    }
}
