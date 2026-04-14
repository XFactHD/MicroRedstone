package io.github.xfacthd.microredstone.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record StackingSource(Identifier primary, List<Identifier> secondaries, Identifier sprite) implements SpriteSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<StackingSource> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Identifier.CODEC.fieldOf("primary_texture").forGetter(StackingSource::primary),
            Identifier.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("secondary_textures").forGetter(StackingSource::secondaries),
            Identifier.CODEC.fieldOf("sprite").forGetter(StackingSource::sprite)
    ).apply(inst, StackingSource::new));
    public static final Identifier ID = Utils.rl("stacking");

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        run(resourceManager, output, Set.of());
    }

    @Override
    public void run(ResourceManager resourceManager, Output output, Set<MetadataSectionType<?>> additionalMetadata) {
        Identifier primaryFile = TEXTURE_ID_CONVERTER.idToFile(primary);
        Optional<Resource> optPrimary = resourceManager.getResource(primaryFile);
        if (optPrimary.isEmpty()) {
            LOGGER.warn("Missing primary source texture: {}", primaryFile);
            return;
        }
        InputImage primaryImage = new InputImage(primaryFile, optPrimary.get(), 1);

        List<InputImage> secondaryImages = new ArrayList<>(secondaries.size());
        for (Identifier secondary : secondaries) {
            Identifier secondaryFile = TEXTURE_ID_CONVERTER.idToFile(secondary);
            Optional<Resource> optSecondary = resourceManager.getResource(secondaryFile);
            if (optSecondary.isEmpty()) {
                LOGGER.warn("Missing primary source texture: {}", secondaryFile);
                return;
            }

            secondaryImages.add(new InputImage(secondaryFile, optSecondary.get(), 1));
        }

        output.add(sprite, new StackingSupplier(primaryImage, secondaryImages, sprite, additionalMetadata));
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }

    public record StackingSupplier(
            InputImage primaryImage,
            List<InputImage> secondaryImages,
            Identifier sprite,
            Set<MetadataSectionType<?>> additionalMetadata
    ) implements DiscardableLoader {
        @Override
        public @Nullable SpriteContents get(SpriteResourceLoader loader) {
            try {
                for (InputImage image : secondaryImages) {
                    if (image.resource().metadata().getSection(AnimationMetadataSection.TYPE).isPresent()) {
                        throw new IllegalArgumentException("Secondary image '" + image.file() + "' has an animation, this is unsupported");
                    }
                }

                NativeImage primarySource = primaryImage.image().get();
                List<NativeImage> secondarySources = secondaryImages.stream()
                        .map(InputImage::image)
                        .map(Utils.uncheckIO(LazyLoadedImage::get))
                        .toList();

                ResourceMetadata primaryMeta = primaryImage.resource().metadata();
                AnimationMetadataSection sourceAnim = primaryMeta
                        .getSection(AnimationMetadataSection.TYPE)
                        .orElse(null);
                FrameSize frameSize = SpriteSourceUtils.calculateFrameSize(primarySource, sourceAnim);

                List<FrameSize> sizes = secondarySources.stream()
                        .map(img -> new FrameSize(img.getWidth(), img.getHeight()))
                        .distinct()
                        .toList();
                if (sizes.size() != 1 || !sizes.getFirst().equals(frameSize)) {
                    List<FrameSize> allSizes = new ArrayList<>(sizes);
                    allSizes.add(frameSize);
                    throw new IllegalArgumentException("Encountered different image sizes: " + allSizes);
                }

                NativeImage imageOut = new NativeImage(NativeImage.Format.RGBA, primarySource.getWidth(), primarySource.getHeight(), false);
                List<FrameInfo> frames = SpriteSourceUtils.collectFrames(primarySource, frameSize, sourceAnim);
                buildOutputImage(frames, primarySource, secondarySources, imageOut, frameSize);
                List<MetadataSectionType.WithValue<?>> metaSections = primaryMeta.getTypedSections(additionalMetadata);
                Optional<TextureMetadataSection> texMeta = primaryMeta.getSection(TextureMetadataSection.TYPE);
                return new SpriteContents(sprite, frameSize, imageOut, Optional.ofNullable(sourceAnim), metaSections, texMeta);
            } catch (Exception e) {
                LOGGER.error("Failed to create stacked texture '{}' from source texture '{}'", sprite, primaryImage.file());
            } finally {
                discard();
            }
            return null;
        }

        private static void buildOutputImage(List<FrameInfo> frames, NativeImage primary, List<NativeImage> secondaries, NativeImage imageOut, FrameSize frameSize) {
            imageOut.copyFrom(primary);
            for (NativeImage secondary : secondaries) {
                for (FrameInfo frame : frames) {
                    int fx = frame.x();
                    int fy = frame.y();

                    for (int y = 0; y < frameSize.height(); y++) {
                        for (int x = 0; x < frameSize.width(); x++) {
                            int absX = fx + x;
                            int absY = fy + y;

                            int secColor = secondary.getPixelABGR(x, y);
                            int secAlpha = ARGB.alpha(secColor);
                            if (secAlpha == 0) {
                                continue;
                            }

                            if (secAlpha < 255) {
                                int primColor = imageOut.getPixelABGR(absX, absY);
                                if (ARGB.alpha(primColor) > 0) {
                                    secColor = ARGB.multiply(primColor, secColor);
                                }
                            }
                            imageOut.setPixelABGR(absX, absY, secColor);
                        }
                    }
                }
            }
        }

        public Resource getPrimaryResource() {
            return primaryImage.resource();
        }

        @Override
        public void discard() {
            primaryImage.release();
            secondaryImages.forEach(InputImage::release);
        }
    }
}
