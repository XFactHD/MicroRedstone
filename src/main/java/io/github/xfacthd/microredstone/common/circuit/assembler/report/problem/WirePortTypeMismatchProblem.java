package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.ProblemReporter;

public record WirePortTypeMismatchProblem(PrototypeNode node, Port port, WireType portType, WireType wireType) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "PrototypeNode " + node + " has wire with incorrect type " + wireType + " connected on port " + port + " , expected type " + portType;
    }
}
