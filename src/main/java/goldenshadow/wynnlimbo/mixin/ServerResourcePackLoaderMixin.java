package goldenshadow.wynnlimbo.mixin;

import goldenshadow.wynnlimbo.client.WynnlimboClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.repository.Pack;

@Mixin(DownloadedPackSource.class)
public class ServerResourcePackLoaderMixin {

    @Inject(at = @At("TAIL"), method = "loadRequestedPacks")
    private void load(List<PackReloadConfig.IdAndPath> packs, CallbackInfoReturnable<List<Pack>> cir) {
        packs.stream()
                .map(PackReloadConfig.IdAndPath::path)
                .map(Path::toFile)
                .forEach(WynnlimboClient.customModelFixer::buildFixVanilla);
    }
}
