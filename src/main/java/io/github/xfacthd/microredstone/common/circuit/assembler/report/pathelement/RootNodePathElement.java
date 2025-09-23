package io.github.xfacthd.microredstone.common.circuit.assembler.report.pathelement;

import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import net.minecraft.util.ProblemReporter;

public record RootNodePathElement(CompoundPrototypeNode node) implements ProblemReporter.PathElement
{
    @Override
    public String get()
    {
        return "root_node";
    }
}
