package io.github.xfacthd.microredstone.client.util.duck;

import io.github.xfacthd.microredstone.mixin.client.AccessorTextureAtlasHolder;
import net.minecraft.client.renderer.texture.TextureAtlas;

@SuppressWarnings("unused") // Used via interface injection
public interface DefaultedAccessorTextureAtlasHolder extends AccessorTextureAtlasHolder
{
    @Override
    default TextureAtlas microredstone$getTextureAtlas()
    {
        throw new AssertionError("Not injected");
    }
}
