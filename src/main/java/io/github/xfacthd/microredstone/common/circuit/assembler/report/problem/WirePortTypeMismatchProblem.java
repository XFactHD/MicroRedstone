package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import net.minecraft.util.ProblemReporter;

public record WirePortTypeMismatchProblem(Wire wire, WireType prevType, WireType type) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Wire " + wire + " has mismatched port types (prev: " + prevType + ", new: " + type + ")";
    }
}
