package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.util.ProblemReporter;

import java.util.Set;

public record UnknownWiresProblem(Set<Wire> wires) implements ProblemReporter.Problem
{
    public UnknownWiresProblem
    {
        wires = Set.copyOf(wires);
    }

    @Override
    public String description()
    {
        return "Unknown wires: " + wires;
    }
}
