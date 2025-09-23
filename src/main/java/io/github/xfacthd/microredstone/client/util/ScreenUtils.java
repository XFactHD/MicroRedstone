package io.github.xfacthd.microredstone.client.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ScreenUtils
{
    public static void renderTransparentFakeItem(GuiGraphics graphics, ItemStack stack, int x, int y)
    {
        graphics.renderFakeItem(stack, x, y, 0);
        graphics.fill(x, y, x + 16, y + 16, 0x80888888);
    }

    public static boolean isInventoryKey(int keyCode, int scanCode)
    {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        return Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key);
    }

    private ScreenUtils() { }
}
