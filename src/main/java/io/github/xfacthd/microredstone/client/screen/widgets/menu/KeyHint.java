package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.Optional;

public sealed interface KeyHint
{
    String WRAPPER = Utils.translationKey("label", "context_menu.key_hint");
    ChatFormatting TEXT_COLOR = ChatFormatting.GRAY;
    KeyHint NO_HINT = new None();

    Optional<Component> format();

    record Simple(InputConstants.Key key) implements KeyHint
    {
        @Override
        public Optional<Component> format()
        {
            Component name = key.getDisplayName();
            return Optional.of(Component.translatable(WRAPPER, name).withStyle(TEXT_COLOR));
        }
    }

    record WithModifier(InputConstants.Key key, KeyModifier modifier) implements KeyHint
    {
        @Override
        public Optional<Component> format()
        {
            Component name = modifier.getCombinedName(key, key::getDisplayName);
            return Optional.of(Component.translatable(WRAPPER, name).withStyle(TEXT_COLOR));
        }
    }

    record Custom(Component text) implements KeyHint
    {
        @Override
        public Optional<Component> format()
        {
            return Optional.of(Component.translatable(WRAPPER, text).withStyle(TEXT_COLOR));
        }
    }

    record None() implements KeyHint
    {
        @Override
        public Optional<Component> format()
        {
            return Optional.empty();
        }
    }
}
