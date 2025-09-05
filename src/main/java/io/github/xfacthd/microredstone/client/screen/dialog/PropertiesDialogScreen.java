package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class PropertiesDialogScreen extends DialogScreen
{
    private final List<Property> properties;
    private final List<FormattedProperty> propertyLines = new ArrayList<>();
    private int maxLabelWidth = 0;
    private int propValueX;

    PropertiesDialogScreen(Component title, List<Component> messageLines, List<Property> properties, Runnable okCallback)
    {
        super(Type.PROPERTIES, title, messageLines, okCallback, () -> {});
        this.properties = properties;
    }

    @Override
    protected boolean computeContent()
    {
        boolean addPadding = super.computeContent();

        propertyLines.clear();
        maxLabelWidth = 0;

        for (Property property : properties)
        {
            if (addPadding)
            {
                imageHeight += PADDING;
            }

            maxLabelWidth = Math.max(maxLabelWidth, font.width(property.label()));
            imageHeight += font.lineHeight;

            addPadding = false;
        }
        int maxValueWidth = MAX_TEXT_WIDTH - PADDING - maxLabelWidth;
        for (Property property : properties)
        {
            FormattedProperty formatted = property.format(font, maxValueWidth);
            propertyLines.add(formatted);
            formatted.value().text(); // Try resolving potential DelayedValues
            int lineWidth = maxLabelWidth + PADDING * 3 + formatted.value().valueWidth();
            imageWidth = Math.max(imageWidth, lineWidth);
        }

        return addPadding;
    }

    @Override
    protected void finalizeContent()
    {
        super.finalizeContent();

        propValueX = leftPos + PADDING * 2 + maxLabelWidth;
    }

    @Override
    protected int renderContent(GuiGraphics graphics, int contentX, int mouseX, int mouseY, float partialTick)
    {
        int contentY = super.renderContent(graphics, contentX, mouseX, mouseY, partialTick);
        for (FormattedProperty line : propertyLines)
        {
            graphics.drawString(font, line.label(), contentX, contentY, 0xFF404040, false);
            FormattedProperty.Value value = line.value();
            graphics.drawString(font, value.text(), propValueX, contentY, 0xFF404040, false);
            Component tooltip = value.tooltip();
            if (tooltip != null && mouseY >= contentY && mouseY < contentY + font.lineHeight && mouseX >= propValueX && mouseX < propValueX + value.valueWidth())
            {
                graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            }
            contentY += font.lineHeight;
        }
        return contentY;
    }
}
