package goldenshadow.wynnlimbo.mixin;

import goldenshadow.wynnlimbo.client.WynnlimboClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(at = @At("HEAD"), method = "onTitle(Lnet/minecraft/network/packet/s2c/play/TitleS2CPacket;)V", cancellable = true)
    private void onTitle(TitleS2CPacket packet, CallbackInfo ci) {

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

    @Inject(at = @At("HEAD"), method = "onSubtitle(Lnet/minecraft/network/packet/s2c/play/SubtitleS2CPacket;)V", cancellable = true)
    private void onSubtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            WynnlimboClient.subtitleText = packet.text();
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "onEntityStatusEffect(Lnet/minecraft/network/packet/s2c/play/EntityStatusEffectS2CPacket;)V", cancellable = true)
    private void onEntityEffect(EntityStatusEffectS2CPacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            if (packet.getEffectId().equals(StatusEffects.BLINDNESS)) {
                ci.cancel();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onPlayerRemove(Lnet/minecraft/network/packet/s2c/play/PlayerRemoveS2CPacket;)V", cancellable = true)
    private void onPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        if (WynnlimboClient.isInQueue()) {
            ci.cancel();
        }
    }
}
