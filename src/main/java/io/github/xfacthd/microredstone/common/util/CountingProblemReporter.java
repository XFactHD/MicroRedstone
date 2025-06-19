package io.github.xfacthd.microredstone.common.util;

import net.minecraft.util.ProblemReporter;
import org.apache.commons.lang3.mutable.MutableInt;

public final class CountingProblemReporter implements ProblemReporter
{
    private final ProblemReporter reporter;
    private final MutableInt issueCounter;

    public static CountingProblemReporter of(ProblemReporter reporter)
    {
        return new CountingProblemReporter(reporter, new MutableInt());
    }

    private CountingProblemReporter(ProblemReporter reporter, MutableInt issueCounter)
    {
        this.reporter = reporter;
        this.issueCounter = issueCounter;
    }

    @Override
    public ProblemReporter forChild(PathElement pathElement)
    {
        return new CountingProblemReporter(reporter.forChild(pathElement), issueCounter);
    }

    @Override
    public void report(Problem problem)
    {
        reporter.report(problem);
        issueCounter.increment();
    }

    public boolean hasIssues()
    {
        return issueCounter.intValue() > 0;
    }

    public int getIssueCount()
    {
        return issueCounter.intValue();
    }
}
