package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public final class NotLogicCircuitNode extends PrimitiveCircuitNode
{
    private final int inputWire;
    private final int outputWire;
    private final int inversionMask;

    public NotLogicCircuitNode(PrimitivePrototypeNode prototype, List<Connector> inputs, Connector output)
    {
        super(inputs, List.of(output));
        PrimitivePrototypeNode.Type type = prototype.getType();
        if (type != PrimitivePrototypeNode.Type.NOT)
        {
            throw new IllegalArgumentException("Invalid logic op: " + type);
        }
        this.inputWire = inputs.getFirst().wire();
        this.outputWire = output.wire();
        this.inversionMask = prototype.isMultiBit() ? 0xFFFF : 0x1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short input = context.loadInput(inputWire);
        context.storeOutput(outputWire, (short) (input ^ inversionMask));
    }

    @Override
    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        int inputLocal = localWires.getLocal(inputWire);

        methodGen.loadLocal(inputLocal);
        methodGen.push(inversionMask);
        methodGen.math(GeneratorAdapter.XOR, Type.SHORT_TYPE);
        localWires.generateStore(outputWire);
    }
}
