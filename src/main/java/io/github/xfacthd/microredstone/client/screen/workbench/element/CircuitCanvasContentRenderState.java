package io.github.xfacthd.microredstone.client.screen.workbench.element;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.PartBlitter;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;

public record CircuitCanvasContentRenderState(
        List<PartRenderState> parts,
        List<WireRenderState> wires,
        int canvasX,
        int canvasY,
        TextureAtlas guiAtlas,
        TextureAtlasSprite whiteSprite,
        TextureSetup textureSetup,
        @Nullable ScreenRectangle bounds,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState
{
    private static final ResourceLocation WHITE_SPRITE = Utils.rl("neoforge", "white");

    public static CircuitCanvasContentRenderState create(
            List<PartRenderState> parts,
            List<WireRenderState> wires,
            int canvasX,
            int canvasY,
            @Nullable ScreenRectangle scissorArea
    )
    {
        ScreenRectangle bounds = getBounds(canvasX, canvasY, scissorArea);
        TextureAtlas guiAtlas = Minecraft.getInstance().getGuiSprites().microredstone$getTextureAtlas();
        TextureAtlasSprite whiteSprite = guiAtlas.getSprite(WHITE_SPRITE);
        TextureSetup textureSetup = TextureSetup.singleTexture(guiAtlas.getTextureView());
        return new CircuitCanvasContentRenderState(parts, wires, canvasX, canvasY, guiAtlas, whiteSprite, textureSetup, bounds, scissorArea);
    }

    @Override
    public void buildVertices(VertexConsumer buffer, float z)
    {
        Matrix3x2fStack pose = new Matrix3x2fStack(2);

        for (WireRenderState wire : wires)
        {
            // TODO: shorten sections going into parts to only enter the part by one pixel and then render wires after parts again
            // TODO: draw textures instead of colored lines (same for nodes)

            int packedColor = wire.packedColor();
            for (RoutedWire.Section section : wire.sections())
            {
                NodePos posOne = section.posOne();
                NodePos posTwo = section.posTwo();
                if (posOne.x() == posTwo.x())
                {
                    int minY = Math.min(posOne.y(), posTwo.y());
                    int maxY = Math.max(posOne.y(), posTwo.y());
                    int x = canvasX + 4 + posOne.x() * CircuitCanvas.PART_SLOT_SIZE + 4;
                    int y1 = canvasY + 4 + minY * CircuitCanvas.PART_SLOT_SIZE + 4;
                    int y2 = canvasY + 5 + maxY * CircuitCanvas.PART_SLOT_SIZE + 4;
                    fill(buffer, x, y1, x + 2, y2, z, packedColor);
                }
                else if (posOne.y() == posTwo.y())
                {
                    int minX = Math.min(posOne.x(), posTwo.x());
                    int maxX = Math.max(posOne.x(), posTwo.x());
                    int x1 = canvasX + 4 + minX * CircuitCanvas.PART_SLOT_SIZE + 4;
                    int x2 = canvasX + 5 + maxX * CircuitCanvas.PART_SLOT_SIZE + 4;
                    int y = canvasY + 4 + posOne.y() * CircuitCanvas.PART_SLOT_SIZE + 4;
                    fill(buffer, x1, y, x2, y + 2, z, packedColor);
                }
            }
            for (NodePos pos : wire.nodes())
            {
                int x = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE + 4;
                int y = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE + 4;
                fill(buffer, x - 2, y - 2, x + 2, y + 2, z, packedColor);
            }
        }
        PartBlitter blitter = (blitPose, icon, x, y, size) -> blit(buffer, blitPose, icon, x, y, x + size, y + size, z);
        for (PartRenderState part : parts)
        {
            NodePos pos = part.pos();
            int x = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE;
            int y = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE;
            CircuitCanvas.drawPartNode(pose, part.icon(), x, y, part.rotation(), CircuitCanvas.PART_SIZE, blitter);
        }
    }

    private void blit(VertexConsumer buffer, Matrix3x2f pose, ResourceLocation texture, int x0, int y0, int x1, int y1, float z)
    {
        blit(buffer, pose, texture, x0, y0, x1, y1, z, 0xFFFFFFFF);
    }

    private void blit(VertexConsumer buffer, Matrix3x2f pose, ResourceLocation texture, int x0, int y0, int x1, int y1, float z, int color)
    {
        TextureAtlasSprite sprite = guiAtlas.getSprite(texture);
        buffer.addVertexWith2DPose(pose, x0, y0, z).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
        buffer.addVertexWith2DPose(pose, x0, y1, z).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y1, z).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y0, z).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
    }

    private void fill(VertexConsumer buffer, int x0, int y0, int x1, int y1, float z, int color)
    {
        buffer.addVertex(x0, y0, z).setUv(whiteSprite.getU0(), whiteSprite.getV0()).setColor(color);
        buffer.addVertex(x0, y1, z).setUv(whiteSprite.getU0(), whiteSprite.getV1()).setColor(color);
        buffer.addVertex(x1, y1, z).setUv(whiteSprite.getU1(), whiteSprite.getV1()).setColor(color);
        buffer.addVertex(x1, y0, z).setUv(whiteSprite.getU1(), whiteSprite.getV0()).setColor(color);
    }

    @Override
    public RenderPipeline pipeline()
    {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Nullable
    private static ScreenRectangle getBounds(int x, int y, @Nullable ScreenRectangle scissorArea)
    {
        ScreenRectangle rect = new ScreenRectangle(x, y, CircuitCanvas.WIDTH, CircuitCanvas.HEIGHT);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }
}
