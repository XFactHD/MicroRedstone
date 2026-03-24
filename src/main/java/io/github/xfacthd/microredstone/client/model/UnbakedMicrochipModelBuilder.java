package io.github.xfacthd.microredstone.client.model;

import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

public final class UnbakedMicrochipModelBuilder extends CustomBlockStateModelBuilder
{
    private final Variant variant;

    public UnbakedMicrochipModelBuilder(Identifier baseModel, Variant.SimpleModelState modelState)
    {
        this(new Variant(baseModel, modelState));
    }

    private UnbakedMicrochipModelBuilder(Variant variant)
    {
        this.variant = variant;
    }

    @Override
    public CustomBlockStateModelBuilder with(VariantMutator variantMutator)
    {
        return new UnbakedMicrochipModelBuilder(variantMutator.apply(variant));
    }

    @Override
    public CustomBlockStateModelBuilder with(UnbakedMutator variantMutator)
    {
        return this;
    }

    @Override
    public CustomUnbakedBlockStateModel toUnbaked()
    {
        return new UnbakedMicrochipModel(variant.modelLocation(), variant.modelState());
    }
}
