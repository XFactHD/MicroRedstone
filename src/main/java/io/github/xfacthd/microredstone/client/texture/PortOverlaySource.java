package io.github.xfacthd.microredstone.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.apache.commons.io.function.Uncheck;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PortOverlaySource(Identifier sprite, Map<Port, WireType> portOverlays, Optional<String> typePrefix) implements SpriteSource
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<PortOverlaySource> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Identifier.CODEC.fieldOf("sprite").forGetter(PortOverlaySource::sprite),
            Codec.unboundedMap(Port.CODEC, WireType.CODEC).fieldOf("ports").forGetter(PortOverlaySource::portOverlays),
            Codec.STRING.optionalFieldOf("type_prefix").forGetter(PortOverlaySource::typePrefix)
    ).apply(inst, PortOverlaySource::new));
    public static final Identifier ID = Utils.rl("port_overlay");

    public PortOverlaySource(Identifier sprite, Map<Port, WireType> portOverlays)
    {
        this(sprite, portOverlays, Optional.empty());
    }

    @Override
    public void run(ResourceManager resourceManager, Output output)
    {
        if (portOverlays.isEmpty()) return;

        Map<Port, Pair<Resource, LazyLoadedImage>> portImages = new HashMap<>();
        Object2IntMap<WireType> types = new Object2IntOpenHashMap<>();
        for (WireType type : portOverlays.values())
        {
            types.compute(type, (_, count) -> count == null ? 1 : (count + 1));
        }
        String prefix = typePrefix.map(s -> s + "_").orElse("");
        Map<WireType, Pair<Resource, LazyLoadedImage>> resources = new HashMap<>();
        for (Object2IntMap.Entry<WireType> entry : types.object2IntEntrySet())
        {
            WireType type = entry.getKey();
            Identifier sprite = Utils.rl("gui/sprites/port/" + prefix + type.getSerializedName());
            Identifier location = TEXTURE_ID_CONVERTER.idToFile(sprite);
            Optional<Resource> typeResource = resourceManager.getResource(location);
            if (typeResource.isEmpty())
            {
                LOGGER.warn("Port overlay '{}' does not exist", location);
                return;
            }
            LazyLoadedImage image = new LazyLoadedImage(location, typeResource.get(), entry.getIntValue());
            resources.put(type, Pair.of(typeResource.get(), image));
        }
        portOverlays.forEach((port, type) -> portImages.put(port, resources.get(type)));
        output.add(sprite, new PortOverlaySupplier(sprite, portImages));
    }

    @Override
    public MapCodec<PortOverlaySource> codec()
    {
        return CODEC;
    }

    public record PortOverlaySupplier(Identifier sprite, Map<Port, Pair<Resource, LazyLoadedImage>> portImages) implements DiscardableLoader
    {
        @Override
        public SpriteContents get(SpriteResourceLoader loader)
        {
            try
            {
                FrameSize size = checkImageSizes();
                NativeImage imageOut = new NativeImage(size.width(), size.height(), true);

                portImages.forEach((port, pair) ->
                {
                    NativeImage image = Uncheck.apply(LazyLoadedImage::get, pair.getSecond());
                    switch (port)
                    {
                        case UP -> copyRect(image, imageOut, true, false);
                        case RIGHT -> copyRect(image, imageOut, false, true);
                        case DOWN -> copyRect(image, imageOut, true, true);
                        case LEFT -> copyRect(image, imageOut, false, false);
                    }
                });
                return new SpriteContents(sprite, size, imageOut);
            }
            catch (Throwable t)
            {
                LOGGER.warn("Failed to create port overlay {}", sprite, t);
            }
            finally
            {
                portImages.values().forEach(pair -> pair.getSecond().release());
            }
            return MissingTextureAtlasSprite.create();
        }

        private static void copyRect(NativeImage src, NativeImage dest, boolean rotate, boolean mirror)
        {
            int width = src.getWidth();
            int height = src.getHeight();

            for (int srcY = 0; srcY < height; srcY++)
            {
                int destY = mirror ? height - 1 - srcY : srcY;
                for (int srcX = 0; srcX < width; srcX++)
                {
                    int destX = mirror ? width - 1 - srcX : srcX;
                    int color = src.getPixelABGR(srcX, srcY);
                    if (ARGB.alpha(color) > 0)
                    {
                        dest.setPixelABGR(rotate ? destY : destX, rotate ? destX : destY, color);
                    }
                }
            }
        }

        private FrameSize checkImageSizes()
        {
            List<FrameSize> sizes = portImages.values()
                    .stream()
                    .map(Pair::getSecond)
                    .map(Utils.uncheckIO(LazyLoadedImage::get))
                    .map(img -> new FrameSize(img.getWidth(), img.getHeight()))
                    .distinct()
                    .toList();
            if (sizes.size() != 1)
            {
                throw new IllegalArgumentException("Encountered different image sizes: " + sizes);
            }
            return sizes.getFirst();
        }

        @Override
        public void discard()
        {
            portImages.values().forEach(pair -> pair.getSecond().release());
        }

        public Resource getPrimaryResource()
        {
            return portImages.values().iterator().next().getFirst();
        }
    }
}
