package dev.modhider.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.modhider.ChannelFilter;
import dev.modhider.ModHider;
import dev.modhider.ModHiderConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class ConnectionMixin {
    private static final String SEND = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V";

    /** Swap packets that would reveal mods for what a vanilla client sends. */
    @ModifyVariable(method = SEND, at = @At("HEAD"), argsOnly = true)
    private Packet<?> modhider$replacePacket(Packet<?> packet) {
        Connection self = (Connection) (Object) this;
        if (!ModHider.shouldHide(self)) return packet;
        ModHiderConfig config = ModHiderConfig.get();

        if (config.spoofBrand && packet instanceof ServerboundCustomPayloadPacket(BrandPayload brand)
                && !"vanilla".equals(brand.brand())) {
            return new ServerboundCustomPayloadPacket(new BrandPayload("vanilla"));
        }
        // Channel announcements: keep only the channels of mods the server may see (e.g. voice chat).
        if (config.hideChannels && packet instanceof ServerboundCustomPayloadPacket custom
                && ChannelFilter.isRegistration(custom.payload().type().id())) {
            CustomPacketPayload filtered = ChannelFilter.filterRegistration(custom.payload());
            if (filtered != null && filtered != custom.payload()) {
                return new ServerboundCustomPayloadPacket(filtered);
            }
            return packet;
        }
        // A vanilla client answers every login query with "not understood".
        if (config.hideChannels && packet instanceof ServerboundCustomQueryAnswerPacket answer && answer.payload() != null) {
            return new ServerboundCustomQueryAnswerPacket(answer.transactionId(), null);
        }
        return packet;
    }

    /** Drop channel announcements and plugin messages of mods the server may not see. */
    @Inject(method = SEND, at = @At("HEAD"), cancellable = true)
    private void modhider$dropModPackets(CallbackInfo ci, @Local(argsOnly = true) Packet<?> packet) {
        if (!(packet instanceof ServerboundCustomPayloadPacket custom)) return;
        Connection self = (Connection) (Object) this;
        if (!ModHiderConfig.get().hideChannels || !ModHider.shouldHide(self)) return;

        Identifier id = custom.payload().type().id();
        boolean allowed = ChannelFilter.isRegistration(id)
                // Already trimmed above; drop it if no visible channel is left.
                ? ChannelFilter.filterRegistration(custom.payload()) != null
                : ChannelFilter.isVisibleChannel(id);
        if (!allowed) {
            ci.cancel();
        }
    }
}
