package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.components.Button;
import org.jetbrains.annotations.Nullable;

record ButtonPair(Button okButton, @Nullable Button cancelButton)
{
    boolean isFocused()
    {
        return okButton.isFocused() || (cancelButton != null && cancelButton.isFocused());
    }
}
