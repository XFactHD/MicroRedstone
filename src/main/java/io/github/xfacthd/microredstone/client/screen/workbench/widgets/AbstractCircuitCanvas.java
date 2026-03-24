package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.element.CircuitCanvasContentRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.LampRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCircuitCanvas implements Renderable
{
    private static final Identifier BLUEPRINT = Utils.rl("blueprint");
    public static final int PART_COUNT_X = 48;
    public static final int PART_COUNT_Y = 24; // TODO: consider increasing height to 36 cells
    public static final int PART_COUNT = PART_COUNT_X * PART_COUNT_Y;
    public static final int PART_SIZE = 8;
    public static final int PART_SLOT_SIZE = PART_SIZE + 1;
    public static final int BORDER_TOP_LEFT = 4;
    private static final int BORDER_BOTTOM_RIGHT = 5;
    public static final int WIDTH = PART_SLOT_SIZE * PART_COUNT_X + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;
    public static final int HEIGHT = PART_SLOT_SIZE * PART_COUNT_Y + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected float canvasOffX;
    protected float canvasOffY;
    protected float canvasScale = 1F; // TODO: implement zoom support

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.enableScissor(x, y, x + width, y + height);
        {
            int canvasX = x - (int) canvasOffX;
            int canvasY = y - (int) canvasOffY;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLUEPRINT, canvasX, canvasY, WIDTH, HEIGHT);

            List<PartRenderState> parts = new ArrayList<>();
            List<WireRenderState> wires = new ArrayList<>();
            List<LampRenderState> lamps = new ArrayList<>();

            collectCanvasContent(canvasX, canvasY, parts, wires, lamps, mouseX, mouseY);

            if (!parts.isEmpty() || !wires.isEmpty() || !lamps.isEmpty())
            {
                graphics.submitGuiElementRenderState(CircuitCanvasContentRenderState.create(
                        parts, wires, lamps, canvasX, canvasY, graphics.peekScissorStack()
                ));
            }

            extractCanvasOverlays(graphics, canvasX, canvasY, mouseX, mouseY);
        }
        graphics.disableScissor();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CircuitWorkbenchScreen.WINDOW_FRAME, x, y, width, height);

        extractAdditionalContent(graphics, mouseX, mouseY);
    }

    protected abstract void collectCanvasContent(
            int canvasX,
            int canvasY,
            List<PartRenderState> parts,
            List<WireRenderState> wires,
            List<LampRenderState> lamps,
            int mouseX,
            int mouseY
    );

    protected void extractCanvasOverlays(GuiGraphicsExtractor graphics, int canvasX, int canvasY, int mouseX, int mouseY) {}

    protected void extractAdditionalContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    public abstract void computeWindowSize(int width, int height);

    public abstract void computeWindowPos(int leftPos, int topPos);

    public final int getWindowWidth()
    {
        return width;
    }

    public final int getWindowHeight()
    {
        return height;
    }

    public final void drag(ArrowKey.Direction dir)
    {
        drag(dir.getDiffX(), dir.getDiffY());
    }

    public final void drag(float xDiff, float yDiff)
    {
        canvasOffX = Mth.clamp(canvasOffX + xDiff, 0, WIDTH - width);
        canvasOffY = Mth.clamp(canvasOffY + yDiff, 0, HEIGHT - height);
    }

    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return mouseX > x &&
                mouseX < (x + width - 1) &&
                mouseY > y &&
                mouseY < (y + height - 1);
    }

    public boolean canDrag(double mouseX, double mouseY)
    {
        return (width < WIDTH || height < HEIGHT) && isMouseOver(mouseX, mouseY);
    }

    @Nullable
    public final NodePos getNodePos(int mouseX, int mouseY)
    {
        if (!isMouseOver(mouseX, mouseY)) return null;

        int relX = mouseX - x + (int) canvasOffX - BORDER_TOP_LEFT;
        int relY = mouseY - y + (int) canvasOffY - BORDER_TOP_LEFT;
        if (relX < 0 || relY < 0) return null;

        int slotX = relX / PART_SLOT_SIZE;
        int slotY = relY / PART_SLOT_SIZE;
        return slotX < PART_COUNT_X && slotY < PART_COUNT_Y ? new NodePos(slotX, slotY) : null;
    }

    @Nullable
    public final ExactNodePos getExactNodePos(double mouseX, double mouseY)
    {
        if (!isMouseOver(mouseX, mouseY)) return null;

        double relX = mouseX - x + (int) canvasOffX - BORDER_TOP_LEFT;
        double relY = mouseY - y + (int) canvasOffY - BORDER_TOP_LEFT;
        if (relX < 0 || relY < 0) return null;

        double slotX = relX / PART_SLOT_SIZE;
        double slotY = relY / PART_SLOT_SIZE;
        return slotX < PART_COUNT_X && slotY < PART_COUNT_Y ? new ExactNodePos(slotX, slotY) : null;
    }
}
