package io.github.xfacthd.microredstone.common.data;

import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Consumer;

public final class MRRegistries {
    public static final Registry<CircuitNodeType<?>> CIRCUIT_NODE_TYPES = create(
            ResourceKey.createRegistryKey(Utils.id("circuit_node_types")),
            builder -> builder.sync(true)
    );
    public static final Registry<ProtoNodeType<?>> PROTO_NODE_TYPES = create(
            ResourceKey.createRegistryKey(Utils.id("proto_node_types")),
            _ -> { }
    );

    private static <T> Registry<T> create(ResourceKey<Registry<T>> key, Consumer<RegistryBuilder<T>> consumer) {
        RegistryBuilder<T> builder = new RegistryBuilder<>(key);
        consumer.accept(builder);
        return builder.create();
    }

    public static void onRegisterNewRegistries(final NewRegistryEvent event) {
        event.register(CIRCUIT_NODE_TYPES);
        event.register(PROTO_NODE_TYPES);
    }

    private MRRegistries() { }
}
