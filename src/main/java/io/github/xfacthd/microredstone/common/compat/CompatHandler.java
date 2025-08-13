package io.github.xfacthd.microredstone.common.compat;

import com.mojang.logging.LogUtils;
import io.github.xfacthd.microredstone.common.compat.atlasviewer.AtlasViewerCompat;
import io.github.xfacthd.microredstone.common.compat.morered.MoreRedCompat;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

public final class CompatHandler
{
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init(IEventBus modBus)
    {
        AtlasViewerCompat.init(modBus);
        MoreRedCompat.init(modBus);
    }

    private CompatHandler() {}
}
