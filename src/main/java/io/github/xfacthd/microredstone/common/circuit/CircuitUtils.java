package io.github.xfacthd.microredstone.common.circuit;

import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;

public final class CircuitUtils
{
    public static IconConfig makeIconConfig(CircuitNode node)
    {
        return switch (node)
        {
            case BundlePackerCircuitNode ignored -> ConverterPrototypeNode.Type.PACK.getIcon();
            case BundleUnpackerCircuitNode ignored -> ConverterPrototypeNode.Type.UNPACK.getIcon();
            case PrimitiveCircuitNode primitive -> PrimitivePrototypeNode.icon(primitive);
            case BufferCircuitNode buffer -> BufferPrototypeNode.icon(buffer);
            case ClockCircuitNode ignored -> ClockPrototypeNode.ICON;
            case CompoundCircuitNode compound -> ReferencePrototypeNode.makeIconConfig(compound);
            default -> throw new IllegalStateException("Unexpected CircuitNode: " + node);
        };
    }

    private CircuitUtils() {}
}
