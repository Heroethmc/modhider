package dev.modhider;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.world.level.block.entity.SignText;

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

    // --- called from ClientLanguageMixin while languages (re)load ---

    public static void beginLoad() {
        pendingVisible.set(new HashMap<>());
        pendingVanilla.set(new HashSet<>());
    }

    public static void collect(PackResources pack, String key, String value) {
        Map<String, String> map = pendingVisible.get();
        if (map == null) return;
        if (pack instanceof VanillaPackResources) {
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
    public static boolean isModPack(PackResources pack) {
        for (Class<?> c = pack.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().startsWith("net.fabricmc.")) return true;
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().startsWith("net.fabricmc.")) return true;
            }
        }
        try {
            Object source = pack.location().source();
            if (source != null && source.getClass().getName().startsWith("net.fabricmc.")) return true;
        } catch (Throwable ignored) {
            // Some packs have no location info; treat them as normal resource packs.
        }
        return false;
    }

    // --- sanitizing ---

    public static Component sanitize(Component text) {
        if (text == null) return null;
        ComponentContents content = text.getContents();
        MutableComponent out;
        if (content instanceof TranslatableContents t) {
            out = translatable(t.getKey(), t.getFallback(), t.getArgs());
        } else if (content instanceof KeybindContents k) {
            // Vanilla shows the bound key for vanilla bindings and just translates the name otherwise.
            out = vanillaKeys.contains(k.getName())
                    ? MutableComponent.create(content)
                    : translatable(k.getName(), null, new Object[0]);
        } else {
            out = MutableComponent.create(content);
        }
        out.setStyle(text.getStyle());
        for (Component sibling : text.getSiblings()) {
            out.append(sanitize(sibling));
        }
        return out;
    }

    private static MutableComponent translatable(String key, String fallback, Object[] args) {
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = args[i] instanceof Component c ? sanitize(c) : args[i];
        }
        // Same lookup vanilla does, but against the mod-free translation table.
        String value = visible.getOrDefault(key, fallback != null ? fallback : key);
        return Component.translatableWithFallback(MISSING_KEY, value, newArgs);
    }

    public static SignText sanitize(SignText sign) {
        Component[] messages = sign.getMessages(false).clone();
        Component[] filtered = sign.getMessages(true).clone();
        for (int i = 0; i < messages.length; i++) messages[i] = sanitize(messages[i]);
        for (int i = 0; i < filtered.length; i++) filtered[i] = sanitize(filtered[i]);
        return new SignText(messages, filtered, sign.getColor(), sign.hasGlowingText());
    }

    public static boolean active() {
        return ModHiderConfig.get().blockTranslationProbes && ModHider.shouldHideNow();
    }

    private TextSanitizer() {}
}
