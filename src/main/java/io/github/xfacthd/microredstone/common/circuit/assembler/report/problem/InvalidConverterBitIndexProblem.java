package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import net.minecraft.util.ProblemReporter;

public record InvalidConverterBitIndexProblem(ConverterPrototypeNode node, int bitIndex) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "ConverterPrototypeNode " + node + " has invalid bit index: " + bitIndex;
    }
}
