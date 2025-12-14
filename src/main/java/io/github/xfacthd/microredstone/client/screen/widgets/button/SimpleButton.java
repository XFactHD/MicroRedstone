package io.github.xfacthd.microredstone.client.screen.widgets.button;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public abstract class SimpleButton extends AbstractButton
{
    protected SimpleButton(int x, int y, int width, int height, Component message)
    {
        super(x, y, width, height, message);
    }

    @Override
    protected final void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        Identifier texture = getSprites().get(active, isHoveredOrFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), getWidth(), getHeight(), ARGB.white(alpha));
        renderForeground(graphics, mouseX, mouseY);
        if (isHovered())
        {
            graphics.requestCursor(isActive() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY)
    {
        renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }

    protected abstract WidgetSprites getSprites();

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        defaultButtonNarrationText(output);
    }

    protected static WidgetSprites sprites(Identifier texture)
    {
        return new WidgetSprites(texture, texture.withSuffix("_hovered"));
    }
}
