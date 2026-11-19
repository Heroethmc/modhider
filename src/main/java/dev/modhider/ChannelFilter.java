package dev.modhider;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/** Decides which plugin channels the server may see. */
public final class ChannelFilter {
    /** True for channels of vanilla or of a mod on the visibleMods list. */
    public static boolean isVisibleChannel(Identifier id) {
        String ns = id.getNamespace();
        if (ns.equals("minecraft")) {
            return !isRegistration(id);
        }
        return ModHiderConfig.get().isVisibleMod(ns);
    }

    public static boolean isRegistration(Identifier id) {
        return id.getNamespace().equals("minecraft")
                && (id.getPath().equals("register") || id.getPath().equals("unregister"));
    }

    /**
     * Fabric API announces channels with a record (id, List of channel ids). Returns a copy that lists only
     * visible mods' channels, or null when nothing would be left (the packet should then be dropped).
     * Done reflectively so this mod doesn't need Fabric API installed.
     */
    public static CustomPayload filterRegistration(CustomPayload payload) {
        if (!(payload instanceof Record)) return null;
        try {
            RecordComponent[] components = payload.getClass().getRecordComponents();
            Object[] values = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            boolean changed = false;
            boolean foundChannels = false;
            for (int i = 0; i < components.length; i++) {
                components[i].getAccessor().setAccessible(true);
                values[i] = components[i].getAccessor().invoke(payload);
                types[i] = components[i].getType();
                if (values[i] instanceof List<?> list && components[i].getName().equals("channels")) {
                    foundChannels = true;
                    List<Identifier> kept = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof Identifier channel && isVisibleChannel(channel)) kept.add(channel);
                    }
                    if (kept.isEmpty()) return null;
                    changed = kept.size() != list.size();
                    values[i] = List.copyOf(kept);
                }
            }
            if (!foundChannels) return null;
            if (!changed) return payload;
            Constructor<?> ctor = payload.getClass().getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return (CustomPayload) ctor.newInstance(values);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModHider.LOGGER.warn("Could not filter channel registration, dropping it", e);
            return null;
        }
    }

    private ChannelFilter() {}
}
