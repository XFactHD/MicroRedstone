package io.github.xfacthd.microredstone.client;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.model.UnbakedMicrochipModel;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.MicrochipScreen;
import io.github.xfacthd.microredstone.client.texture.AreaMaskSource;
import io.github.xfacthd.microredstone.client.texture.PortOverlaySource;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;

@Mod(value = MicroRedstone.MOD_ID, dist = Dist.CLIENT)
public final class MRClient
{
    public MRClient(IEventBus modBus)
    {
        modBus.addListener(MRClient::onRegisterSpriteSourceTypes);
        modBus.addListener(MRClient::onRegisterBlockModels);
        modBus.addListener(MRClient::onRegisterMenuScreens);
    }

    private static void onRegisterSpriteSourceTypes(RegisterSpriteSourcesEvent event)
    {
        event.register(AreaMaskSource.ID, AreaMaskSource.CODEC);
        event.register(PortOverlaySource.ID, PortOverlaySource.CODEC);
    }

    private static void onRegisterBlockModels(RegisterBlockStateModels event)
    {
        event.registerModel(Utils.rl("microchip"), UnbakedMicrochipModel.CODEC);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(MRContent.MENU_TYPE_MICROCHIP.value(), MicrochipScreen::new);
        event.register(MRContent.MENU_TYPE_CIRCUIT_WORKBENCH.value(), CircuitWorkbenchScreen::new);
    }
}
