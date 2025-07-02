package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
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

        add(MRContent.ITEM_INTEGRATED_CIRCUIT.value(), "Integrated Circuit");

        add(MicrochipBlockEntity.MENU_TITLE, "Microchip");
    }

    private void add(Component key, String value)
    {
        ComponentContents contents = key.getContents();
        if (contents instanceof TranslatableContents translatable)
        {
            add(translatable.getKey(), value);
        }
        else
        {
            add(key.getString(), value);
        }
    }
}
