package io.github.xfacthd.microredstone.client.screen.microchip;

import io.github.xfacthd.microredstone.client.util.ScreenUtils;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.menu.MicrochipMenu;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MicrochipScreen extends AbstractContainerScreen<MicrochipMenu>
{
    private static final Identifier BACKGROUND = Utils.rl("textures/gui/microchip.png");

    private final ItemStack icStack = new ItemStack(MRContent.ITEM_INTEGRATED_CIRCUIT);

    public MicrochipScreen(MicrochipMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY)
    {
        super.renderSlot(graphics, slot, mouseX, mouseY);
        if (slot == menu.getCircuitSlot() && !slot.hasItem())
        {
            ScreenUtils.renderTransparentFakeItem(graphics, icStack, slot.x, slot.y);
        }
    }
}
