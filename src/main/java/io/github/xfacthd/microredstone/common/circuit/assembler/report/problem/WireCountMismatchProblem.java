package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import net.minecraft.util.ProblemReporter;

public record WireCountMismatchProblem(int knownCount, int mappedCount) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Wire count mismatch (known: " + knownCount + ", mapped: " + mappedCount + ")";
    }
}
