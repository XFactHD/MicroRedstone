package io.github.xfacthd.microredstone.client.texture;

import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public record InputImage(ResourceLocation file, Resource resource, LazyLoadedImage image)
{
    public InputImage(ResourceLocation file, Resource resource, int refCount)
    {
        this(file, resource, new LazyLoadedImage(file, resource, refCount));
    }

    public void release()
    {
        image.release();
    }
}
