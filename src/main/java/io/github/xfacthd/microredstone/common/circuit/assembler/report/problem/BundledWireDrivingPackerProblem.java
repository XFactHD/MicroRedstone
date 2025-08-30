package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.util.ProblemReporter;

public record BundledWireDrivingPackerProblem(Wire wire, int packerBit) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "Wire " + wire + " has multiple packers driving bit " + packerBit;
    }
}
