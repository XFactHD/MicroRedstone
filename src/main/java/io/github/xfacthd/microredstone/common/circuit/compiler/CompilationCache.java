package io.github.xfacthd.microredstone.common.circuit.compiler;

import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class CompilationCache
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<CompilationKey, CompilationResult> COMPILATION_CACHE = new Object2ObjectOpenHashMap<>();
    private static final long GC_INTERVAL_MS = 30_000;
    private static final Lazy<ScheduledExecutorService> COMPILATION_EXECUTOR = Lazy.of(() ->
    {
        ThreadFactory threadFactory = Thread.ofPlatform().daemon().factory();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        executor.scheduleAtFixedRate(CompilationCache::runCacheGC, 0, GC_INTERVAL_MS, TimeUnit.MILLISECONDS);
        return executor;
    });

    static CompletableFuture<RootCircuitNode> tryCompileNode(CompoundCircuitNode node, @Nullable String name, boolean suppressExport)
    {
        return CompletableFuture.supplyAsync(() -> getOrCompileNode0(node, name, suppressExport), COMPILATION_EXECUTOR.get());
    }

    private static RootCircuitNode getOrCompileNode0(CompoundCircuitNode node, @Nullable String name, boolean suppressExport)
    {
        CompilationKey cacheKey = CompilationKey.of(node);
        CompilationResult result = COMPILATION_CACHE.get(cacheKey);
        if (result == null)
        {
            result = CompilationResult.of(CircuitCompiler.compileNode(node, name, suppressExport));
            COMPILATION_CACHE.put(cacheKey, result);
        }
        try
        {
            return result.instantiate(node);
        }
        catch (Throwable t)
        {
            String logName = name != null ? name : "<unnamed>";
            LOGGER.error("Failed to instantiate compiled node {} (name: {}), falling back to interpreted eval", node, logName, t);
            COMPILATION_CACHE.put(cacheKey, CompilationResult.of(null));
        }
        return node;
    }

    private static void runCacheGC()
    {
        COMPILATION_CACHE.values().removeIf(CompilationResult::isUnreferenced);
    }

    private CompilationCache() {}
}
