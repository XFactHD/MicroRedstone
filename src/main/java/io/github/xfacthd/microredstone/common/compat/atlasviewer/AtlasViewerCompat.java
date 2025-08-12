package io.github.xfacthd.microredstone.common.compat.atlasviewer;

import io.github.xfacthd.microredstone.client.texture.AreaMaskSource;
import io.github.xfacthd.microredstone.client.texture.PortOverlaySource;
import io.github.xfacthd.microredstone.common.compat.CompatHandler;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import xfacthd.atlasviewer.client.api.RegisterSpriteSourceDetailsEvent;

public final class AtlasViewerCompat
{
    public static final Component LABEL_MASK_TEXTURE = Utils.translate("label", "source_tooltip.area_mask.texture");
    public static final Component LABEL_MASK_SPRITE = Utils.translate("label", "source_tooltip.area_mask.sprite");
    public static final Component LABEL_MASK_AREA = Utils.translate("label", "source_tooltip.area_mask.area");
    public static final String VALUE_MASK_AREA = Utils.translationKey("value", "source_tooltip.area_mask.area");
    public static final Component LABEL_PORT_SPRITE = Utils.translate("label", "source_tooltip.port_overlay.sprite");
    public static final Component LABEL_PORT_ENTRIES = Utils.translate("label", "source_tooltip.port_overlay.entries");
    public static final String VALUE_PORT_ENTRY = Utils.translationKey("value", "source_tooltip.port_overlay.entry");
    public static final Component LABEL_PORT_TYPE_PREFIX = Utils.translate("label", "source_tooltip.port_overlay.type_prefix");

    public static void init(IEventBus modBus)
    {
        if (ModList.get().isLoaded("atlasviewer"))
        {
            try
            {
                GuardedClientAccess.init(modBus);
            }
            catch (Throwable t)
            {
                CompatHandler.LOGGER.error("Failed to initialize AtlasViewer compat", t);
            }
        }
    }

    private static final class GuardedClientAccess
    {
        static void init(IEventBus modBus)
        {
            modBus.addListener(GuardedClientAccess::onRegisterSpriteSourceDetails);
        }

        private static void onRegisterSpriteSourceDetails(RegisterSpriteSourceDetailsEvent event)
        {
            event.registerPrimaryResourceGetter(AreaMaskSource.AreaMaskInstance.class, AreaMaskSource.AreaMaskInstance::srcRes);
            event.registerPrimaryResourceGetter(PortOverlaySource.PortOverlaySupplier.class, PortOverlaySource.PortOverlaySupplier::getPrimaryResource);

            event.registerSourceTooltipAppender(AreaMaskSource.class, (src, consumer) ->
            {
                consumer.accept(LABEL_MASK_TEXTURE, Component.literal(src.src().toString()));
                consumer.accept(LABEL_MASK_SPRITE, Component.literal(src.sprite().toString()));
                consumer.accept(LABEL_MASK_AREA, Component.translatable(VALUE_MASK_AREA, src.x(), src.y(), src.w(), src.h()));
            });
            event.registerSourceTooltipAppender(PortOverlaySource.class, (src, consumer) ->
            {
                consumer.accept(LABEL_PORT_SPRITE, Component.literal(src.sprite().toString()));
                consumer.accept(LABEL_PORT_ENTRIES, Component.empty());
                src.portOverlays().forEach((port, type) ->
                        consumer.accept(null, Component.translatable(VALUE_PORT_ENTRY, port.toString(), type.toString()))
                );
                src.typePrefix().ifPresent(prefix -> consumer.accept(LABEL_PORT_TYPE_PREFIX, Component.literal(prefix)));
            });
        }

        private GuardedClientAccess() {}
    }

    private AtlasViewerCompat() {}
}
