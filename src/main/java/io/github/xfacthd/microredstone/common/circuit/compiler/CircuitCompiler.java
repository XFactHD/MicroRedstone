package io.github.xfacthd.microredstone.common.circuit.compiler;

import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class CircuitCompiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MethodHandles.Lookup LOOKUP = Util.make(() -> {
        try {
            return MethodHandles.privateLookupIn(CompiledCircuitNode.class, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    });
    private static final AtomicLong CLASS_COUNTER = new AtomicLong();
    private static final String DUMP_ROOT_DIR_PROPERTY = "microredstone.compiler_dump.root_dir";
    private static final String DUMP_SUB_DIR_PROPERTY = "microredstone.compiler_dump.sub_dir";
    @Nullable
    private static final Path EXPORT_PATH = buildExportPath();
    // Disable stack map generation to allow dumping broken code
    private static final boolean DISABLE_STACK_MAP_GEN = false;

    private static final ClassDesc CMP_NODE_TYPE = classDesc(CompoundCircuitNode.class);
    private static final ClassDesc SUPER_TYPE = classDesc(CompiledCircuitNode.class);
    private static final String CLASS_NAME_PREFIX = CompiledCircuitNode.class.getName() + "$";
    private static final MethodType CTOR_MTH_TYPE = MethodType.methodType(void.class, CompoundCircuitNode.class, Runnable.class);
    private static final MethodType CTOR_HANDLE_MTH_TYPE = CTOR_MTH_TYPE.changeReturnType(RootCircuitNode.class);
    private static final ClassDesc RUNNABLE_TYPE = classDesc(Runnable.class);
    private static final MethodTypeDesc CTOR_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, CMP_NODE_TYPE, RUNNABLE_TYPE);
    private static final MethodTypeDesc SUPER_CTOR_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, CMP_NODE_TYPE, RUNNABLE_TYPE);
    private static final ClassDesc INT_ARRAY_TYPE = ConstantDescs.CD_int.arrayType();
    private static final ClassDesc CIRCUIT_STATE_TYPE = classDesc(CircuitState.class);
    private static final MethodTypeDesc CIRCUIT_STATE_CTOR_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, INT_ARRAY_TYPE, INT_ARRAY_TYPE, INT_ARRAY_TYPE);
    private static final MethodTypeDesc STATE_SERIALIZE_MTH = MethodTypeDesc.of(CIRCUIT_STATE_TYPE);
    private static final MethodTypeDesc STATE_DESERIALIZE_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, CIRCUIT_STATE_TYPE);
    private static final MethodTypeDesc STATE_BUFFER_STATES_MTH = MethodTypeDesc.of(INT_ARRAY_TYPE);
    private static final MethodTypeDesc STATE_CLOCK_COUNTERS_MTH = MethodTypeDesc.of(INT_ARRAY_TYPE);
    private static final MethodTypeDesc STATE_CLOCK_STATES_MTH = MethodTypeDesc.of(INT_ARRAY_TYPE);
    static final ClassDesc EVAL_CONTEXT_TYPE = classDesc(EvalContext.class);
    static final ClassDesc WIRE_PAIR_TYPE = classDesc(WirePair.class);
    static final ClassDesc WIRE_PAIR_ARR_TYPE = WIRE_PAIR_TYPE.arrayType();
    static final ClassDesc WIRE_STATES_TYPE = classDesc(WireStates.class);
    static final MethodTypeDesc NODE_EVAL_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, EVAL_CONTEXT_TYPE, WIRE_PAIR_ARR_TYPE, WIRE_PAIR_ARR_TYPE, WIRE_STATES_TYPE);
    static final MethodTypeDesc EVAL_CONTEXT_LOAD_MTH = MethodTypeDesc.of(ConstantDescs.CD_short, ConstantDescs.CD_int);
    static final MethodTypeDesc EVAL_CONTEXT_STORE_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_short);
    static final MethodTypeDesc WIRE_PAIR_EXTERNAL_MTH = MethodTypeDesc.of(ConstantDescs.CD_int);
    static final MethodTypeDesc WIRE_STATES_SET_MTH = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int);

    public static CompletableFuture<RootCircuitNode> tryCompileNode(CompoundCircuitNode node) {
        return tryCompileNode(node, false);
    }

    public static CompletableFuture<RootCircuitNode> tryCompileNode(CompoundCircuitNode node, boolean suppressExport) {
        return CompilationCache.tryCompileNode(node, suppressExport);
    }

    static @Nullable MethodHandle compileNode(CompoundCircuitNode node, boolean suppressExport) {
        try {
            String className = CLASS_NAME_PREFIX + node.getName().replace(" ", "_") + "$" + CLASS_COUNTER.getAndIncrement();

            ClassDesc selfType = ClassDesc.of(className);
            ClassFile classFile = ClassFile.of();
            if (DISABLE_STACK_MAP_GEN) {
                classFile.withOptions(ClassFile.StackMapsOption.DROP_STACK_MAPS);
            }
            byte[] bytes = classFile.build(selfType, clsBuilder -> {
                clsBuilder.withSuperclass(SUPER_TYPE);
                clsBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);

                EvalMethodCompiler evalCompiler = new EvalMethodCompiler(clsBuilder, selfType);
                node.collectFields(evalCompiler.makeFieldCollector());
                compileConstructor(clsBuilder, selfType, evalCompiler.getClockFields());
                node.compile(evalCompiler);
                evalCompiler.executeCompileQueue();

                compileStateSerdes(clsBuilder, selfType, evalCompiler.getBufferFields(), evalCompiler.getClockFields());
            });
            if (EXPORT_PATH != null && !suppressExport) {
                exportClassBytes(className, bytes);
            }
            MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(bytes, true);
            MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), CTOR_MTH_TYPE);
            return constructor.asType(CTOR_HANDLE_MTH_TYPE);
        } catch (Throwable t) {
            LOGGER.error("Failed to compile node {}, falling back to interpreted eval", node, t);
            return null;
        }
    }

    private static void compileConstructor(ClassBuilder clsBuilder, ClassDesc selfType, List<ClockFieldSpec> clockFields) {
        clsBuilder.withMethodBody(ConstantDescs.INIT_NAME, CTOR_MTH, ClassFile.ACC_PUBLIC, ctorBody -> {
            ctorBody.localVariable(1, "originalNode", CMP_NODE_TYPE, ctorBody.startLabel(), ctorBody.endLabel());
            ctorBody.localVariable(2, "releaser", RUNNABLE_TYPE, ctorBody.startLabel(), ctorBody.endLabel());

            ctorBody.aload(0) // this
                    .aload(1) // arg0
                    .aload(2) // arg1
                    .invokespecial(SUPER_TYPE, ConstantDescs.INIT_NAME, SUPER_CTOR_MTH);
            if (!clockFields.isEmpty()) {
                for (ClockFieldSpec field : clockFields) {
                    if (field.counterField != null) {
                        ctorBody.aload(0) // this
                                .loadConstant(field.counterInit)
                                .putfield(selfType, field.counterField, ConstantDescs.CD_int);
                    }
                    ctorBody.aload(0) // this
                            .loadConstant(0)
                            .putfield(selfType, field.stateField, ConstantDescs.CD_int);
                }
            }
            ctorBody.return_();
        });
    }

    private static void compileStateSerdes(ClassBuilder clsBuilder, ClassDesc selfType, List<BufferFieldSpec> bufferFields, List<ClockFieldSpec> clockFields) {
        boolean hasBuffers = !bufferFields.isEmpty();
        boolean hasClocks = !clockFields.isEmpty();
        boolean hasClocksWithCounters = hasClocks && clockFields.stream().anyMatch(clock -> clock.counterField != null);

        clsBuilder.withMethodBody("serializeState", STATE_SERIALIZE_MTH, ClassFile.ACC_PUBLIC, mthBody -> {
            if (!hasBuffers && !hasClocks) {
                mthBody.getstatic(CIRCUIT_STATE_TYPE, "EMPTY", CIRCUIT_STATE_TYPE)
                        .return_(TypeKind.REFERENCE);
                return;
            }

            int bufStateLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
            mthBody.localVariable(bufStateLocal, "bufferStates", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                    .loadConstant(bufferFields.size())
                    .newarray(TypeKind.INT)
                    .storeLocal(TypeKind.REFERENCE, bufStateLocal);
            for (int i = 0; i < bufferFields.size(); i++) {
                BufferFieldSpec buffer = bufferFields.get(i);

                mthBody.aload(bufStateLocal)
                        .loadConstant(i)
                        .aload(0) // this
                        .getfield(selfType, buffer.name, ConstantDescs.CD_short)
                        .arrayStore(TypeKind.INT);
            }

            int clockCountLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
            int clockStateLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
            mthBody.localVariable(clockCountLocal, "clockCounters", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                    .localVariable(clockStateLocal, "clockStates", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                    .loadConstant(clockFields.size())
                    .dup()
                    .newarray(TypeKind.INT)
                    .storeLocal(TypeKind.REFERENCE, clockCountLocal)
                    .newarray(TypeKind.INT)
                    .storeLocal(TypeKind.REFERENCE, clockStateLocal);
            for (int i = 0; i < clockFields.size(); i++) {
                ClockFieldSpec clock = clockFields.get(i);

                if (clock.counterField != null) {
                    mthBody.aload(clockCountLocal)
                            .loadConstant(i)
                            .aload(0) // this
                            .getfield(selfType, clock.counterField, ConstantDescs.CD_int)
                            .arrayStore(TypeKind.INT);
                }

                mthBody.aload(clockStateLocal)
                        .loadConstant(i)
                        .aload(0) // this
                        .getfield(selfType, clock.stateField, ConstantDescs.CD_int)
                        .arrayStore(TypeKind.INT);
            }

            mthBody.new_(CIRCUIT_STATE_TYPE)
                    .dup()
                    .aload(bufStateLocal)
                    .aload(clockCountLocal)
                    .aload(clockStateLocal)
                    .invokespecial(CIRCUIT_STATE_TYPE, ConstantDescs.INIT_NAME, CIRCUIT_STATE_CTOR_MTH)
                    .return_(TypeKind.REFERENCE);
        });
        clsBuilder.withMethodBody("applyState", STATE_DESERIALIZE_MTH, ClassFile.ACC_PUBLIC, mthBody -> {
            mthBody.localVariable(1, "state", CIRCUIT_STATE_TYPE, mthBody.startLabel(), mthBody.endLabel());

            if (!hasBuffers && !hasClocks) {
                mthBody.return_();
                return;
            }

            int bufStateLocal;
            if (hasBuffers) {
                bufStateLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
                mthBody.localVariable(bufStateLocal, "bufferStates", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                        .aload(1) // arg0
                        .invokevirtual(CIRCUIT_STATE_TYPE, "bufferStates", STATE_BUFFER_STATES_MTH)
                        .storeLocal(TypeKind.REFERENCE, bufStateLocal);
            } else {
                bufStateLocal = -1;
            }
            for (int i = 0; i < bufferFields.size(); i++) {
                BufferFieldSpec buffer = bufferFields.get(i);

                mthBody.aload(0) // this
                        .aload(bufStateLocal)
                        .loadConstant(i)
                        .arrayLoad(TypeKind.INT)
                        .putfield(selfType, buffer.name, ConstantDescs.CD_short);
            }

            int clockCountLocal;
            if (hasClocksWithCounters) {
                clockCountLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
                mthBody.localVariable(clockCountLocal, "clockCounters", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                        .aload(1) // arg0
                        .invokevirtual(CIRCUIT_STATE_TYPE, "clockCounters", STATE_CLOCK_COUNTERS_MTH)
                        .storeLocal(TypeKind.REFERENCE, clockCountLocal);
            } else {
                clockCountLocal = -1;
            }
            int clockStateLocal;
            if (hasClocks) {
                clockStateLocal = mthBody.allocateLocal(TypeKind.REFERENCE);
                mthBody.localVariable(clockStateLocal, "clockStates", INT_ARRAY_TYPE, mthBody.startLabel(), mthBody.endLabel())
                        .aload(1) // arg0
                        .invokevirtual(CIRCUIT_STATE_TYPE, "clockStates", STATE_CLOCK_STATES_MTH)
                        .storeLocal(TypeKind.REFERENCE, clockStateLocal);
            } else {
                clockStateLocal = -1;
            }
            for (int i = 0; i < clockFields.size(); i++) {
                ClockFieldSpec clock = clockFields.get(i);

                if (clock.counterField != null) {
                    mthBody.aload(0) // this
                            .aload(clockCountLocal)
                            .loadConstant(i)
                            .arrayLoad(TypeKind.INT)
                            .putfield(selfType, clock.counterField, ConstantDescs.CD_int);
                }

                mthBody.aload(0) // this
                        .aload(clockStateLocal)
                        .loadConstant(i)
                        .arrayLoad(TypeKind.INT)
                        .putfield(selfType, clock.stateField, ConstantDescs.CD_int);
            }

            mthBody.return_();
        });
    }

    record BufferFieldSpec(String name) { }

    record ClockFieldSpec(@Nullable String counterField, String stateField, int counterInit) implements ClockCircuitNode.Fields { }

    private static ClassDesc classDesc(Class<?> clazz) {
        return ClassDesc.of(clazz.getName());
    }

    private static void exportClassBytes(String name, byte[] bytes) {
        Path exportPath = Objects.requireNonNull(EXPORT_PATH);
        String fileName = name.substring(name.lastIndexOf(".") + 1);
        Path path = exportPath.resolve(fileName + ".class");
        try {
            Files.createDirectories(exportPath);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to export class bytes for {}", fileName, e);
        }
    }

    public static void clearDumpDirectory() {
        if (EXPORT_PATH == null || !Files.isDirectory(EXPORT_PATH)) {
            return;
        }

        try (Stream<Path> paths = Files.list(EXPORT_PATH)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(file -> {
                        String fileName = file.getFileName().toString();
                        return fileName.startsWith("CompiledCircuitNode$") && fileName.endsWith(".class");
                    })
                    .toList();
            for (Path file : files) {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to clear export directory", e);
        }
    }

    private static @Nullable Path buildExportPath() {
        String rootDir = System.getProperty(DUMP_ROOT_DIR_PROPERTY);
        String dumpDirName = System.getProperty(DUMP_SUB_DIR_PROPERTY);
        if (rootDir == null || dumpDirName == null) {
            return null;
        }

        return Path.of(rootDir).resolve(MicroRedstone.MOD_ID).resolve("dump").resolve(dumpDirName);
    }

    private CircuitCompiler() { }
}
