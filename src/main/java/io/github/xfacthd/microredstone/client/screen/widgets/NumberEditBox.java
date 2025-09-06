package io.github.xfacthd.microredstone.client.screen.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;
import java.util.regex.Pattern;

public final class NumberEditBox extends ValidatingEditBox
{
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?[0-9]*)");

    public NumberEditBox(Font font, int width, int height, Validator validator, boolean supportNegative, int defaultValue)
    {
        this(font, 0, 0, width, height, supportNegative, validator, defaultValue);
    }

    public NumberEditBox(Font font, int x, int y, int width, int height, boolean supportNegative, Validator validator, int defaultValue)
    {
        this(font, x, y, width, height, validator, supportNegative, null, defaultValue);
    }

    public NumberEditBox(Font font, int x, int y, int width, int height, Validator validator, boolean supportNegative, @Nullable NumberEditBox prevEditBox, int defaultValue)
    {
        super(font, x, y, width, height, toValidator(validator, supportNegative), prevEditBox, Integer.toString(defaultValue));
    }

    public int getIntValue()
    {
        try
        {
            return Integer.parseInt(getValue());
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    public void setIntValue(int value)
    {
        setValue(Integer.toString(value));
    }

    private static ValidatingEditBox.Validator toValidator(Validator validator, boolean supportNegative)
    {
        return new ValidatingEditBox.Validator()
        {
            @Override
            public TriState validate(String value)
            {
                if (value.isEmpty()) return TriState.DEFAULT;
                if (!NUMBER_PATTERN.matcher(value).matches()) return TriState.FALSE;
                if (value.length() == 1 && value.charAt(0) == '-' && !supportNegative)
                {
                    return TriState.FALSE;
                }
                try
                {
                    boolean result = validator.test(Integer.parseInt(value));
                    return result ? TriState.TRUE : TriState.DEFAULT;
                }
                catch (NumberFormatException e)
                {
                    return TriState.FALSE;
                }
            }

            @Override
            @Nullable
            public Component getInvalidValueTooltip()
            {
                return validator.getInvalidValueTooltip();
            }
        };
    }

    @FunctionalInterface
    public interface Validator extends IntPredicate
    {
        @Nullable
        default Component getInvalidValueTooltip()
        {
            return null;
        }
    }
}
