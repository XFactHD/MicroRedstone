package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnspecifiedConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;

public final class ConverterPrototypeNode extends PrototypeNode
{
    private final Type type;
    private int bitIndex = 0;

    public ConverterPrototypeNode(Type type)
    {
        super(type.portConfig, type.icon);
        this.type = type;
    }

    public boolean isPacker()
    {
        return type == Type.PACK;
    }

    public void setBitIndex(int bitIndex)
    {
        this.bitIndex = bitIndex;
    }

    public int getBitIndex()
    {
        return bitIndex;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (!isConnected(Port.LEFT)) reporter.report(UnspecifiedConnectionProblem.input(Port.LEFT));
        if (!isConnected(Port.RIGHT)) reporter.report(UnspecifiedConnectionProblem.output(Port.RIGHT));
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        Connector inputConnector = new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, type.inType);
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector outputConnector = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, type.outType);
        return switch (type)
        {
            case PACK -> new BundlePackerCircuitNode(bitIndex, inputConnector, outputConnector);
            case UNPACK -> new BundleUnpackerCircuitNode(bitIndex, inputConnector, outputConnector);
        };
    }

    public enum Type
    {
        PACK(WireType.SINGLE, WireType.BUNDLED, new IconConfig(Utils.rl("part/packer"), Utils.rl("port/left_single_right_bundled"))),
        UNPACK(WireType.BUNDLED, WireType.SINGLE, new IconConfig(Utils.rl("part/unpacker"), Utils.rl("port/left_bundled_right_single")));

        private final WireType inType;
        private final WireType outType;
        private final PortConfig portConfig;
        private final IconConfig icon;

        Type(WireType inType, WireType outType, IconConfig icon)
        {
            this.inType = inType;
            this.outType = outType;
            this.portConfig = PortConfig.builder()
                    .addPort(Port.LEFT, inType, PortDir.INPUT)
                    .addPort(Port.RIGHT, outType, PortDir.OUTPUT)
                    .build();
            this.icon = icon;
        }

        public IconConfig getIcon()
        {
            return icon;
        }
    }
}
