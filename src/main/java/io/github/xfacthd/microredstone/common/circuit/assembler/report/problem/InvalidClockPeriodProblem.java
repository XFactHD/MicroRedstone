package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import net.minecraft.util.ProblemReporter;

public record InvalidClockPeriodProblem(ClockPrototypeNode node, int halfPeriod) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "ClockPrototypeNode " + node + " has invalid half-period: " + halfPeriod;
    }
}
