package io.github.xfacthd.microredstone.client.screen.microchip;

import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.menu.MicrochipCircuitMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public final class MicrochipCircuitScreen extends AbstractContainerScreen<MicrochipCircuitMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("background");
    static final int CANVAS_BORDER = 10;

    private final MicrochipCircuitCanvas canvas = new MicrochipCircuitCanvas();

    public MicrochipCircuitScreen(MicrochipCircuitMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        canvas.update(menu.getInitialRootNode());
    }

    @Override
    protected void init()
    {
        canvas.computeWindowSize(width, height);
        imageWidth = canvas.getWindowWidth() + CANVAS_BORDER * 2;
        imageHeight = canvas.getWindowHeight() + CANVAS_BORDER * 3;

        super.init();

        canvas.computeWindowPos(leftPos, topPos);
        canvas.drag(0, 0); // Clamp canvas offset
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);
        canvas.render(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
    }

    public void handleCircuitUpdate(@Nullable CompoundCircuitNode rootNode)
    {
        canvas.update(rootNode);
    }

    public void handleWireStateUpdate(WireStates wireStates)
    {
        canvas.updateWireStates(wireStates);
    }
}
