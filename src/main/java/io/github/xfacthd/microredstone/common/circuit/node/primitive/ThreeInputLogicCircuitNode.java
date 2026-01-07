package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.classfile.CodeBuilder;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class ThreeInputLogicCircuitNode extends PrimitiveCircuitNode
{
    public static final MapCodec<ThreeInputLogicCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            PrimitivePrototypeNode.Type.CODEC.fieldOf("function").forGetter(node -> node.type),
            Codec.BOOL.fieldOf("multi_bit").forGetter(ThreeInputLogicCircuitNode::isMultiBit),
            Connector.CODEC.listOf(3, 3).fieldOf("inputs").forGetter(node -> List.of(node.inputs)),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, ThreeInputLogicCircuitNode::new));
    public static final StreamCodec<ByteBuf, ThreeInputLogicCircuitNode> STREAM_CODEC = StreamCodec.composite(
            PrimitivePrototypeNode.Type.STREAM_CODEC,
            node -> node.type,
            ByteBufCodecs.BOOL,
            node -> node.inversionMask != 0x1,
            Connector.STREAM_CODEC.apply(ByteBufCodecs.list()),
            node -> List.of(node.getInputs()),
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            ThreeInputLogicCircuitNode::new
    );

    private final PrimitivePrototypeNode.Type type;
    private final int inputOneWire;
    private final int inputTwoWire;
    private final int inputThreeWire;
    private final int outputWire;
    private final boolean invertResult;
    private final int inversionMask;

    public ThreeInputLogicCircuitNode(PrimitivePrototypeNode.Type type, boolean multiBit, List<Connector> inputs, Connector output)
    {
        super(inputs, List.of(output));
        this.type = type;
        this.inputOneWire = inputs.get(0).wire();
        this.inputTwoWire = inputs.get(1).wire();
        this.inputThreeWire = inputs.get(2).wire();
        this.outputWire = output.wire();
        this.invertResult = type.invertsResult();
        this.inversionMask = multiBit ? 0xFFFF : 0x1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short inputOne = context.loadInput(inputOneWire);
        short inputTwo = context.loadInput(inputTwoWire);
        short inputThree = context.loadInput(inputThreeWire);
        int output = switch (type)
        {
            case AND, NAND -> inputOne & inputTwo & inputThree;
            case OR, NOR -> inputOne | inputTwo | inputThree;
            case XOR, XNOR -> inputOne ^ inputTwo ^ inputThree;
            default -> throw new UnsupportedOperationException("Invalid logic op: " + type);
        };
        if (invertResult)
        {
            output ^= inversionMask;
        }
        context.storeOutput(outputWire, (short) output);
    }

    @Override
    public void compile(CodeBuilder mthBody, LocalWireMapper localWires)
    {
        localWires.generateLoad(inputOneWire);
        localWires.generateLoad(inputTwoWire);
        UnaryOperator<CodeBuilder> opcode = switch (type)
        {
            case AND, NAND -> CodeBuilder::iand;
            case OR, NOR -> CodeBuilder::ior;
            case XOR, XNOR -> CodeBuilder::ixor;
            default -> throw new UnsupportedOperationException("Invalid logic op: " + type);
        };
        opcode.apply(mthBody);
        localWires.generateLoad(inputThreeWire);
        opcode.apply(mthBody);
        if (invertResult)
        {
            mthBody.loadConstant(inversionMask)
                    .ixor();
        }
        localWires.generateStore(outputWire);
    }

    public PrimitivePrototypeNode.Type getType()
    {
        return type;
    }

    @Override
    public boolean validate(NodeEntry<?> entry, BitSet wires, int wireCount)
    {
        return true;
    }

    @Override
    public PrototypeNode disassemble()
    {
        return new PrimitivePrototypeNode(type, 3, isMultiBit() ? WireType.BUNDLED : WireType.SINGLE);
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_LOGIC_THREE_INPUT.value();
    }

    private boolean isMultiBit()
    {
        return inversionMask != 0x1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof ThreeInputLogicCircuitNode other)) return false;
        return other.type == type &&
                other.inputOneWire == inputOneWire &&
                other.inputTwoWire == inputTwoWire &&
                other.inputThreeWire == inputThreeWire &&
                other.outputWire == outputWire &&
                other.invertResult == invertResult &&
                other.inversionMask == inversionMask;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, inputOneWire, inputTwoWire, inputThreeWire, outputWire, invertResult, inversionMask);
    }
}
