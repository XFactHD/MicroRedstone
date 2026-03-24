package io.github.xfacthd.microredstone.client.screen.microchip;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.menu.MicrochipCircuitMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public final class MicrochipCircuitScreen extends AbstractContainerScreen<MicrochipCircuitMenu>
{
    private static final Identifier BACKGROUND = Utils.rl("background");
    public static final String TITLE_WITH_CIRCUIT = Utils.translationKey("title", "microchip.circuit");
    public static final String TITLE_WITH_CIRCUIT_DEBUG = Utils.translationKey("title", "microchip.circuit_debug");
    static final int CANVAS_BORDER = 10;
    private static final long ARROW_REPEAT_DELAY_INITIAL = 300;

    private final MicrochipCircuitCanvas canvas = new MicrochipCircuitCanvas();
    private Component fullTitle;
    @Nullable
    private ArrowKey activeArrowKey = null;

    public MicrochipCircuitScreen(MicrochipCircuitMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.fullTitle = title;
        handleCircuitUpdate(menu.getInitialRootNode(), menu.getInitialNodeClassName());
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
        addRenderableOnly(canvas);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        if (isDragging())
        {
            graphics.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        graphics.text(font, fullTitle, titleLabelX, titleLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)
    {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_3 && canvas.canDrag(event.x(), event.y()))
        {
            canvas.drag((float) -dragX, (float) -dragY);
            setDragging(true);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_3 && isDragging())
        {
            setDragging(false);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        ArrowKey.Direction arrowDir = ArrowKey.Direction.of(event.key());
        if (arrowDir != null)
        {
            activeArrowKey = new ArrowKey(arrowDir, System.currentTimeMillis());
            canvas.drag(arrowDir);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (activeArrowKey != null && ArrowKey.Direction.of(event.key()) == activeArrowKey.dir())
        {
            activeArrowKey = null;
            return true;
        }
        return super.keyReleased(event);
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

    public void handleCircuitUpdate(@Nullable CompoundCircuitNode rootNode, @Nullable String nodeClassName)
    {
        canvas.update(rootNode);
        if (rootNode != null)
        {
            String key = Utils.PRODUCTION ? TITLE_WITH_CIRCUIT : TITLE_WITH_CIRCUIT_DEBUG;
            fullTitle = Component.translatable(key, title, rootNode.getName(), Objects.toString(nodeClassName));
        }
        else
        {
            fullTitle = title;
        }
    }

    public void handleWireStateUpdate(WireStates wireStates)
    {
        canvas.updateWireStates(wireStates);
    }
}
