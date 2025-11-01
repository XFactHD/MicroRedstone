package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import org.junit.jupiter.api.extension.Extension;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit extension to override the export path of {@link CircuitCompiler} and
 * clear the export directory. Ensures this happens exactly once, regardless
 * of how many test classes run.
 */
public final class DebugExportConfigExtension implements Extension
{
    private static final AtomicBoolean CONFIGURING = new AtomicBoolean();
    private static final AtomicBoolean CONFIGURED = new AtomicBoolean();

    public DebugExportConfigExtension()
    {
        if (CONFIGURED.get()) return;

        if (!CONFIGURING.compareAndSet(false, true))
        {
            while (!CONFIGURED.get())
            {
                Thread.onSpinWait();
            }
            return;
        }

        CircuitCompiler.clearDumpDirectory();
        CONFIGURED.set(true);
    }
}
