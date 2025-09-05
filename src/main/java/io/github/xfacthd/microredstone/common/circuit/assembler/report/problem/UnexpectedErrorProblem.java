package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import net.minecraft.util.ProblemReporter;

public record UnexpectedErrorProblem(Throwable error) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Unexpected error: " + error.getMessage();
    }
}
