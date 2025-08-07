package io.github.xfacthd.microredstone.common.circuit.prototype;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.NotLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ThreeInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.TwoInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class PrimitivePrototypeNode extends PrototypeNode
{
    private static final Port[][] PORT_MAPPING = new Port[][] {
            { Port.LEFT },
            { Port.UP, Port.DOWN },
            { Port.UP, Port.LEFT, Port.DOWN }
    };
    private static final int[][] INV_PORT_MAPPING = Util.make(() ->
    {
        int[][] arr = new int[PORT_MAPPING.length][];
        for (int i = 0; i < PORT_MAPPING.length; i++)
        {
            int[] innerInv = new int[4];
            Arrays.fill(innerInv, -1);
            Port[] inner = PORT_MAPPING[i];
            for (int j = 0; j < inner.length; j++)
            {
                int port = inner[j].ordinal();
                innerInv[port] = j;
            }
            arr[i] = innerInv;
        }
        return arr;
    });

    private final Type type;
    private final int inputCount;
    private final WireType wireType;
    private final @Nullable Wire[] inputs;
    @Nullable
    private Wire output = null;

    public PrimitivePrototypeNode(Type type, int inputCount, WireType wireType)
    {
        super(type.icon(inputCount, wireType));
        type.validateInputCount(inputCount);
        this.type = type;
        this.inputCount = inputCount;
        this.wireType = wireType;
        this.inputs = new Wire[inputCount];
    }

    public Type getType()
    {
        return type;
    }

    public int getInputCount()
    {
        return inputCount;
    }

    public boolean isMultiBit()
    {
        return wireType == WireType.BUNDLED;
    }

    @Override
    protected boolean setConnectionInternal(Port port, Wire wire, boolean connect)
    {
        if (wire.getWireType() != wireType) return false;

        if (port == Port.RIGHT)
        {
            output = connect ? wire : null;
            return true;
        }

        int inputIdx = INV_PORT_MAPPING[inputCount - 1][port.ordinal()];
        if (inputIdx != -1)
        {
            inputs[inputIdx] = connect ? wire : null;
            return true;
        }
        return false;
    }

    @Override
    protected boolean hasPortInternal(Port port, @Nullable WireType wireType)
    {
        return (inputCount == 3 || port != Port.LEFT) && (wireType == null || wireType == this.wireType);
    }

    @Override
    protected boolean isConnectedInternal(Port port)
    {
        if (port == Port.RIGHT)
        {
            return output != null;
        }
        int inputIdx = INV_PORT_MAPPING[inputCount - 1][port.ordinal()];
        return inputIdx != -1 && inputs[inputIdx] != null;
    }

    @Override
    public void replaceWire(Wire oldWire, Wire newWire)
    {
        for (int i = 0; i < inputs.length; i++)
        {
            if (inputs[i] == oldWire)
            {
                inputs[i] = newWire;
                return;
            }
        }
        if (output == oldWire)
        {
            output = newWire;
        }
    }

    @Override
    public void clearWires()
    {
        Arrays.fill(inputs, null);
        output = null;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        for (int i = 0; i < inputCount; i++)
        {
            if (inputs[i] == null)
            {
                int input = i;
                reporter.report(() -> "Input " + input + " unspecified");
            }
            if (inputs[i] == output) reporter.report(() -> "Connected to self");
        }
        if (output == null) reporter.report(() -> "Output unspecified");
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        List<Connector> inputConnectors = new ArrayList<>();
        for (int i = 0; i < inputs.length; i++)
        {
            int wire = wireMapper.resolveWire(Objects.requireNonNull(inputs[i]));
            Port port = PORT_MAPPING[inputCount - 1][i];
            inputConnectors.add(new Connector(getPos(), port, wire, PortDir.INPUT, wireType));
        }
        int outWire = wireMapper.resolveWire(Objects.requireNonNull(output));
        Connector outputConnector = new Connector(getPos(), Port.RIGHT, outWire, PortDir.OUTPUT, wireType);
        return switch (inputCount)
        {
            case 1 ->
            {
                if (type != Type.NOT)
                {
                    throw new IllegalStateException("Invalid single-input logic op: " + type);
                }
                yield new NotLogicCircuitNode(isMultiBit(), inputConnectors.getFirst(), outputConnector);
            }
            case 2 -> new TwoInputLogicCircuitNode(type, isMultiBit(), inputConnectors, outputConnector);
            case 3 -> new ThreeInputLogicCircuitNode(type, isMultiBit(), inputConnectors, outputConnector);
            default -> throw new IllegalStateException("Invalid input count: " + inputCount);
        };
    }

    @Override
    public Set<Wire> getConnectedInputWires()
    {
        return Set.of(inputs);
    }

    @Override
    public Set<Wire> getConnectedOutputWires()
    {
        return Set.of(Objects.requireNonNull(output));
    }

    @Override
    public WireType getOutputType(Wire wire)
    {
        return wireType;
    }

    public enum Type implements StringRepresentable
    {
        NOT(false, false, Utils.rl("part/not")),
        AND(true, false, Utils.rl("part/and")),
        OR(true, false, Utils.rl("part/or")),
        XOR(true, false, Utils.rl("part/xor")),
        NAND(true, true, Utils.rl("part/nand")),
        NOR(true, true, Utils.rl("part/nor")),
        XNOR(true, true, Utils.rl("part/xnor")),
        ;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::ordinal, Type.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::ordinal);

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final boolean multiInput;
        private final boolean invertsResult;
        private final IntFunction<IconConfig> iconSingle;
        private final IntFunction<IconConfig> iconBundled;

        Type(boolean multiInput, boolean invertsResult, ResourceLocation icon)
        {
            this.multiInput = multiInput;
            this.invertsResult = invertsResult;
            if (multiInput)
            {
                IconConfig iconSingleTwo = new IconConfig(icon, Utils.rl("port/up_down_right_single"));
                IconConfig iconBundledTwo = new IconConfig(icon, Utils.rl("port/up_down_right_bundled"));
                IconConfig iconSingleThree = new IconConfig(icon, Utils.rl("port/full_single"));
                IconConfig iconBundledThree = new IconConfig(icon, Utils.rl("port/full_bundled"));
                this.iconSingle = inputs -> inputs == 2 ? iconSingleTwo : iconSingleThree;
                this.iconBundled = inputs -> inputs == 2 ? iconBundledTwo : iconBundledThree;
            }
            else
            {
                IconConfig iconSingle = new IconConfig(icon, Utils.rl("port/hor_single"));
                IconConfig iconBundled = new IconConfig(icon, Utils.rl("port/hor_bundled"));
                this.iconSingle = inputs -> iconSingle;
                this.iconBundled = inputs -> iconBundled;
            }
        }

        public boolean invertsResult()
        {
            return invertsResult;
        }

        public IconConfig icon(int inputCount, WireType wireType)
        {
            validateInputCount(inputCount);
            return wireType.select(iconSingle, iconBundled).apply(inputCount);
        }

        public Supplier<PrototypeNode> factory(int inputCount, WireType wireType)
        {
            validateInputCount(inputCount);
            return () -> new PrimitivePrototypeNode(this, inputCount, wireType);
        }

        private void validateInputCount(int inputCount)
        {
            if ((inputCount > 1) != multiInput)
            {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid input count %d for type %s", inputCount, this));
            }
            if (inputCount < 1 || inputCount > 3)
            {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid input count %d, expected 1 <= count <= 3", inputCount));
            }
        }

        @Override
        public String getSerializedName()
        {
            return name;
        }
    }
}
