package io.github.xfacthd.microredstone.client.screen.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;

public class ValidatingEditBox extends EditBox
{
    private final Validator validator;
    private boolean alignRight = false;
    private boolean wasFocusedNonEmpty = false;

    public ValidatingEditBox(Font font, int width, int height, Validator validator, String defaultValue)
    {
        this(font, 0, 0, width, height, validator, null, defaultValue);
    }

    public ValidatingEditBox(Font font, int x, int y, int width, int height, Validator validator, String defaultValue)
    {
        this(font, x, y, width, height, validator, null, defaultValue);
    }

    public ValidatingEditBox(Font font, int x, int y, int width, int height, Validator validator, @Nullable ValidatingEditBox prevEditBox, String defaultValue)
    {
        super(font, x, y, width, height, prevEditBox, Component.empty());
        this.validator = validator;
        setFilter(value -> validator.validate(value) != TriState.FALSE);
        if (prevEditBox == null)
        {
            setValue(defaultValue);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        boolean focusedNonEmpty = isFocused() && !getValue().isEmpty();
        if ((focusedNonEmpty || wasFocusedNonEmpty) && !isInputValid())
        {
            graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFF0000);

            Component tooltip;
            if (isMouseOver(mouseX, mouseY) && (tooltip = validator.getInvalidValueTooltip()) != null)
            {
                graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            }
        }
        wasFocusedNonEmpty |= focusedNonEmpty;
    }

    public String getTrimmedValue()
    {
        return getValue().trim();
    }

    public boolean isInputValid()
    {
        return validator.validate(getValue()) == TriState.TRUE;
    }

    public void clearInvalidState()
    {
        wasFocusedNonEmpty = false;
    }

    public void setTextAlignment(TextAlignment alignment)
    {
        setCentered(alignment == TextAlignment.CENTER);
        alignRight = alignment == TextAlignment.RIGHT;
        updateTextPosition();
    }

    @Override
    public void setCentered(boolean centered)
    {
        super.setCentered(centered);
        if (centered)
        {
            alignRight = false;
        }
    }

    @Override
    protected void updateTextPosition()
    {
        if (alignRight)
        {
            String text = font.plainSubstrByWidth(getValue().substring(displayPos), getInnerWidth());
            textX = getX() + getWidth() - (isBordered() ? 4 : 0) - font.width(text);
            textY = isBordered() ? getY() + (height - 8) / 2 : getY();
        }
        else
        {
            super.updateTextPosition();
        }
    }

    @Override
    public boolean microredstone$forceIBeamCursor()
    {
        return alignRight;
    }

    @FunctionalInterface
    public interface Validator
    {
        TriState validate(String value);

        @Nullable
        default Component getInvalidValueTooltip()
        {
            return null;
        }
    }

    public enum TextAlignment
    {
        LEFT,
        CENTER,
        RIGHT
    }
}
