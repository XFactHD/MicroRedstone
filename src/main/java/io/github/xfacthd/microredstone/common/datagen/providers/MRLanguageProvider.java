package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.MRContent;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class MRLanguageProvider extends LanguageProvider
{
    public MRLanguageProvider(PackOutput output)
    {
        super(output, MicroRedstone.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations()
    {
        add(MRContent.BLOCK_MICROCHIP.value(), "Microchip");
    }
}
