package io.github.xfacthd.microredstone.common.datagen;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.datagen.providers.MRBlockModelProvider;
import io.github.xfacthd.microredstone.common.datagen.providers.MRLanguageProvider;
import io.github.xfacthd.microredstone.common.datagen.providers.MRSpriteSourceProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(MicroRedstone.MOD_ID)
public final class GeneratorHandler
{
    public GeneratorHandler(IEventBus modBus)
    {
        modBus.addListener(GeneratorHandler::onGatherData);
    }

    private static void onGatherData(GatherDataEvent.Client event)
    {
        event.createProvider(MRBlockModelProvider::new);
        event.createProvider(MRSpriteSourceProvider::new);
        event.createProvider(MRLanguageProvider::new);
    }
}
