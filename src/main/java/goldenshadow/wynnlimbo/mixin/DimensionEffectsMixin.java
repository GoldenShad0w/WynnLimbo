package goldenshadow.wynnlimbo.mixin;

import goldenshadow.wynnlimbo.client.WynnlimboClient;
import net.minecraft.client.render.DimensionEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionEffects.class)
public abstract class DimensionEffectsMixin {

    @Inject(at = @At("HEAD"), method = "getSkyType()Lnet/minecraft/client/render/DimensionEffects$SkyType;", cancellable = true)
    private void getSkyType(CallbackInfoReturnable<DimensionEffects.SkyType> cir) {
        if (WynnlimboClient.isInQueue()) {
            cir.setReturnValue(DimensionEffects.SkyType.NORMAL);
        }
    }
}
