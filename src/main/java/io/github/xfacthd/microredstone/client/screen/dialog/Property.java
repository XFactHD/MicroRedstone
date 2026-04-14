package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

record Property(Component label, Value value) {
    FormattedProperty format(Font font, int maxValueWidth) {
        return new FormattedProperty(label, FormattedProperty.Value.create(value, font, maxValueWidth));
    }

    sealed interface Value { }

    record ImmediateValue(Component text) implements Value { }

    record DelayedValue(Supplier<@Nullable Component> supplier, int expectedMaxWidth) implements Value { }
}
