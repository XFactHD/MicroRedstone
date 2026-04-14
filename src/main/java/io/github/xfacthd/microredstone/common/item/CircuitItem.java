package io.github.xfacthd.microredstone.common.item;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class CircuitItem extends Item {
    public CircuitItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        stack.getOrDefault(MRContent.DC_TYPE_CIRCUIT, StoredCircuit.EMPTY).addToTooltip(context, tooltipAdder, flag, stack);
    }
}
