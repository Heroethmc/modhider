package dev.modhider.mixin;

import dev.modhider.TextSanitizer;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Builds a second translation table containing only what an unmodded client would have. */
@Mixin(TranslationStorage.class)
abstract class TranslationStorageMixin {
    private static final String LOAD_ALL = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;";
    private static final String LOAD_FILES = "load(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V";

    @Inject(method = LOAD_ALL, at = @At("HEAD"))
    private static void modhider$begin(ResourceManager manager, List<String> definitions, boolean rtl,
                                       CallbackInfoReturnable<TranslationStorage> cir) {
        TextSanitizer.beginLoad();
    }

    @Inject(method = LOAD_ALL, at = @At("RETURN"))
    private static void modhider$finish(ResourceManager manager, List<String> definitions, boolean rtl,
                                        CallbackInfoReturnable<TranslationStorage> cir) {
        TextSanitizer.finishLoad();
    }

    @Inject(method = LOAD_FILES, at = @At("HEAD"))
    private static void modhider$collect(String langCode, List<Resource> resources, Map<String, String> translations,
                                         CallbackInfo ci) {
        for (Resource resource : resources) {
            if (TextSanitizer.isModPack(resource.getPack())) continue;
            try (InputStream in = resource.getInputStream()) {
                Language.load(in, (key, value) -> TextSanitizer.collect(resource.getPack(), key, value));
            } catch (Exception ignored) {
                // Vanilla logs and skips broken files; it will do so right after this.
            }
        }
    }
}
