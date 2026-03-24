package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

@FunctionalInterface
public interface PartBlitter
{
    void blit(Matrix3x2f pose, Identifier icon, int x, int y, int size);

    static PartBlitter of(GuiGraphicsExtractor graphics)
    {
        return (_, icon, x, y, size) ->
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, size, size);
    }
}
