package io.github.xfacthd.microredstone.common.circuit.compiler;

import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private static final boolean DUMP_TO_FILE = true;
    @Nullable
    @VisibleForTesting
    public static Path EXPORT_PATH_OVERRIDE = null;
    private static final Lazy<Path> EXPORT_PATH = Lazy.of(() -> FMLPaths.GAMEDIR.get().resolve(MicroRedstone.MOD_ID));

    private static final String SUPER_CLASS = CompiledCircuitNode.class.getName().replace(".", "/");
    public static final Type SUPER_TYPE = Type.getType(CompiledCircuitNode.class);
    private static final String CLASS_NAME_PREFIX = SUPER_CLASS + "$";
    private static final MethodType CTOR_MTH_TYPE = MethodType.methodType(void.class, CompoundCircuitNode.class);
    private static final MethodType CTOR_HANDLE_MTH_TYPE = CTOR_MTH_TYPE.changeReturnType(RootCircuitNode.class);
    private static final Method CTOR_MTH = method("<init>", Type.VOID_TYPE, CompoundCircuitNode.class);
    private static final Method SUPER_CTOR_MTH = method("<init>", Type.VOID_TYPE, CompoundCircuitNode.class, int.class, Connector[].class, Connector[].class);
    private static final Type LIST_TYPE = Type.getType(List.class);
    private static final Method LIST_GET_MTH = findMethod(List.class, "get", int.class);
    private static final Type CMP_NODE_TYPE = Type.getType(CompoundCircuitNode.class);
    private static final Method CMP_NODE_INPUTS_MTH = findMethod(CircuitNode.class, "getInputs");
    private static final Method CMP_NODE_OUTPUTS_MTH = findMethod(CircuitNode.class, "getOutputs");
    private static final Method CMP_NODE_WIRE_COUNT_MTH = findMethod(CompoundCircuitNode.class, "getWireCount");
    private static final Method CMP_NODE_CHILDREN_MTH = findMethod(CompoundCircuitNode.class, "getChildNodes");
    public static final Method NODE_EVAL_MTH = findMethod(CircuitNode.class, "evaluate", EvalContext.class, WirePair[].class, WirePair[].class);
    public static final Type NODE_ENTRY_TYPE = Type.getType(NodeEntry.class);
    public static final Method NODE_ENTRY_EVAL_MTH = findMethod(NodeEntry.class, "evaluate", EvalContext.class);
    public static final Type EVAL_CONTEXT_TYPE = Type.getType(EvalContext.class);
    public static final Type NESTED_EVAL_CONTEXT_TYPE = Type.getType(EvalContext.Nested.class);
    public static final Method EVAL_CONTEXT_LOAD_MTH = findMethod(EvalContext.class, "loadInput", int.class);
    public static final Method EVAL_CONTEXT_STORE_MTH = findMethod(EvalContext.class, "storeOutput", int.class, short.class);
    public static final Method NESTED_EVAL_CONTEXT_PREPARE_MTH = findMethod(EvalContext.Nested.class, "prepare", EvalContext.class, WirePair[].class);
    public static final Method NESTED_EVAL_CONTEXT_FLUSH_MTH = findMethod(EvalContext.Nested.class, "flush", EvalContext.class, WirePair[].class);

    private static final Map<CompilationKey, Optional<MethodHandle>> COMPILATION_CACHE = new Object2ObjectOpenHashMap<>();

    @Nullable
    public static RootCircuitNode getOrCompileNode(CompoundCircuitNode node, @Nullable String name)
    {
        return getOrCompileNode(node, name, false);
    }

    @Nullable
    public static RootCircuitNode getOrCompileNode(CompoundCircuitNode node, @Nullable String name, boolean suppressExport)
    {
        CompilationKey cacheKey = new CompilationKey(node);
        Optional<MethodHandle> nodeConstructor = COMPILATION_CACHE.get(cacheKey);
        if (nodeConstructor == null)
        {
            nodeConstructor = Optional.ofNullable(compileNode(node, name, suppressExport));
            COMPILATION_CACHE.put(cacheKey, nodeConstructor);
        }
        try
        {
            if (nodeConstructor.isPresent())
            {
                return (RootCircuitNode) nodeConstructor.get().invokeExact(node);
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to instantiate compiled node {}, falling back to interpreted eval", node, t);
            COMPILATION_CACHE.put(cacheKey, Optional.empty());
        }
        return null;
    }

    @Nullable
    private static MethodHandle compileNode(CompoundCircuitNode node, @Nullable String name, boolean suppressExport)
    {
        try
        {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            String className = CLASS_NAME_PREFIX;
            if (name != null)
            {
                className += name + "$";
            }
            className += CLASS_COUNTER.getAndIncrement();
            Type selfType = Type.getType("L" + className + ";");
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, className, null, SUPER_CLASS, null);

            GeneratorAdapter ctorGen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, CTOR_MTH, null, null, writer);

            EvalMethodCompiler evalCompiler = new EvalMethodCompiler(writer, selfType);
            node.compile(evalCompiler);

            compileConstructor(ctorGen, selfType, evalCompiler.getNodeFields(), evalCompiler.getClockFields());

            byte[] bytes = writer.toByteArray();
            if (!suppressExport)
            {
                exportClassBytes(className, bytes);
            }
            MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.STRONG);
            MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), CTOR_MTH_TYPE);
            return constructor.asType(CTOR_HANDLE_MTH_TYPE);
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to compile node {}, falling back to interpreted eval", node, t);
            return null;
        }
    }

    private static void compileConstructor(GeneratorAdapter ctorGen, Type selfType, List<NodeFieldSpec> nodeFields, List<ClockFieldSpec> clockFields)
    {
        ctorGen.loadThis();
        ctorGen.loadArg(0);
        ctorGen.dup();
        ctorGen.invokeVirtual(CMP_NODE_TYPE, CMP_NODE_WIRE_COUNT_MTH);
        ctorGen.loadArg(0);
        ctorGen.invokeVirtual(CMP_NODE_TYPE, CMP_NODE_INPUTS_MTH);
        ctorGen.loadArg(0);
        ctorGen.invokeVirtual(CMP_NODE_TYPE, CMP_NODE_OUTPUTS_MTH);
        ctorGen.invokeConstructor(SUPER_TYPE, SUPER_CTOR_MTH);
        if (!nodeFields.isEmpty())
        {
            int listLocal = ctorGen.newLocal(LIST_TYPE);
            ctorGen.loadArg(0);
            ctorGen.invokeVirtual(CMP_NODE_TYPE, CMP_NODE_CHILDREN_MTH);
            ctorGen.storeLocal(listLocal);
            for (NodeFieldSpec field : nodeFields)
            {
                ctorGen.loadThis();
                ctorGen.loadLocal(listLocal);
                ctorGen.push(field.nodeIdx);
                ctorGen.invokeInterface(LIST_TYPE, LIST_GET_MTH);
                ctorGen.checkCast(NODE_ENTRY_TYPE);
                ctorGen.putField(selfType, field.name, NODE_ENTRY_TYPE);
            }
        }
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

    record NodeFieldSpec(String name, int nodeIdx) {}

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
        if (!DUMP_TO_FILE || FMLEnvironment.production) return;

        Path exportPath = Objects.requireNonNullElseGet(EXPORT_PATH_OVERRIDE, EXPORT_PATH);
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
        Path exportPath = Objects.requireNonNullElseGet(EXPORT_PATH_OVERRIDE, EXPORT_PATH);
        if (!Files.isDirectory(exportPath)) return;

        try (Stream<Path> paths = Files.list(exportPath))
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

    private CircuitCompiler() { }
}
