package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.texture.AreaMaskSource;
import io.github.xfacthd.microredstone.client.texture.PortOverlaySource;
import io.github.xfacthd.microredstone.client.texture.StackingSource;
import io.github.xfacthd.microredstone.client.util.PortOverlays;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.resources.model.AtlasIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class MRSpriteSourceProvider extends SpriteSourceProvider
{
    public MRSpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, MicroRedstone.MOD_ID);
    }

    @Override
    protected void gather()
    {
        atlas(BLOCKS_ATLAS)
                .addSource(new AreaMaskSource(
                        ResourceLocation.withDefaultNamespace("block/moss_block"),
                        Optional.empty(),
                        Utils.rl("block/pcb"),
                        2, 2, 12, 12
                ))
                .addSource(new AreaMaskSource(
                        ResourceLocation.fromNamespaceAndPath("morered", "block/redwire_post_plate_overlay"),
                        Optional.of(Utils.rl("block/type_single")),
                        Utils.rl("block/overlay_single"),
                        0, 0, 16, 2
                ))
                .addSource(new AreaMaskSource(
                        ResourceLocation.fromNamespaceAndPath("morered", "block/bundled_cable_plate_overlay"),
                        Optional.of(Utils.rl("block/type_bundled")),
                        Utils.rl("block/overlay_bundled"),
                        0, 0, 16, 2
                ));

        SourceList guiAtlas = atlas(AtlasIds.GUI);
        PortOverlays.forEach((location, ports) ->
                guiAtlas.addSource(new PortOverlaySource(location, ports))
        );
        guiAtlas.addSource(new PortOverlaySource(
                        Utils.rl("port/border_up_single"),
                        Map.of(Port.UP, WireType.SINGLE),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_up_bundled"),
                        Map.of(Port.UP, WireType.BUNDLED),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_down_single"),
                        Map.of(Port.DOWN, WireType.SINGLE),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_down_bundled"),
                        Map.of(Port.DOWN, WireType.BUNDLED),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_left_single"),
                        Map.of(Port.LEFT, WireType.SINGLE),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_left_bundled"),
                        Map.of(Port.LEFT, WireType.BUNDLED),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_right_single"),
                        Map.of(Port.RIGHT, WireType.SINGLE),
                        Optional.of("border")
                ))
                .addSource(new PortOverlaySource(
                        Utils.rl("port/border_right_bundled"),
                        Map.of(Port.RIGHT, WireType.BUNDLED),
                        Optional.of("border")
                ));
        guiAtlas.addSource(new SingleFile(Utils.rl("neoforge", "white")));
        guiAtlas.addSource(new StackingSource(
                LampPrototypeNode.ICON_BG.icon().withPrefix("gui/sprites/"),
                List.of(LampPrototypeNode.ICON_FG.icon().withPrefix("gui/sprites/")),
                LampPrototypeNode.ICON.icon()
        ));
    }
}
