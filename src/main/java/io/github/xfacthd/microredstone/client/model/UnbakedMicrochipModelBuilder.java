package io.github.xfacthd.microredstone.client.model;

import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

public final class UnbakedMicrochipModelBuilder extends CustomBlockStateModelBuilder
{
    private final Variant variant;
    private final boolean up;

    public UnbakedMicrochipModelBuilder(ResourceLocation baseModel, Variant.SimpleModelState modelState, boolean up)
    {
        this(new Variant(baseModel, modelState), up);
    }

    private UnbakedMicrochipModelBuilder(Variant variant, boolean up)
    {
        this.variant = variant;
        this.up = up;
    }

    @Override
    public CustomBlockStateModelBuilder with(VariantMutator variantMutator)
    {
        return new UnbakedMicrochipModelBuilder(variantMutator.apply(variant), up);
    }

    @Override
    public CustomBlockStateModelBuilder with(UnbakedMutator variantMutator)
    {
        return this;
    }

    @Override
    public CustomUnbakedBlockStateModel toUnbaked()
    {
        return new UnbakedMicrochipModel(variant.modelLocation(), variant.modelState(), up);
    }
}
