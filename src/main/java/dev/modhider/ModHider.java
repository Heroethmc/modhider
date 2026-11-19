package dev.modhider;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModHider implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("modhider");

    /** Address typed in the server list for the server currently being joined or played on. */
    public static volatile String connectingTo;

    @Override
    public void onInitializeClient() {
        ModHiderConfig.load();
        LOGGER.info("Mod Hider loaded ({} allow-listed server(s))", ModHiderConfig.get().allowedServers.size());
    }

    /** True when this connection goes to a real (non-singleplayer) server that is not on the allow list. */
    public static boolean shouldHide(ClientConnection connection) {
        if (connection.isLocal() || connection.getSide() != NetworkSide.CLIENTBOUND) {
            return false;
        }
        return !isCurrentServerAllowed();
    }

    /** Same check, for code that runs on the render thread and has no connection handy. */
    public static boolean shouldHideNow() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        return handler != null && shouldHide(handler.getConnection());
    }

    private static boolean isCurrentServerAllowed() {
        ServerInfo entry = MinecraftClient.getInstance().getCurrentServerEntry();
        String address = entry != null ? entry.address : connectingTo;
        return address != null && ModHiderConfig.get().isAllowed(address);
    }
}
