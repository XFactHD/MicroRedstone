package io.github.xfacthd.microredstone.common.util.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public final class DeferredDataComponentTypeRegister extends DeferredRegister.DataComponents
{
    private DeferredDataComponentTypeRegister(String namespace)
    {
        super(Registries.DATA_COMPONENT_TYPE, namespace);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <I extends DataComponentType<?>> DeferredHolder<DataComponentType<?>, I> createHolder(
            ResourceKey<? extends Registry<DataComponentType<?>>> registryKey, ResourceLocation key
    )
    {
        return (DeferredHolder<DataComponentType<?>, I>) DeferredDataComponentType.createDataComponent(ResourceKey.create(registryKey, key));
    }

    @Override
    public <D> DeferredDataComponentType<D> registerComponentType(String name, UnaryOperator<DataComponentType.Builder<D>> builder)
    {
        return (DeferredDataComponentType<D>) super.registerComponentType(name, builder);
    }

    public static DeferredDataComponentTypeRegister create(String namespace)
    {
        return new DeferredDataComponentTypeRegister(namespace);
    }
}
