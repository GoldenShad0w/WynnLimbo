package goldenshadow.wynnlimbo.mixin.wynntils;

import com.wynntils.features.ui.CustomLoadingScreenFeature;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * We disable custom loading screens completely because they break wynnlimbo, and also they are a terrible feature!
 */
@Pseudo
@Mixin(CustomLoadingScreenFeature.class)
public abstract class CustomLoadingScreenMixin {

    @Inject(at = @At("HEAD"), method = "createCustomScreen()V", cancellable = true)
    private void createScreen(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onChatMessageReceived", cancellable = true)
    private void onChatMessage(ChatMessageEvent.Match e, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onScreenOpened(Lcom/wynntils/mc/event/ScreenOpenedEvent$Pre;)V", cancellable = true)
    private void onScreenOpened(ScreenOpenedEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onWorldStateChange(Lcom/wynntils/models/worlds/event/WorldStateEvent;)V", cancellable = true)
    private void onWorldStateChange(WorldStateEvent event, CallbackInfo ci) {
        ci.cancel();
    }

}
