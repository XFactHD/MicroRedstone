package io.github.xfacthd.microredstone.common.circuit.compiler;

import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import net.minecraft.util.Util;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class CircuitCompiler
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MethodHandles.Lookup LOOKUP = Util.make(() ->
    {
        try
        {
            return MethodHandles.privateLookupIn(CompiledCircuitNode.class, MethodHandles.lookup());
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    });
    private static final AtomicLong CLASS_COUNTER = new AtomicLong();
    private static final String DUMP_ROOT_DIR_PROPERTY = "microredstone.compiler_dump.root_dir";
    private static final String DUMP_SUB_DIR_PROPERTY = "microredstone.compiler_dump.sub_dir";
    @Nullable
    private static final Path EXPORT_PATH = buildExportPath();

    private static final String SUPER_CLASS = CompiledCircuitNode.class.getName().replace(".", "/");
    private static final Type SUPER_TYPE = Type.getType(CompiledCircuitNode.class);
    private static final String CLASS_NAME_PREFIX = SUPER_CLASS + "$";
    private static final MethodType CTOR_MTH_TYPE = MethodType.methodType(void.class, CompoundCircuitNode.class, Runnable.class);
    private static final MethodType CTOR_HANDLE_MTH_TYPE = CTOR_MTH_TYPE.changeReturnType(RootCircuitNode.class);
    private static final Method CTOR_MTH = method("<init>", Type.VOID_TYPE, CompoundCircuitNode.class, Runnable.class);
    private static final Method SUPER_CTOR_MTH = method("<init>", Type.VOID_TYPE, CompoundCircuitNode.class, Runnable.class);
    private static final Type INT_ARRAY_TYPE = Type.getType(int[].class);
    private static final Type CIRCUIT_STATE_TYPE = Type.getType(CircuitState.class);
    private static final Method CIRCUIT_STATE_CTOR_MTH = method("<init>", Type.VOID_TYPE, int[].class, int[].class, int[].class);
    private static final Method STATE_SERIALIZE_MTH = method("serializeState", CIRCUIT_STATE_TYPE);
    private static final Method STATE_DESERIALIZE_MTH = method("applyState", Type.VOID_TYPE, CircuitState.class);
    private static final Method STATE_BUFFER_STATES_MTH = findMethod(CircuitState.class, "bufferStates");
    private static final Method STATE_CLOCK_COUNTERS_MTH = findMethod(CircuitState.class, "clockCounters");
    private static final Method STATE_CLOCK_STATES_MTH = findMethod(CircuitState.class, "clockStates");
    static final Method NODE_EVAL_MTH = findMethod(RootCircuitNode.class, "evaluate", EvalContext.class, WirePair[].class, WirePair[].class, WireStates.class);
    static final Type EVAL_CONTEXT_TYPE = Type.getType(EvalContext.class);
    static final Method EVAL_CONTEXT_LOAD_MTH = findMethod(EvalContext.class, "loadInput", int.class);
    static final Method EVAL_CONTEXT_STORE_MTH = findMethod(EvalContext.class, "storeOutput", int.class, short.class);
    static final Type WIRE_PAIR_TYPE = Type.getType(WirePair.class);
    static final Method WIRE_PAIR_EXTERNAL_MTH = findMethod(WirePair.class, "external");
    static final Type WIRE_STATES_TYPE = Type.getType(WireStates.class);
    static final Method WIRE_STATES_SET_MTH = findMethod(WireStates.class, "set", int.class, int.class);

    public static CompletableFuture<RootCircuitNode> tryCompileNode(CompoundCircuitNode node)
    {
        return tryCompileNode(node, false);
    }

    public static CompletableFuture<RootCircuitNode> tryCompileNode(CompoundCircuitNode node, boolean suppressExport)
    {
        return CompilationCache.tryCompileNode(node, suppressExport);
    }

    @Nullable
    static MethodHandle compileNode(CompoundCircuitNode node, boolean suppressExport)
    {
        try
        {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            String className = CLASS_NAME_PREFIX + node.getName().replaceAll(" ", "_") + "$" + CLASS_COUNTER.getAndIncrement();
            Type selfType = Type.getType("L" + className + ";");
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null, SUPER_CLASS, null);

            GeneratorAdapter ctorGen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, CTOR_MTH, null, null, writer);

            EvalMethodCompiler evalCompiler = new EvalMethodCompiler(writer, selfType);
            node.compile(evalCompiler);

            compileConstructor(ctorGen, selfType, evalCompiler.getClockFields());
            compileStateSerdes(writer, selfType, evalCompiler.getBufferFields(), evalCompiler.getClockFields());

            byte[] bytes = writer.toByteArray();
            if (EXPORT_PATH != null && !suppressExport)
            {
                exportClassBytes(className, bytes);
            }
            MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(bytes, true);
            MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), CTOR_MTH_TYPE);
            return constructor.asType(CTOR_HANDLE_MTH_TYPE);
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to compile node {}, falling back to interpreted eval", node, t);
            return null;
        }
    }

    private static void compileConstructor(GeneratorAdapter ctorGen, Type selfType, List<ClockFieldSpec> clockFields)
    {
        ctorGen.loadThis();
        ctorGen.loadArg(0);
        ctorGen.loadArg(1);
        ctorGen.invokeConstructor(SUPER_TYPE, SUPER_CTOR_MTH);
        if (!clockFields.isEmpty())
        {
            for (ClockFieldSpec field : clockFields)
            {
                if (field.counterName != null)
                {
                    ctorGen.loadThis();
                    ctorGen.push(field.counterInit);
                    ctorGen.putField(selfType, field.counterName, Type.INT_TYPE);
                }
                ctorGen.loadThis();
                ctorGen.push(0);
                ctorGen.putField(selfType, field.stateName, Type.INT_TYPE);
            }
        }
        ctorGen.returnValue();
        ctorGen.endMethod();
    }

    private static void compileStateSerdes(ClassWriter writer, Type selfType, List<BufferFieldSpec> bufferFields, List<ClockFieldSpec> clockFields)
    {
        GeneratorAdapter serGen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, STATE_SERIALIZE_MTH, null, null, writer);
        GeneratorAdapter desGen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, STATE_DESERIALIZE_MTH, null, null, writer);

        boolean hasBuffers = !bufferFields.isEmpty();
        boolean hasClocks = !clockFields.isEmpty();
        boolean hasClocksWithCounters = hasClocks && clockFields.stream().anyMatch(clock -> clock.counterName != null);

        if (hasBuffers || hasClocks)
        {
            serGen.newInstance(CIRCUIT_STATE_TYPE);
            serGen.dup();

            int serBufStateLocal = serGen.newLocal(INT_ARRAY_TYPE);
            int desBufStateLocal = hasBuffers ? desGen.newLocal(INT_ARRAY_TYPE) : -1;
            serGen.push(bufferFields.size());
            serGen.newArray(Type.INT_TYPE);
            serGen.storeLocal(serBufStateLocal);
            if (hasBuffers)
            {
                desGen.loadArg(0);
                desGen.invokeVirtual(CIRCUIT_STATE_TYPE, STATE_BUFFER_STATES_MTH);
                desGen.storeLocal(desBufStateLocal);
            }
            for (int i = 0; i < bufferFields.size(); i++)
            {
                BufferFieldSpec buffer = bufferFields.get(i);

                serGen.loadLocal(serBufStateLocal);
                serGen.push(i);
                serGen.loadThis();
                serGen.getField(selfType, buffer.name, Type.SHORT_TYPE);
                serGen.arrayStore(Type.INT_TYPE);

                desGen.loadThis();
                desGen.loadLocal(desBufStateLocal);
                desGen.push(i);
                desGen.arrayLoad(Type.INT_TYPE);
                desGen.putField(selfType, buffer.name, Type.SHORT_TYPE);
            }

            int serClockCountLocal = serGen.newLocal(INT_ARRAY_TYPE);
            int desClockCountLocal = hasClocksWithCounters ? desGen.newLocal(INT_ARRAY_TYPE) : -1;
            int serClockStateLocal = serGen.newLocal(INT_ARRAY_TYPE);
            int desClockStateLocal = hasClocks ? desGen.newLocal(INT_ARRAY_TYPE) : -1;
            serGen.push(clockFields.size());
            serGen.dup();
            serGen.newArray(Type.INT_TYPE);
            serGen.storeLocal(serClockCountLocal);
            serGen.newArray(Type.INT_TYPE);
            serGen.storeLocal(serClockStateLocal);
            if (hasClocksWithCounters)
            {
                desGen.loadArg(0);
                desGen.invokeVirtual(CIRCUIT_STATE_TYPE, STATE_CLOCK_COUNTERS_MTH);
                desGen.storeLocal(desClockCountLocal);
            }
            if (hasClocks)
            {
                desGen.loadArg(0);
                desGen.invokeVirtual(CIRCUIT_STATE_TYPE, STATE_CLOCK_STATES_MTH);
                desGen.storeLocal(desClockStateLocal);
            }
            for (int i = 0; i < clockFields.size(); i++)
            {
                ClockFieldSpec clock = clockFields.get(i);

                if (clock.counterName != null)
                {
                    serGen.loadLocal(serClockCountLocal);
                    serGen.push(i);
                    serGen.loadThis();
                    serGen.getField(selfType, clock.counterName, Type.INT_TYPE);
                    serGen.arrayStore(Type.INT_TYPE);

                    desGen.loadThis();
                    desGen.loadLocal(desClockCountLocal);
                    desGen.push(i);
                    desGen.arrayLoad(Type.INT_TYPE);
                    desGen.putField(selfType, clock.counterName, Type.INT_TYPE);
                }

                serGen.loadLocal(serClockStateLocal);
                serGen.push(i);
                serGen.loadThis();
                serGen.getField(selfType, clock.stateName, Type.INT_TYPE);
                serGen.arrayStore(Type.INT_TYPE);

                desGen.loadThis();
                desGen.loadLocal(desClockStateLocal);
                desGen.push(i);
                desGen.arrayLoad(Type.INT_TYPE);
                desGen.putField(selfType, clock.stateName, Type.INT_TYPE);
            }

            serGen.loadLocal(serBufStateLocal);
            serGen.loadLocal(serClockCountLocal);
            serGen.loadLocal(serClockStateLocal);
            serGen.invokeConstructor(CIRCUIT_STATE_TYPE, CIRCUIT_STATE_CTOR_MTH);
        }
        else
        {
            serGen.getStatic(CIRCUIT_STATE_TYPE, "EMPTY", CIRCUIT_STATE_TYPE);
        }

        serGen.returnValue();
        serGen.endMethod();
        desGen.returnValue();
        desGen.endMethod();
    }

    record BufferFieldSpec(String name) {}

    record ClockFieldSpec(@Nullable String counterName, String stateName, int counterInit) {}

    private static Method findMethod(Class<?> owner, String methodName, Class<?>... parameterTypes)
    {
        // ObfuscationReflectionHelper.findMethod() must be duplicated due to ORH's use of LogManager.getLogger() failing with JMH
        java.lang.reflect.Method method;
        try
        {
            method = owner.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
        }
        catch (Exception e)
        {
            throw new ObfuscationReflectionHelper.UnableToFindMethodException(e);
        }
        return Method.getMethod(method);
    }

    @SuppressWarnings("SameParameterValue")
    static Method method(String name, Type retType, Class<?>... paramTypes)
    {
        return new Method(name, retType, Arrays.stream(paramTypes).map(Type::getType).toArray(Type[]::new));
    }

    private static void exportClassBytes(String name, byte[] bytes)
    {
        Path exportPath = Objects.requireNonNull(EXPORT_PATH);
        String fileName = name.substring(name.lastIndexOf("/") + 1);
        Path path = exportPath.resolve(fileName + ".class");
        try
        {
            Files.createDirectories(exportPath);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to export class bytes for {}", fileName, e);
        }
    }

    public static void clearDumpDirectory()
    {
        if (EXPORT_PATH == null || !Files.isDirectory(EXPORT_PATH)) { return; }

        try (Stream<Path> paths = Files.list(EXPORT_PATH))
        {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(file ->
                    {
                        String fileName = file.getFileName().toString();
                        return fileName.startsWith("CompiledCircuitNode$") && fileName.endsWith(".class");
                    })
                    .toList();
            for (Path file : files)
            {
                Files.delete(file);
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to clear export directory", e);
        }
    }

    @Nullable
    private static Path buildExportPath()
    {
        String rootDir = System.getProperty(DUMP_ROOT_DIR_PROPERTY);
        String dumpDirName = System.getProperty(DUMP_SUB_DIR_PROPERTY);
        if (rootDir == null || dumpDirName == null) return null;

        return Path.of(rootDir).resolve(MicroRedstone.MOD_ID).resolve("dump").resolve(dumpDirName);
    }

    private CircuitCompiler() { }
}
