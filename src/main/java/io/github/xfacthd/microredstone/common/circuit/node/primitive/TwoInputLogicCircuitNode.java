package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.Objects;

public final class TwoInputLogicCircuitNode extends PrimitiveCircuitNode
{
    public static final MapCodec<TwoInputLogicCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            PrimitivePrototypeNode.Type.CODEC.fieldOf("function").forGetter(node -> node.type),
            Codec.BOOL.fieldOf("multi_bit").forGetter(TwoInputLogicCircuitNode::isMultiBit),
            Connector.CODEC.listOf(2, 2).fieldOf("inputs").forGetter(node -> List.of(node.inputs)),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, TwoInputLogicCircuitNode::new));
    public static final StreamCodec<ByteBuf, TwoInputLogicCircuitNode> STREAM_CODEC = StreamCodec.composite(
            PrimitivePrototypeNode.Type.STREAM_CODEC,
            node -> node.type,
            ByteBufCodecs.BOOL,
            node -> node.inversionMask != 0x1,
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.getInputs()),
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            TwoInputLogicCircuitNode::new
    );

    private final PrimitivePrototypeNode.Type type;
    private final int inputOneWire;
    private final int inputTwoWire;
    private final int outputWire;
    private final boolean invertResult;
    private final int inversionMask;

    public TwoInputLogicCircuitNode(PrimitivePrototypeNode.Type type, boolean multiBit, List<Connector> inputs, Connector output)
    {
        super(inputs, List.of(output));
        this.type = type;
        this.inputOneWire = inputs.get(0).wire();
        this.inputTwoWire = inputs.get(1).wire();
        this.outputWire = output.wire();
        this.invertResult = type.invertsResult();
        this.inversionMask = multiBit ? 0xFFFF : 0x1;
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
        int opcode = switch (type)
        {
            case AND, NAND -> GeneratorAdapter.AND;
            case OR, NOR -> GeneratorAdapter.OR;
            case XOR, XNOR -> GeneratorAdapter.XOR;
            default -> throw new UnsupportedOperationException("Invalid logic op: " + type);
        };

        localWires.generateLoad(inputOneWire);
        localWires.generateLoad(inputTwoWire);
        methodGen.math(opcode, Type.SHORT_TYPE);
        if (invertResult)
        {
            methodGen.push(inversionMask);
            methodGen.math(GeneratorAdapter.XOR, Type.SHORT_TYPE);
        }
        localWires.generateStore(outputWire);
    }

    public PrimitivePrototypeNode.Type getType()
    {
        return type;
    }

    @Override
    public PrototypeNode disassemble()
    {
        return new PrimitivePrototypeNode(type, 2, isMultiBit() ? WireType.BUNDLED : WireType.SINGLE);
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_LOGIC_TWO_INPUT.value();
    }

    private boolean isMultiBit()
    {
        return inversionMask != 0x1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof TwoInputLogicCircuitNode other)) return false;
        return other.type == type &&
                other.inputOneWire == inputOneWire &&
                other.inputTwoWire == inputTwoWire &&
                other.outputWire == outputWire &&
                other.invertResult == invertResult &&
                other.inversionMask == inversionMask;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, inputOneWire, inputTwoWire, outputWire, invertResult, inversionMask);
    }
}
