package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicInteger;

sealed interface CompilationResult
{
    RootCircuitNode instantiate(CompoundCircuitNode originalNode) throws Throwable;

    boolean isUnreferenced();

    static CompilationResult of(@Nullable MethodHandle compiledNodeConstructor)
    {
        return compiledNodeConstructor != null ? new Success(compiledNodeConstructor) : Failure.INSTANCE;
    }

    final class Success implements CompilationResult
    {
        private final MethodHandle compiledNodeConstructor;
        private final AtomicInteger refCount = new AtomicInteger();
        private final Runnable releaser = () -> refCount.updateAndGet(i -> i > 0 ? i - 1 : i);

        private Success(MethodHandle compiledNodeConstructor)
        {
            this.compiledNodeConstructor = compiledNodeConstructor;
        }

        @Override
        public RootCircuitNode instantiate(CompoundCircuitNode originalNode) throws Throwable
        {
            RootCircuitNode result = (RootCircuitNode) compiledNodeConstructor.invokeExact(originalNode, releaser);
            refCount.incrementAndGet();
            return result;
        }

        @Override
        public boolean isUnreferenced()
        {
            return refCount.get() == 0;
        }
    }

    final class Failure implements CompilationResult
    {
        private static final Failure INSTANCE = new Failure();

        private Failure() {}

        @Override
        public RootCircuitNode instantiate(CompoundCircuitNode originalNode)
        {
            return originalNode;
        }

        @Override
        public boolean isUnreferenced()
        {
            return false;
        }
    }
}
