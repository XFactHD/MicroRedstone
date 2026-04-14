package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.common.MRContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public final class MRRecipeProvider extends RecipeProvider
{
    private MRRecipeProvider(HolderLookup.Provider registries, RecipeOutput output)
    {
        super(registries, output);
    }

    @Override
    protected void buildRecipes()
    {
        shaped(RecipeCategory.REDSTONE, MRContent.BLOCK_MICROCHIP.value(), 4)
                .pattern("SRS")
                .pattern("RGR")
                .pattern("SRS")
                .define('S', Items.SMOOTH_STONE_SLAB)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('G', Tags.Items.INGOTS_GOLD)
                .unlockedBy("hasRedstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(output);

        shaped(RecipeCategory.REDSTONE, MRContent.BLOCK_CIRCUIT_WORKBENCH.value())
                .pattern("I")
                .pattern("C")
                .define('I', MRContent.ITEM_INTEGRATED_CIRCUIT.value())
                .define('C', Items.CRAFTING_TABLE)
                .unlockedBy("hasIntegratedCircuit", has(MRContent.ITEM_INTEGRATED_CIRCUIT.value()))
                .save(output);

        shaped(RecipeCategory.REDSTONE, MRContent.ITEM_INTEGRATED_CIRCUIT.value())
                .pattern("GRG")
                .pattern("RCR")
                .pattern("GRG")
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Items.COMPARATOR)
                .unlockedBy("hasComparator", has(Items.COMPARATOR))
                .save(output);
    }

    public static final class Runner extends RecipeProvider.Runner
    {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries)
        {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output)
        {
            return new MRRecipeProvider(registries, output);
        }

        @Override
        public String getName()
        {
            return "Recipes - MicroRedstone";
        }
    }
}
