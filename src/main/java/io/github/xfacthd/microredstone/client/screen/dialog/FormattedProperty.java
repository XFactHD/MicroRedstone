package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Supplier;

record FormattedProperty(Component label, Value value)
{
    sealed interface Value
    {
        static Value create(Property.Value value, Font font, int maxValueWidth)
        {
            return switch (value)
            {
                case Property.ImmediateValue immediate -> ImmediateValue.create(font, immediate.text(), maxValueWidth);
                case Property.DelayedValue delayed -> new DelayedValue(delayed.supplier(), font, Math.min(maxValueWidth, delayed.expectedMaxWidth()));
            };
        }

        FormattedCharSequence text();

        @Nullable
        Component tooltip();

        int valueWidth();
    }

    private record ImmediateValue(FormattedCharSequence text, @Nullable Component tooltip, int valueWidth) implements Value
    {
        static ImmediateValue create(Font font, Component value, int maxValueWidth)
        {
            Component tooltip = null;
            FormattedText valueCut = font.ellipsize(value, maxValueWidth);
            if (valueCut != value)
            {
                tooltip = value;
            }
            FormattedCharSequence text = Language.getInstance().getVisualOrder(valueCut);
            return new ImmediateValue(text, tooltip, font.width(valueCut));
        }
    }

    private static final class DelayedValue implements Value
    {
        private static final FormattedCharSequence[] THROBBER = Arrays.stream(new String[] { "|", "/", "-", "\\" })
                .map(string -> FormattedCharSequence.forward(string, Style.EMPTY))
                .toArray(FormattedCharSequence[]::new);

        private final Supplier<@Nullable Component> source;
        private final Font font;
        private final int maxValueWidth;
        @Nullable
        FormattedProperty.ImmediateValue resolved = null;

        private DelayedValue(Supplier<@Nullable Component> source, Font font, int maxValueWidth)
        {
            this.source = source;
            this.font = font;
            this.maxValueWidth = maxValueWidth;
        }

        @Override
        public FormattedCharSequence text()
        {
            if (resolved == null)
            {
                Component value = source.get();
                if (value != null)
                {
                    resolved = ImmediateValue.create(font, value, maxValueWidth);
                }
            }
            if (resolved != null)
            {
                return resolved.text;
            }

            // TODO - 1.21.9: try replace this with a throbber sprite
            long time = System.currentTimeMillis() / 200;
            int idx = (int) (time % THROBBER.length);
            return THROBBER[idx];
        }

        @Override
        @Nullable
        public Component tooltip()
        {
            return resolved != null ? resolved.tooltip : null;
        }

        @Override
        public int valueWidth()
        {
            return resolved != null ? resolved.valueWidth : maxValueWidth;
        }
    }
}
