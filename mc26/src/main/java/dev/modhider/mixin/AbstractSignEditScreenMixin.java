package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.TextSanitizer;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Servers open a sign editor with mod translation keys and read back what the client filled in. */
@Mixin(AbstractSignEditScreen.class)
abstract class AbstractSignEditScreenMixin {
    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;getText(Z)Lnet/minecraft/world/level/block/entity/SignText;"))
    private SignText modhider$sanitizeSign(SignText original) {
        return TextSanitizer.active() ? TextSanitizer.sanitize(original) : original;
    }
}
