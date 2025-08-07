package io.github.xfacthd.microredstone.client.screen.workbench;

import com.mojang.datafixers.util.Either;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.PartsList;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public final class CircuitWorkbenchScreen extends AbstractContainerScreen<CircuitWorkbenchMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("background");
    public static final ResourceLocation WINDOW_FRAME = Utils.rl("window_frame");
    public static final int PADDING = 5;
    public static final int BORDER_LEFT = PADDING * 2;
    public static final int BORDER_RIGHT = PADDING * 2 + 1;
    public static final int OFFSET_TOP = 20;
    private static final int BORDER_BOTTOM = PADDING * 2 + 1;
    public static final int NON_CIRCUIT_WIDTH = BORDER_LEFT + BORDER_RIGHT + PartsList.FULL_WIDTH + PADDING;
    public static final int NON_CIRCUIT_HEIGHT = OFFSET_TOP + BORDER_BOTTOM + CircuitCanvas.CELL_COORD_TEXT_HEIGHT;
    private static final long ARROW_REPEAT_DELAY_INITIAL = 300;

    private final CircuitCanvas canvas = new CircuitCanvas(this);
    private final PartsList partsList = new PartsList(this);
    @Nullable
    private Either<NodePos, Integer> dragStart = null;
    @Nullable
    private FloatingNode floatingNode = null;
    @Nullable
    private ArrowKey activeArrowKey = null;

    public CircuitWorkbenchScreen(CircuitWorkbenchMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        inventoryLabelY = -20; // TODO: set proper position
        // TODO: consider making the canvas size configurable in the workbench screen (requires storing the size in the assembled circuit for reconstruction)
        canvas.setCanvasSize(CircuitCanvas.MAX_WIDTH, CircuitCanvas.MAX_HEIGHT);
    }

    @Override
    protected void init()
    {
        canvas.computeWindowSize(width, height);
        imageWidth = canvas.getWindowWidth() + NON_CIRCUIT_WIDTH;
        imageHeight = canvas.getWindowHeight() + NON_CIRCUIT_HEIGHT;

        super.init();

        canvas.computeWindowPos(leftPos, topPos);
        partsList.computeDimensions(leftPos, topPos, imageWidth, height);

        canvas.drag(0, 0); // Clamp canvas offset
        partsList.scroll(0); // Clamp parts list offset
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        canvas.render(graphics, mouseX, mouseY);
        partsList.render(graphics, mouseX, mouseY);

        if (floatingNode != null)
        {
            CircuitCanvas.drawPartNode(graphics, floatingNode.node().getIcon(), mouseX - 4, mouseY - 4, floatingNode.rotation());
        }
        if (canvas.isPullingWire() && Objects.requireNonNull(canvas.getWireInProgress()).isEmpty())
        {
            // TODO: render indicator on the cursor
            //graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, mouseX - 4, mouseY - 8, 8, 8);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!isDragging() && button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            if (canvas.isPullingWire())
            {
                canvas.pullWire((int) mouseX, (int) mouseY);
                return true;
            }
            NodePos nodePos = canvas.getNodePos((int) mouseX, (int) mouseY);
            if (nodePos != null)
            {
                dragStart = Either.left(nodePos);
            }
            else if (partsList.isMouseOverList(mouseX, mouseY))
            {
                dragStart = partsList.getClickedPartIdx(mouseY);
            }
            if (dragStart != null)
            {
                setDragging(true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (floatingNode != null)
        {
            return true;
        }
        if (dragStart != null)
        {
            PlaceableNode node = getDraggedNode(dragStart);
            if (node != null)
            {
                NodePos lastPos = dragStart.left().orElse(null);
                floatingNode = FloatingNode.of(node, lastPos);
            }
            dragStart = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_3 && canvas.isMouseOver(mouseX, mouseY))
        {
            canvas.drag((float) -dragX, (float) -dragY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && (partsList.isDragging() || partsList.isMouseOverScrollBar(mouseX, mouseY)))
        {
            partsList.dragScrollBar(mouseY);
            partsList.setDragging(true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (partsList.isDragging())
        {
            partsList.setDragging(false);
            return true;
        }
        if (isDragging())
        {
            if (floatingNode != null)
            {
                NodePos target = canvas.getNodePos((int) mouseX, (int) mouseY);
                boolean revertToLast = false;
                if (target != null && !floatingNode.canPlaceAt(canvas, target))
                {
                    target = null;
                }
                if (target == null)
                {
                    target = floatingNode.lastPos();
                    revertToLast = true;
                }
                if (target != null)
                {
                    floatingNode.placeAt(canvas, target, revertToLast);
                }
                floatingNode = null;
            }
            setDragging(false);
            dragStart = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (partsList.isMouseOver(mouseX, mouseY))
        {
            partsList.scroll(-scrollY);
            return true;
        }
        if (floatingNode != null)
        {
            floatingNode = floatingNode.rotate(Mth.sign(-scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        if (canvas.isPullingWire() && keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            canvas.cancelWirePull();
            return true;
        }
        // TODO: augment with UI button
        if (!canvas.isPullingWire() && keyCode == GLFW.GLFW_KEY_W)
        {
            canvas.startWirePull(WireType.SINGLE);
            return true;
        }
        if (!canvas.isPullingWire() && keyCode == GLFW.GLFW_KEY_B)
        {
            canvas.startWirePull(WireType.BUNDLED);
            return true;
        }
        if (floatingNode != null && keyCode == GLFW.GLFW_KEY_R)
        {
            floatingNode = floatingNode.rotate(Screen.hasShiftDown() ? -1 : 1);
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

    @Nullable
    private PlaceableNode getDraggedNode(Either<NodePos, Integer> dragStart)
    {
        return dragStart.map(canvas.getPartGrid()::getPartNode, listSlot -> PartsList.getEntryAt(listSlot).create());
    }

    @Nullable
    public FloatingNode getFloatingNode()
    {
        return floatingNode;
    }

    public boolean isNodeFloating(PlaceableNode node)
    {
        return floatingNode != null && floatingNode.node() == node;
    }
}
