package goldenshadow.wynnlimbo.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.core.time.MutableInstant;

import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class WynnlimboClient implements ClientModInitializer {

    private static boolean inQueue;
    private static Set<ChunkDataS2CPacket> chunkPackets;
    private static Set<NbtCompound> entityNbt;
    public static Text titleText;
    public static Text subtitleText;

    public static CustomModelFixer customModelFixer;


    @Override
    public void onInitializeClient() {

        inQueue = false;
        titleText = Text.literal("");
        subtitleText = Text.literal("");
        customModelFixer = new CustomModelFixer();

        FabricLoader.getInstance().getModContainer("wynnlimbo").flatMap(wynnlimbo -> wynnlimbo.findPath("assets/wynnlimbo/data.json")).ifPresentOrElse(path -> {
            try {
                JsonElement element = JsonParser.parseString(Files.readString(path));
                Deserializer deserializer = new Deserializer();
                Deserializer.Result result = deserializer.deserialize(element);
                chunkPackets = result.chunkData();
                entityNbt = result.entities();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            throw new RuntimeException("Data file could not be found!");
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isInQueue()) {
                if (client.player != null) {
                    if (client.player.getY() < 64) {
                        client.player.setPos(31, 118, 28);
                    }
                    client.player.sendMessage(titleText.copy().append(Text.literal(" §7| ")).append(subtitleText), true);
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> onQueueExit());

        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, packetSender, minecraftClient) -> {
            onQueueExit();
        });


        //Code used to serialize chunks to file
        //Usage Info: You need (!!!) to serialize more chunks than the build encompasses. At least 2-3 buffer chunks
        //are needed, because otherwise minecraft will keep trying to unload the chunks on the edge
        //The current island is serialized with the command: /serializechunks 31 28 8 8
        /*
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("serializechunks")
                    .then(ClientCommandManager.argument("centerX", integer())
                    .then(ClientCommandManager.argument("centerZ", integer())
                    .then(ClientCommandManager.argument("chunkRadiusX", integer())
                    .then(ClientCommandManager.argument("chunkRadiusZ", integer())
                    .executes(context -> {

                        int centerX = getInteger(context, "centerX");
                        int centerZ = getInteger(context, "centerZ");
                        int chunkRadiusX = getInteger(context, "chunkRadiusX");
                        int chunkRadiusZ = getInteger(context, "chunkRadiusZ");

                        try {
                            File configFolder = FabricLoader.getInstance().getConfigDir().resolve("wynnlimbo").toFile();
                            File file = new File(configFolder.getAbsolutePath() + File.separator + "data.json");
                            configFolder.mkdir();
                            file.createNewFile();


                            Writer writer = new FileWriter(file, false);
                            Serializer serializer = new Serializer();
                            writer.write(serializer.serializeChunks(centerX, centerZ, chunkRadiusX, chunkRadiusZ).toString());
                            writer.flush();
                            writer.close();
                            context.getSource().sendFeedback(Text.literal("Finished serializing chunks!").formatted(Formatting.GREEN));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return 0;
                    })
            )))));
        }));
         */

    }

    public static void onQueueEnter() {
        try {

            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                return;
            }


            for (ChunkDataS2CPacket packet : chunkPackets) {
                ClientConnection.handlePacket(packet, client.getNetworkHandler().getConnection().getPacketListener());

                //set biome to plains
                WorldChunk worldChunk = client.world.getWorldChunk(new ChunkPos(packet.getChunkX(), packet.getChunkZ()).getStartPos());
                Registry<Biome> biomeRegistry = client.world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
                Optional<RegistryEntry.Reference<Biome>> biome = biomeRegistry.getEntry(Identifier.of("plains"));
                assert biome.isPresent();
                worldChunk.populateBiomes((x, y, z, noise) -> biome.get(), null);
            }

            entityNbt.forEach(nbtCompound -> {
                DisplayEntity.ItemDisplayEntity entity = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, client.world);
                entity.readNbt(nbtCompound);
                //this is needed as the world was made in a version where negative y coordinates exist.
                //they however do not in wynn, so everything is shifted up by 64 blocks
                customModelFixer.fixCustomModel(entity);
                entity.setPosition(entity.getX(), entity.getY() + 64, entity.getZ());
                client.world.addEntity(entity);
            });
            client.player.setPos(31, 118, 28);
            client.world.setTime(client.world.getTime(), 6000, true);
            client.player.removeStatusEffect(StatusEffects.BLINDNESS);
            client.player.getAttributes().resetToBaseValue(EntityAttributes.MOVEMENT_SPEED); // <- reset to default because otherwise entering queue from a high walkspeed class will make you be really fast
            client.player.setReducedDebugInfo(false);
            client.inGameHud.setTitle(Text.empty());
            client.inGameHud.setSubtitle(Text.empty());

            inQueue = true;

        } catch (Exception ignored) {}
    }

    public static void onQueueExit() {
        inQueue = false;
    }

    public static boolean isInQueue() {
        return inQueue;
    }
}
