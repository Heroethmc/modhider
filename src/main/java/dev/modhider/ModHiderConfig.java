package dev.modhider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Stored in .minecraft/config/modhider.json. */
public final class ModHiderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModHiderConfig instance = new ModHiderConfig();

    /** Report the client brand as "vanilla" instead of "fabric". */
    public boolean spoofBrand = true;
    /** Don't announce mod network channels and drop mod-only packets to the server. */
    public boolean hideChannels = true;
    /** Resolve server-supplied sign/anvil text exactly like an unmodded client would. */
    public boolean blockTranslationProbes = true;
    /** Servers that should see your mods normally, e.g. "play.example.com" or "1.2.3.4:25566". */
    public List<String> allowedServers = new ArrayList<>();

    public static ModHiderConfig get() {
        return instance;
    }

    public boolean isAllowed(String address) {
        String normalized = normalize(address);
        for (String allowed : allowedServers) {
            if (normalize(allowed).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String address) {
        String s = address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(":25565")) s = s.substring(0, s.length() - 6);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("modhider.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ModHiderConfig loaded = GSON.fromJson(reader, ModHiderConfig.class);
                    if (loaded != null) {
                        if (loaded.allowedServers == null) loaded.allowedServers = new ArrayList<>();
                        instance = loaded;
                    }
                }
            }
            // Write back so new options show up in old files.
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            ModHider.LOGGER.error("Could not read {}, using defaults", path, e);
        }
    }
}
