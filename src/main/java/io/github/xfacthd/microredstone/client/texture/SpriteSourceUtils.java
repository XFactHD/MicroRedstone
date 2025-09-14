package io.github.xfacthd.microredstone.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class SpriteSourceUtils
{
    static FrameSize calculateFrameSize(NativeImage source, @Nullable AnimationMetadataSection sourceAnim)
    {
        if (sourceAnim != null)
        {
            return sourceAnim.calculateFrameSize(source.getWidth(), source.getHeight());
        }
        return new FrameSize(source.getWidth(), source.getHeight());
    }

    static List<FrameInfo> collectFrames(NativeImage image, FrameSize size, @Nullable AnimationMetadataSection animation)
    {
        List<FrameInfo> frames = new ArrayList<>();
        int rowCount = image.getWidth() / size.width();
        // Collect explicitly specified frames
        if (animation != null && animation.frames().isPresent())
        {
            animation.frames().get().forEach(frame ->
            {
                int idx = frame.index();
                int frameX = (idx % rowCount) * size.width();
                int frameY = (idx / rowCount) * size.height();
                frames.add(new FrameInfo(idx, frameX, frameY));
            });
        }
        // Collect implicit frames if no explicit ones are specified in the animation or no animation is present
        if (frames.isEmpty())
        {
            int frameCount = rowCount * (image.getHeight() / size.height());
            for (int idx = 0; idx < frameCount; idx++)
            {
                int frameX = (idx % rowCount) * size.width();
                int frameY = (idx / rowCount) * size.height();
                frames.add(new FrameInfo(idx, frameX, frameY));
            }
        }
        return frames;
    }

    private SpriteSourceUtils() {}
}
