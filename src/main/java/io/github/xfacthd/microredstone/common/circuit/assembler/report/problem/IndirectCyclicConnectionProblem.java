package io.github.xfacthd.microredstone.common.circuit.assembler.report.problem;

import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.util.ProblemReporter;

import java.util.Set;

public record IndirectCyclicConnectionProblem(Set<Set<PrototypeNode>> cycles) implements ProblemReporter.Problem
{
    @Override
    public String description()
    {
        StringBuilder builder = new StringBuilder("Indirect cyclic connections:");
        for (Set<PrototypeNode> cycle : cycles)
        {
            builder.append("\n  - ");
            for (PrototypeNode node : cycle)
            {
                builder.append(node).append("->");
            }
            builder.append(cycle.iterator().next());
        }
        return builder.toString();
    }
}
