package dev.modhider.mixin;

import dev.modhider.ModHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remember which server is being joined, so the allow list works during login, before the game knows. */
@Mixin(ConnectScreen.class)
abstract class ConnectScreenMixin {
    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
            at = @At("HEAD"))
    private void modhider$rememberServer(MinecraftClient client, ServerAddress address, ServerInfo info,
                                         CookieStorage cookies, CallbackInfo ci) {
        ModHider.connectingTo = info != null ? info.address : address.getAddress() + ":" + address.getPort();
    }
}
