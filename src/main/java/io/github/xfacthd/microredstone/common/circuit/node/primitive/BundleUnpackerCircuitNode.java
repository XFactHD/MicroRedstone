package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public final class BundleUnpackerCircuitNode extends PrimitiveCircuitNode
{
    private final int bitIndex;
    private final int inputWire;
    private final int outputWire;

    public BundleUnpackerCircuitNode(int bitIndex, Connector input, Connector output)
    {
        super(List.of(input), List.of(output));
        this.bitIndex = bitIndex;
        this.inputWire = input.wire();
        this.outputWire = output.wire();
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short input = context.loadInput(inputWire);
        short output = (short) ((input >>> bitIndex) & 0x1);
        context.storeOutput(outputWire, output);
    }

    @Override
    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        int inputLocal = localWires.getLocal(inputWire);

        methodGen.loadLocal(inputLocal);
        methodGen.push(bitIndex);
        methodGen.math(GeneratorAdapter.USHR, Type.SHORT_TYPE);
        methodGen.push(0x1);
        methodGen.math(GeneratorAdapter.AND, Type.SHORT_TYPE);
        localWires.generateStore(outputWire);
    }
}
