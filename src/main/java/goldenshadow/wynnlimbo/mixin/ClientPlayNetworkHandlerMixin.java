package goldenshadow.wynnlimbo.mixin;

import goldenshadow.wynnlimbo.client.WynnlimboClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(at = @At("HEAD"), method = "setTitleText", cancellable = true)
    private void onTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {

        if (packet.text().getString().matches("§aQueueing for [A-Z]{2}[1-9][0-9]?\\.*")) {
            if (!WynnlimboClient.isInQueue()) {
                WynnlimboClient.onQueueEnter();
            }
        }
        if (WynnlimboClient.isInQueue()) {
            WynnlimboClient.titleText = packet.text();
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "setSubtitleText", cancellable = true)
    private void onSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            WynnlimboClient.subtitleText = packet.text();
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "handleUpdateMobEffect", cancellable = true)
    private void onEntityEffect(ClientboundUpdateMobEffectPacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            if (packet.getEffect().equals(MobEffects.BLINDNESS)) {
                ci.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "handlePlayerInfoRemove", cancellable = true)
    private void onPlayerRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            ci.cancel();
        }
    }
}
