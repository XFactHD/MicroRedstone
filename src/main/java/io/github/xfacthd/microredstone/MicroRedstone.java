package io.github.xfacthd.microredstone;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.compat.CompatHandler;
import io.github.xfacthd.microredstone.common.net.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MicroRedstone.MOD_ID)
@SuppressWarnings("UtilityClassWithPublicConstructor")
public final class MicroRedstone
{
    public static final String MOD_ID = "microredstone";

    public MicroRedstone(IEventBus modBus)
    {
        MRContent.init(modBus);
        CompatHandler.init(modBus);

        modBus.addListener(NetworkHandler::onRegisterPayloadHandlers);
        modBus.addListener(NetworkHandler::onRegisterConfigTasks);
    }
}
