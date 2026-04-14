package io.github.xfacthd.microredstone.client.texture;

import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public record InputImage(Identifier file, Resource resource, LazyLoadedImage image) {
    public InputImage(Identifier file, Resource resource, int refCount) {
        this(file, resource, new LazyLoadedImage(file, resource, refCount));
    }

    public void release() {
        image.release();
    }
}
