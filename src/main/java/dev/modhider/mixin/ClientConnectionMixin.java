package dev.modhider.mixin;

import dev.modhider.ModHider;
import dev.modhider.ModHiderConfig;
import net.minecraft.network.ClientConnection;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
abstract class ClientConnectionMixin {
    private static final String SEND = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V";

    /** Swap packets that would reveal mods for what a vanilla client sends. */
    @ModifyVariable(method = SEND, at = @At("HEAD"), argsOnly = true)
    private Packet<?> modhider$replacePacket(Packet<?> packet) {
        ClientConnection self = (ClientConnection) (Object) this;
        if (!ModHider.shouldHide(self)) return packet;
        ModHiderConfig config = ModHiderConfig.get();

        if (config.spoofBrand && packet instanceof CustomPayloadC2SPacket(BrandCustomPayload brand)
                && !"vanilla".equals(brand.brand())) {
            return new CustomPayloadC2SPacket(new BrandCustomPayload("vanilla"));
        }
        // A vanilla client answers every login query with "not understood".
        if (config.hideChannels && packet instanceof LoginQueryResponseC2SPacket response && response.response() != null) {
            return new LoginQueryResponseC2SPacket(response.queryId(), null);
        }
        return packet;
    }

    /** Drop channel announcements and any mod-namespaced plugin messages. */
    @Inject(method = SEND, at = @At("HEAD"), cancellable = true)
    private void modhider$dropModPackets(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!(packet instanceof CustomPayloadC2SPacket custom)) return;
        ClientConnection self = (ClientConnection) (Object) this;
        if (!ModHiderConfig.get().hideChannels || !ModHider.shouldHide(self)) return;

        Identifier id = custom.payload().getId().id();
        boolean vanillaChannel = id.getNamespace().equals("minecraft")
                && !id.getPath().equals("register")
                && !id.getPath().equals("unregister");
        if (!vanillaChannel) {
            ci.cancel();
        }
    }
}
