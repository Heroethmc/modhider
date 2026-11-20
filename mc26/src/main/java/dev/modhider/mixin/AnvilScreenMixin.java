package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.TextSanitizer;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Same probe as signs, using an item name in an anvil; the client sends the resolved name back. */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    @ModifyExpressionValue(
            method = {"onNameChanged", "slotChanged"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"),
            require = 2)
    private Component modhider$sanitizeName(Component original) {
        return TextSanitizer.active() ? TextSanitizer.sanitize(original) : original;
    }
}
