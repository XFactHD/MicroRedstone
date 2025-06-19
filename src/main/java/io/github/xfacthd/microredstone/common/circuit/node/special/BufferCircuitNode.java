package io.github.xfacthd.microredstone.common.circuit.node.special;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

/**
 * Special buffer node to use for safely evaluating circular dependencies
 */
public final class BufferCircuitNode extends CircuitNode
{
    private final int inputWire;
    private final int outputWire;

    public BufferCircuitNode(Connector input, Connector output)
    {
        super(List.of(input), List.of(output));
        this.inputWire = input.wire();
        this.outputWire = output.wire();
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        context.copyState(inputWire, outputWire);
    }

    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        int inputLocal = localWires.getLocal(inputWire);

        methodGen.loadLocal(inputLocal);
        localWires.generateStore(outputWire);
    }

    public int getInputWire()
    {
        return inputWire;
    }

    public int getOutputWire()
    {
        return outputWire;
    }
}
