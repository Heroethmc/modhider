package dev.modhider.mixin;

import dev.modhider.ChannelFilter;
import dev.modhider.ModHider;
import dev.modhider.ModHiderConfig;
import net.minecraft.network.ClientConnection;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
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
    // The second argument changed type across 1.21.x, so match both forms.
    private static final String SEND_OLD = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V";
    private static final String SEND_NEW = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V";

    /** Swap packets that would reveal mods for what a vanilla client sends. */
    @ModifyVariable(method = {SEND_OLD, SEND_NEW}, at = @At("HEAD"), argsOnly = true, require = 1)
    private Packet<?> modhider$replacePacket(Packet<?> packet) {
        ClientConnection self = (ClientConnection) (Object) this;
        if (!ModHider.shouldHide(self)) return packet;
        ModHiderConfig config = ModHiderConfig.get();

        if (config.spoofBrand && packet instanceof CustomPayloadC2SPacket(BrandCustomPayload brand)
                && !"vanilla".equals(brand.brand())) {
            return new CustomPayloadC2SPacket(new BrandCustomPayload("vanilla"));
        }
        // Channel announcements: keep only the channels of mods the server may see (e.g. voice chat).
        if (config.hideChannels && packet instanceof CustomPayloadC2SPacket custom
                && ChannelFilter.isRegistration(custom.payload().getId().id())) {
            CustomPayload filtered = ChannelFilter.filterRegistration(custom.payload());
            if (filtered != null && filtered != custom.payload()) {
                return new CustomPayloadC2SPacket(filtered);
            }
            return packet;
        }
        // A vanilla client answers every login query with "not understood".
        if (config.hideChannels && packet instanceof LoginQueryResponseC2SPacket response && response.response() != null) {
            return new LoginQueryResponseC2SPacket(response.queryId(), null);
        }
        return packet;
    }

    /** Drop channel announcements and plugin messages of mods the server may not see. */
    @Inject(method = {SEND_OLD, SEND_NEW}, at = @At("HEAD"), cancellable = true, require = 1)
    private void modhider$dropModPackets(CallbackInfo ci, @Local(argsOnly = true) Packet<?> packet) {
        if (!(packet instanceof CustomPayloadC2SPacket custom)) return;
        ClientConnection self = (ClientConnection) (Object) this;
        if (!ModHiderConfig.get().hideChannels || !ModHider.shouldHide(self)) return;

        Identifier id = custom.payload().getId().id();
        boolean allowed = ChannelFilter.isRegistration(id)
                // Already trimmed above; drop it if no visible channel is left.
                ? ChannelFilter.filterRegistration(custom.payload()) != null
                : ChannelFilter.isVisibleChannel(id);
        if (!allowed) {
            ci.cancel();
        }
    }
}
