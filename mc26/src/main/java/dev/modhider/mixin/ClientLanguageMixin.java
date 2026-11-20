package dev.modhider.mixin;

import dev.modhider.TextSanitizer;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Builds a second translation table containing only what an unmodded client would have. */
@Mixin(ClientLanguage.class)
abstract class ClientLanguageMixin {
    @Inject(method = "loadFrom", at = @At("HEAD"))
    private static void modhider$begin(ResourceManager manager, List<String> languages, boolean rtl,
                                       CallbackInfoReturnable<ClientLanguage> cir) {
        TextSanitizer.beginLoad();
    }

    @Inject(method = "loadFrom", at = @At("RETURN"))
    private static void modhider$finish(ResourceManager manager, List<String> languages, boolean rtl,
                                        CallbackInfoReturnable<ClientLanguage> cir) {
        TextSanitizer.finishLoad();
    }

    @Inject(method = "appendFrom", at = @At("HEAD"))
    private static void modhider$collect(String langCode, List<Resource> resources, Map<String, String> translations,
                                         CallbackInfo ci) {
        for (Resource resource : resources) {
            if (TextSanitizer.isModPack(resource.source())) continue;
            try (InputStream in = resource.open()) {
                Language.loadFromJson(in, (key, value) -> TextSanitizer.collect(resource.source(), key, value));
            } catch (Exception ignored) {
                // Vanilla logs and skips broken files; it will do so right after this.
            }
        }
    }
}
