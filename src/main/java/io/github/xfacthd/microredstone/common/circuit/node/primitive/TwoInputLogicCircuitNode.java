package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public final class TwoInputLogicCircuitNode extends PrimitiveCircuitNode
{
    private final PrimitivePrototypeNode.Type type;
    private final int inputOneWire;
    private final int inputTwoWire;
    private final int outputWire;
    private final boolean invertResult;
    private final int inversionMask;

    public TwoInputLogicCircuitNode(PrimitivePrototypeNode prototype, List<Connector> inputs, Connector output)
    {
        super(inputs, List.of(output));
        this.type = prototype.getType();
        this.inputOneWire = inputs.get(0).wire();
        this.inputTwoWire = inputs.get(1).wire();
        this.outputWire = output.wire();
        this.invertResult = type.invertsResult();
        this.inversionMask = prototype.isMultiBit() ? 0xFFFF : 0x1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short inputOne = context.loadInput(inputOneWire);
        short inputTwo = context.loadInput(inputTwoWire);
        int output = switch (type)
        {
            case AND, NAND -> inputOne & inputTwo;
            case OR, NOR -> inputOne | inputTwo;
            case XOR, XNOR -> inputOne ^ inputTwo;
            default -> throw new UnsupportedOperationException("Invalid logic op: " + type);
        };
        if (invertResult)
        {
            output ^= inversionMask;
        }
        context.storeOutput(outputWire, (short) output);
    }

    @Override
    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        int inputOneLocal = localWires.getLocal(inputOneWire);
        int inputTwoLocal = localWires.getLocal(inputTwoWire);

        int opcode = switch (type)
        {
            case AND, NAND -> GeneratorAdapter.AND;
            case OR, NOR -> GeneratorAdapter.OR;
            case XOR, XNOR -> GeneratorAdapter.XOR;
            default -> throw new UnsupportedOperationException("Invalid logic op: " + type);
        };

        methodGen.loadLocal(inputOneLocal);
        methodGen.loadLocal(inputTwoLocal);
        methodGen.math(opcode, Type.SHORT_TYPE);
        if (invertResult)
        {
            methodGen.push(inversionMask);
            methodGen.math(GeneratorAdapter.XOR, Type.SHORT_TYPE);
        }
        localWires.generateStore(outputWire);
    }
}
