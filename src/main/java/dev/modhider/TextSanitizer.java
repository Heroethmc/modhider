package dev.modhider;

import net.minecraft.block.entity.SignText;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rewrites server-supplied text so it resolves the way an unmodded client would resolve it:
 * only translations from vanilla and from normal resource packs (including server packs) are used,
 * never translations or key bindings that come from mods.
 */
public final class TextSanitizer {
    /** A translation key that is guaranteed not to exist, so lookups always use the fallback we give. */
    private static final String MISSING_KEY = "modhider.missing." + UUID.randomUUID();

    /** What an unmodded client would show for each key (vanilla + resource packs, no mod packs). */
    private static volatile Map<String, String> visible = Map.of();
    /** Keys from the vanilla game itself; used to tell vanilla key bindings from mod ones. */
    private static volatile Set<String> vanillaKeys = Set.of();

    private static final ThreadLocal<Map<String, String>> pendingVisible = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> pendingVanilla = new ThreadLocal<>();

    // --- called from TranslationStorageMixin while languages (re)load ---

    public static void beginLoad() {
        pendingVisible.set(new HashMap<>());
        pendingVanilla.set(new HashSet<>());
    }

    public static void collect(ResourcePack pack, String key, String value) {
        Map<String, String> map = pendingVisible.get();
        if (map == null) return;
        if (pack instanceof DefaultResourcePack) {
            pendingVanilla.get().add(key);
        }
        map.put(key, value);
    }

    public static void finishLoad() {
        Map<String, String> map = pendingVisible.get();
        Set<String> vanilla = pendingVanilla.get();
        pendingVisible.remove();
        pendingVanilla.remove();
        if (map != null) {
            visible = Map.copyOf(map);
            vanillaKeys = Set.copyOf(vanilla);
        }
    }

    /** Resource packs added by mods (through Fabric API) are not something a vanilla client would have. */
    public static boolean isModPack(ResourcePack pack) {
        for (Class<?> c = pack.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals("net.fabricmc.fabric.api.resource.ModResourcePack")) return true;
            }
            if (c.getName().startsWith("net.fabricmc.fabric.impl.resource.loader.")) return true;
        }
        try {
            Object source = pack.getInfo().source();
            if (source != null && source.getClass().getName().startsWith("net.fabricmc.")) return true;
        } catch (Throwable ignored) {
            // Some packs have no info; treat them as normal resource packs.
        }
        return false;
    }

    // --- sanitizing ---

    public static Text sanitize(Text text) {
        if (text == null) return null;
        TextContent content = text.getContent();
        MutableText out;
        if (content instanceof TranslatableTextContent t) {
            out = translatable(t.getKey(), t.getFallback(), t.getArgs());
        } else if (content instanceof KeybindTextContent k) {
            // Vanilla shows the bound key for vanilla bindings and just translates the name otherwise.
            out = vanillaKeys.contains(k.getKey())
                    ? MutableText.of(content)
                    : translatable(k.getKey(), null, new Object[0]);
        } else {
            out = MutableText.of(content);
        }
        out.setStyle(text.getStyle());
        for (Text sibling : text.getSiblings()) {
            out.append(sanitize(sibling));
        }
        return out;
    }

    private static MutableText translatable(String key, String fallback, Object[] args) {
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = args[i] instanceof Text t ? sanitize(t) : args[i];
        }
        // Same lookup vanilla does, but against the mod-free translation table.
        String value = visible.getOrDefault(key, fallback != null ? fallback : key);
        return Text.translatableWithFallback(MISSING_KEY, value, newArgs);
    }

    public static SignText sanitize(SignText sign) {
        Text[] messages = sign.getMessages(false).clone();
        Text[] filtered = sign.getMessages(true).clone();
        for (int i = 0; i < messages.length; i++) messages[i] = sanitize(messages[i]);
        for (int i = 0; i < filtered.length; i++) filtered[i] = sanitize(filtered[i]);
        return new SignText(messages, filtered, sign.getColor(), sign.isGlowing());
    }

    public static boolean active() {
        return ModHiderConfig.get().blockTranslationProbes && ModHider.shouldHideNow();
    }

    private TextSanitizer() {}
}
