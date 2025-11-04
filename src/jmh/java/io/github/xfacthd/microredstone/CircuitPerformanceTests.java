package io.github.xfacthd.microredstone;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitAssembler;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.util.ComplexTestCircuits;
import io.github.xfacthd.microredstone.util.TestInterfaceAdapter;
import net.minecraft.Util;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@State(Scope.Benchmark)
@SuppressWarnings("MethodMayBeStatic")
public class CircuitPerformanceTests
{
    private static final CompoundCircuitNode BCD_7SEG_DEC_INTERP = Util.make(() ->
    {
        CompoundPrototypeNode protoNode = ComplexTestCircuits.bcdTo7SegDecoder();
        CompoundCircuitNode assembled = CircuitAssembler.assemble("BCD_7SEG_DEC", protoNode, new CircuitErrorCollector());
        return Objects.requireNonNull(assembled);
    });
    private static final Circuit BCD_7SEG_DEC_INTERP_CIRCUIT = new Circuit(BCD_7SEG_DEC_INTERP);
    private static final TestInterfaceAdapter BCD_7SEG_DEC_INTERP_ADAPTER = new TestInterfaceAdapter();
    private static final RootCircuitNode BCD_7SEG_DEC_COMPILED = Util.make(() ->
    {
        CompletableFuture<RootCircuitNode> future = CircuitCompiler.tryCompileNode(BCD_7SEG_DEC_INTERP, true);
        if (future.join() instanceof CompiledCircuitNode compiled) return compiled;
        throw new IllegalStateException("Test circuit failed to compile");
    });
    private static final Circuit BCD_7SEG_DEC_COMPILED_CIRCUIT = new Circuit(BCD_7SEG_DEC_COMPILED);
    private static final TestInterfaceAdapter BCD_7SEG_DEC_COMPILED_ADAPTER = new TestInterfaceAdapter();

    @Benchmark
    public void runInterpreted(Blackhole bh)
    {
        for (int i = 0; i < 10; i++)
        {
            BCD_7SEG_DEC_INTERP_ADAPTER.setValue(Port.LEFT, i);
            BCD_7SEG_DEC_INTERP_CIRCUIT.evaluate(BCD_7SEG_DEC_INTERP_ADAPTER, null);
            bh.consume(BCD_7SEG_DEC_INTERP_ADAPTER.getValue(Port.RIGHT));
        }
    }

    @Benchmark
    public void runCompiled(Blackhole bh)
    {
        for (int i = 0; i < 10; i++)
        {
            BCD_7SEG_DEC_COMPILED_ADAPTER.setValue(Port.LEFT, i);
            BCD_7SEG_DEC_COMPILED_CIRCUIT.evaluate(BCD_7SEG_DEC_COMPILED_ADAPTER, null);
            bh.consume(BCD_7SEG_DEC_COMPILED_ADAPTER.getValue(Port.RIGHT));
        }
    }
}
