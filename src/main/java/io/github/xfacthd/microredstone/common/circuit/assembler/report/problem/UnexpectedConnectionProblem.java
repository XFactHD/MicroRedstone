package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.ProblemReporter;

public record UnexpectedConnectionProblem(PrototypeNode node, Port port) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return "PrototypeNode " + node + " has unexpected connection on port " + port;
    }
}
