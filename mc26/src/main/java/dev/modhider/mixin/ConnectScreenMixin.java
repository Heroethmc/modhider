package dev.modhider.mixin;

import dev.modhider.ModHider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remember which server is being joined, so the allow list works during login, before the game knows. */
@Mixin(ConnectScreen.class)
abstract class ConnectScreenMixin {
    @Inject(method = "connect", at = @At("HEAD"))
    private void modhider$rememberServer(Minecraft minecraft, ServerAddress address, ServerData server,
                                         TransferState transferState, CallbackInfo ci) {
        ModHider.connectingTo = server != null ? server.ip : address.getHost() + ":" + address.getPort();
    }
}
