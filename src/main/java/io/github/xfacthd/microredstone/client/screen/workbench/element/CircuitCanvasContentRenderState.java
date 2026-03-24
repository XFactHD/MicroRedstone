package io.github.xfacthd.microredstone.client.screen.workbench.element;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.PartBlitter;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;

public record CircuitCanvasContentRenderState(
        List<PartRenderState> parts,
        List<WireRenderState> wires,
        List<LampRenderState> lamps,
        int canvasX,
        int canvasY,
        TextureAtlas guiAtlas,
        TextureAtlasSprite whiteSprite,
        TextureSetup textureSetup,
        @Nullable ScreenRectangle bounds,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState
{
    static final Identifier WHITE_SPRITE = Utils.rl("neoforge", "white");

    public static CircuitCanvasContentRenderState create(
            List<PartRenderState> parts,
            List<WireRenderState> wires,
            List<LampRenderState> lamps,
            int canvasX,
            int canvasY,
            @Nullable ScreenRectangle scissorArea
    )
    {
        ScreenRectangle bounds = getBounds(canvasX, canvasY, scissorArea);
        TextureAtlas guiAtlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        TextureAtlasSprite whiteSprite = guiAtlas.getSprite(WHITE_SPRITE);
        TextureSetup textureSetup = TextureSetup.singleTexture(guiAtlas.getTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
        return new CircuitCanvasContentRenderState(parts, wires, lamps, canvasX, canvasY, guiAtlas, whiteSprite, textureSetup, bounds, scissorArea);
    }

    @Override
    public void buildVertices(VertexConsumer buffer)
    {
        Matrix3x2fStack pose = new Matrix3x2fStack(2);

        PartBlitter blitter = (blitPose, icon, x, y, size) -> blit(buffer, blitPose, icon, x, y, x + size, y + size);
        for (PartRenderState part : parts)
        {
            NodePos pos = part.pos();
            int x = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE;
            int y = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE;
            CircuitCanvas.drawPartNode(pose, part.icon(), x, y, part.rotation(), CircuitCanvas.PART_SIZE, blitter);
        }
        for (LampRenderState lamp : lamps)
        {
            NodePos pos = lamp.pos();
            int x = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE;
            int y = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE;

            CircuitCanvas.drawPartNode(pose, LampPrototypeNode.ICON_BG, x, y, lamp.rotation(), CircuitCanvas.PART_SIZE, blitter);
            fill(buffer, pose, x + 1, y + 1, x + CircuitCanvas.PART_SIZE - 1, y + CircuitCanvas.PART_SIZE - 1, lamp.packedColor());
            CircuitCanvas.drawPartNode(pose, LampPrototypeNode.ICON_FG, x, y, 0, CircuitCanvas.PART_SIZE, blitter);
        }
        for (LampRenderState lamp : lamps)
        {
            if (!lamp.chainedToNeighbor()) continue;

            NodePos pos = lamp.pos();
            int cx = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE - 2;
            int cy = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE - 2;
            int size = CircuitCanvas.PART_SIZE + 4;
            int rotation = lamp.rotation();

            CircuitCanvas.drawPartNode(pose, LampPrototypeNode.ICON_CHAIN, cx, cy, rotation, size, blitter);

            pose.pushMatrix();
            pose.translate(cx, cy);
            if (rotation != 0)
            {
                pose.rotateAbout((float) Math.toRadians(90 * rotation), size / 2F, size / 2F);
            }
            fill(buffer, pose, 0, 3, 3, CircuitCanvas.PART_SIZE + 1, lamp.packedColor());
            pose.popMatrix();
        }

        for (WireRenderState wire : wires)
        {
            // TODO: draw textures instead of colored lines (same for nodes)

            int packedColor = wire.packedColor();
            for (WireRenderState.WireSection section : wire.sections())
            {
                int x0 = canvasX + section.minX();
                int y0 = canvasY + section.minY();
                int x1 = canvasX + section.maxX();
                int y1 = canvasY + section.maxY();
                fill(buffer, pose, x0, y0, x1, y1, packedColor);
            }
            for (NodePos pos : wire.nodes())
            {
                int x = canvasX + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.x() * CircuitCanvas.PART_SLOT_SIZE + 4;
                int y = canvasY + CircuitCanvas.BORDER_TOP_LEFT + 1 + pos.y() * CircuitCanvas.PART_SLOT_SIZE + 4;
                fill(buffer, pose, x - 2, y - 2, x + 2, y + 2, packedColor);
            }
        }
    }

    private void blit(VertexConsumer buffer, Matrix3x2f pose, Identifier texture, int x0, int y0, int x1, int y1)
    {
        blit(buffer, pose, texture, x0, y0, x1, y1, 0xFFFFFFFF);
    }

    private void blit(VertexConsumer buffer, Matrix3x2f pose, Identifier texture, int x0, int y0, int x1, int y1, int color)
    {
        TextureAtlasSprite sprite = guiAtlas.getSprite(texture);
        buffer.addVertexWith2DPose(pose, x0, y0).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
        buffer.addVertexWith2DPose(pose, x0, y1).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y1).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y0).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
    }

    private void fill(VertexConsumer buffer, Matrix3x2f pose, int x0, int y0, int x1, int y1, int color)
    {
        buffer.addVertexWith2DPose(pose, x0, y0).setUv(whiteSprite.getU0(), whiteSprite.getV0()).setColor(color);
        buffer.addVertexWith2DPose(pose, x0, y1).setUv(whiteSprite.getU0(), whiteSprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y1).setUv(whiteSprite.getU1(), whiteSprite.getV1()).setColor(color);
        buffer.addVertexWith2DPose(pose, x1, y0).setUv(whiteSprite.getU1(), whiteSprite.getV0()).setColor(color);
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
