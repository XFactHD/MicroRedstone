package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.client.util.PortOverlays;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;

// TODO: find a way to implement nesting properly
public final class ReferencePrototypeNode extends PrototypeNode
{
    private final CompoundCircuitNode referenced;
    private final Connector[] connectors;

    public static ReferencePrototypeNode create(CompoundCircuitNode referenced)
    {
        return new ReferencePrototypeNode(referenced, Utils.concatArrays(referenced.getInputs(), referenced.getOutputs()));
    }

    private ReferencePrototypeNode(CompoundCircuitNode referenced, Connector[] connectors)
    {
        super(makePortConfig(connectors), makeIconConfig(connectors));
        this.referenced = referenced;
        this.connectors = connectors;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        for (Connector connector : connectors)
        {
            if (!isConnected(connector.port()))
            {
                reporter.report(() -> "Port " + connector.port() + " unspecified");
            }
        }
    }

    @Override
    public CompoundCircuitNode assemble(WireMapper wireMapper)
    {
        return referenced;
    }

    private static PortConfig makePortConfig(Connector[] connectors)
    {
        PortConfig.Builder builder = PortConfig.builder();
        for (Connector connector : connectors)
        {
            builder.addPort(connector.port(), connector.type(), connector.dir());
        }
        return builder.build();
    }

    private static IconConfig makeIconConfig(Connector[] connectors)
    {
        int portMask = 0;
        for (Connector connector : connectors)
        {
            portMask = connector.port().appendMask(portMask, connector.type());
        }
        ResourceLocation portOverlay = PortOverlays.get(portMask);
        return new IconConfig(Utils.rl("part/reference"), portOverlay);
    }
}
