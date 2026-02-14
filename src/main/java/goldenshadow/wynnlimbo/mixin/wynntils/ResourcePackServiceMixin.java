package goldenshadow.wynnlimbo.mixin.wynntils;


import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.services.resourcepack.ResourcePackService;
import goldenshadow.wynnlimbo.client.WynnlimboClient;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ResourcePackService.class)
public abstract class ResourcePackServiceMixin {


    @Shadow
    private boolean serverHasResourcePack;

    @Shadow
    protected abstract Pack getPreloadedPack();

    @Inject(at = @At("HEAD"), method = "onServerResourcePackLoad(Lcom/wynntils/mc/event/ServerResourcePackEvent$Load;)V")
    private void onServerResourcePackLoad(ServerResourcePackEvent.Load event, CallbackInfo ci) {
        if (!serverHasResourcePack) {
            Pack pack = getPreloadedPack();
            WynnlimboClient.customModelFixer.buildFixForWynntils(pack);
        }
    }
}
