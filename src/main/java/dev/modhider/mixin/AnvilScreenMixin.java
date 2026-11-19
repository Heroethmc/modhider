package dev.modhider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.modhider.TextSanitizer;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Same probe as signs, using an item name in an anvil; the client sends the resolved name back. */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    @ModifyExpressionValue(
            method = {"onRenamed", "onSlotUpdate"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"),
            require = 2)
    private Text modhider$sanitizeName(Text original) {
        return TextSanitizer.active() ? TextSanitizer.sanitize(original) : original;
    }
}
