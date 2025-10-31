package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import net.minecraft.util.ProblemReporter;

public record InvalidConstantValueProblem(ConstantPrototypeNode node, int value) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "ConstantPrototypeNode " + node + " has invalid value: " + value;
    }
}
