package io.github.xfacthd.microredstone.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.xfacthd.microredstone.client.util.duck.EditBoxExtensions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
public class MixinEditBox implements EditBoxExtensions {
    @WrapOperation(
            method = "extractWidgetRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;getMaxLength()I"
            )
    )
    private int microredstone$setCursorShape(EditBox self, Operation<Integer> op) {
        return microredstone$forceIBeamCursor() ? -1 : op.call(self);
    }
}
