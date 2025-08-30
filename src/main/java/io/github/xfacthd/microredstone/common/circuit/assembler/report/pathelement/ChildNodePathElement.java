package io.github.xfacthd.microredstone.common.circuit.assembler.report.pathelement;

import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.ProblemReporter;

public record ChildNodePathElement(PrototypeNode node) implements ProblemReporter.PathElement
{
    @Override
    public String get()
    {
        return node.toString();
    }
}
