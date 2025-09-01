package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

public abstract class SimpleButton extends AbstractButton
{
    private final Font font;

    protected SimpleButton(int x, int y, int width, int height, Component message)
    {
        super(x, y, width, height, message);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    protected final void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ResourceLocation texture = getSprites().get(active, isHoveredOrFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), getWidth(), getHeight(), ARGB.white(alpha));
        renderForeground(graphics, mouseX, mouseY);
    }

    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY)
    {
        renderString(graphics, font, ARGB.color(alpha, getFGColor()));
    }

    protected abstract WidgetSprites getSprites();

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        defaultButtonNarrationText(output);
    }

    protected static WidgetSprites sprites(ResourceLocation texture)
    {
        return new WidgetSprites(texture, texture.withSuffix("_hovered"));
    }
}
