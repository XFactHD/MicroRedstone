package io.github.xfacthd.microredstone.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record AreaMaskSource(Identifier src, Identifier sprite, int x, int y, int w, int h) implements SpriteSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<AreaMaskSource> CODEC = RecordCodecBuilder.<AreaMaskSource>mapCodec(inst -> inst.group(
            Identifier.CODEC.fieldOf("src").forGetter(AreaMaskSource::src),
            Identifier.CODEC.fieldOf("sprite").forGetter(AreaMaskSource::sprite),
            Codec.intRange(0, 15).fieldOf("x").forGetter(AreaMaskSource::x),
            Codec.intRange(0, 15).fieldOf("y").forGetter(AreaMaskSource::y),
            Codec.intRange(1, 16).fieldOf("width").forGetter(AreaMaskSource::w),
            Codec.intRange(1, 16).fieldOf("height").forGetter(AreaMaskSource::h)
    ).apply(inst, AreaMaskSource::new)).validate(res -> {
        if (res.x + res.w > 16) {
            return DataResult.error(() -> "x + width must be <= 16!");
        }
        if (res.y + res.h > 16) {
            return DataResult.error(() -> "y + height must be <= 16!");
        }
        return DataResult.success(res);
    });
    public static final Identifier ID = Utils.id("mask");

    @Override
    public void run(ResourceManager manager, Output out) {
        run(manager, out, Set.of());
    }

    @Override
    public void run(ResourceManager manager, Output out, Set<MetadataSectionType<?>> additionalMetadata) {
        Identifier srcPath = TEXTURE_ID_CONVERTER.idToFile(src);
        Optional<Resource> optSource = manager.getResource(srcPath);
        if (optSource.isEmpty()) {
            LOGGER.warn("Missing source texture: {}", srcPath);
            return;
        }

        Resource srcRes = optSource.get();
        Rect2i rect = new Rect2i(x, y, w - 1, h - 1);
        out.add(sprite, new AreaMaskInstance(new InputImage(srcPath, srcRes, 1), rect, sprite, additionalMetadata));
    }

    @Override
    public MapCodec<AreaMaskSource> codec() {
        return CODEC;
    }

    public record AreaMaskInstance(
            InputImage srcImg,
            Rect2i rect,
            Identifier sprite,
            Set<MetadataSectionType<?>> additionalMetadata
    ) implements DiscardableLoader {
        @Override
        public @Nullable SpriteContents get(SpriteResourceLoader loader) {
            try {
                NativeImage source = srcImg.image().get();

                ResourceMetadata srcMeta = srcImg.resource().metadata();
                AnimationMetadataSection sourceAnim = srcMeta
                        .getSection(AnimationMetadataSection.TYPE)
                        .orElse(null);
                FrameSize frameSize = SpriteSourceUtils.calculateFrameSize(source, sourceAnim);
                int factorX = frameSize.width() / 16;
                int factorY = frameSize.height() / 16;
                rect.setPosition(factorX * rect.getX(), factorY * rect.getY());
                rect.setWidth(rect.getWidth() * factorX);
                rect.setHeight(rect.getHeight() * factorY);

                NativeImage imageOut = new NativeImage(NativeImage.Format.RGBA, source.getWidth(), source.getHeight(), false);
                List<FrameInfo> frames = SpriteSourceUtils.collectFrames(source, frameSize, sourceAnim);
                buildOutputImage(frames, source, rect, imageOut, frameSize);
                List<MetadataSectionType.WithValue<?>> metaSections = srcMeta.getTypedSections(additionalMetadata);
                Optional<TextureMetadataSection> texMeta = srcMeta.getSection(TextureMetadataSection.TYPE);
                return new SpriteContents(sprite, frameSize, imageOut, Optional.ofNullable(sourceAnim), metaSections, texMeta);
            } catch (Exception e) {
                LOGGER.error("Failed to create masked texture '{}' from source texture'{}'", sprite, srcImg.file());
            } finally {
                srcImg.release();
            }
            return null;
        }

        private static void buildOutputImage(List<FrameInfo> frames, NativeImage source, Rect2i rect, NativeImage imageOut, FrameSize frameSize) {
            frames.forEach(frame -> {
                int fx = frame.x();
                int fy = frame.y();

                for (int y = 0; y < frameSize.height(); y++) {
                    for (int x = 0; x < frameSize.width(); x++) {
                        int absX = fx + x;
                        int absY = fy + y;
                        int color = 0;
                        if (rect.contains(x, y)) {
                            color = source.getPixel(absX, absY);
                        }
                        imageOut.setPixel(absX, absY, color);
                    }
                }
            });
        }

        public Resource getPrimaryResource() {
            return srcImg.resource();
        }

        @Override
        public void discard() {
            srcImg.release();
        }
    }
}
