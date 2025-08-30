package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import net.minecraft.util.ProblemReporter;

public record UnspecifiedConnectionProblem(Port port, PortDir dir) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        return dir.getDescription() + " unspecified on port " + port;
    }

    public static UnspecifiedConnectionProblem input(Port port)
    {
        return new UnspecifiedConnectionProblem(port, PortDir.INPUT);
    }

    public static UnspecifiedConnectionProblem output(Port port)
    {
        return new UnspecifiedConnectionProblem(port, PortDir.OUTPUT);
    }
}
