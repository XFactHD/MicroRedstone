package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import net.minecraft.util.ProblemReporter;

public record DanglingConnectionProblem() implements ProblemReporter.Problem
{
    public static final DanglingConnectionProblem INSTANCE = new DanglingConnectionProblem();

    @Override
    public String description()
    {
        return "Connection unspecified";
    }
}
