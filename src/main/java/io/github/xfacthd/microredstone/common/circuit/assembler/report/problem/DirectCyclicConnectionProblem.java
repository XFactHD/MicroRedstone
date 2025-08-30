package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.ProblemReporter;

public record DirectCyclicConnectionProblem(PrototypeNode node) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Immediate cyclic connection on " + node;
    }
}
