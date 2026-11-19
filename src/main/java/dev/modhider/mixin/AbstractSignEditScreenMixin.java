package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.TextSanitizer;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Servers open a sign editor with mod translation keys and read back what the client filled in. */
@Mixin(AbstractSignEditScreen.class)
abstract class AbstractSignEditScreenMixin {
    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;getText(Z)Lnet/minecraft/block/entity/SignText;"))
    private SignText modhider$sanitizeSign(SignText original) {
        return TextSanitizer.active() ? TextSanitizer.sanitize(original) : original;
    }
}
