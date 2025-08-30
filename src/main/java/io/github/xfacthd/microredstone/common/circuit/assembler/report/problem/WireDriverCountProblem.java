package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.util.ProblemReporter;

public record WireDriverCountProblem(Wire wire, int drivers) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Wire " + wire + " has incorrect amount of driving outputs: " + drivers;
    }
}
