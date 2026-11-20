package dev.modhider;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
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
    public static boolean shouldHide(Connection connection) {
        if (connection.isMemoryConnection() || connection.getReceiving() != PacketFlow.CLIENTBOUND) {
            return false;
        }
        return !isCurrentServerAllowed();
    }

    /** Same check, for code that runs on the render thread and has no connection handy. */
    public static boolean shouldHideNow() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && shouldHide(listener.getConnection());
    }

    private static boolean isCurrentServerAllowed() {
        ServerData entry = Minecraft.getInstance().getCurrentServer();
        String address = entry != null ? entry.ip : connectingTo;
        return address != null && ModHiderConfig.get().isAllowed(address);
    }
}
