package io.github.xfacthd.microredstone.client.screen.microchip;

import io.github.xfacthd.microredstone.client.util.ArrowKey;
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
import org.lwjgl.glfw.GLFW;

public final class MicrochipCircuitScreen extends AbstractContainerScreen<MicrochipCircuitMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("background");
    static final int CANVAS_BORDER = 10;
    private static final long ARROW_REPEAT_DELAY_INITIAL = 300;

    private final MicrochipCircuitCanvas canvas = new MicrochipCircuitCanvas();
    @Nullable
    private ArrowKey activeArrowKey = null;

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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == GLFW.GLFW_MOUSE_BUTTON_3 && canvas.isMouseOver(mouseX, mouseY))
        {
            canvas.drag((float) -dragX, (float) -dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        ArrowKey.Direction arrowDir = ArrowKey.Direction.of(keyCode);
        if (arrowDir != null)
        {
            activeArrowKey = new ArrowKey(arrowDir, System.currentTimeMillis());
            canvas.drag(arrowDir);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        if (activeArrowKey != null && ArrowKey.Direction.of(keyCode) == activeArrowKey.dir())
        {
            activeArrowKey = null;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick()
    {
        if (activeArrowKey != null)
        {
            long diff = System.currentTimeMillis() - activeArrowKey.startTime();
            if (diff > ARROW_REPEAT_DELAY_INITIAL)
            {
                canvas.drag(activeArrowKey.dir());
            }
        }
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
