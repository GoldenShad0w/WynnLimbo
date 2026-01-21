package goldenshadow.wynnlimbo.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class WynnlimboClient implements ClientModInitializer {

    private static boolean inQueue;
    private static Set<ClientboundLevelChunkWithLightPacket> chunkPackets;
    private static Set<CompoundTag> entityNbt;
    public static Component titleText;
    public static Component subtitleText;

    public static CustomModelFixer customModelFixer;


    @Override
    public void onInitializeClient() {

        inQueue = false;
        titleText = Component.literal("");
        subtitleText = Component.literal("");
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
                        client.player.setPosRaw(31, 118, 28);
                    }
                    client.player.displayClientMessage(titleText.copy().append(Component.literal(" §7| ")).append(subtitleText), true);
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
                            context.getSource().sendFeedback(Component.literal("Finished serializing chunks!").withStyle(ChatFormatting.GREEN));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return 0;
                    })
            )))));
        }));

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("load_limbo").executes(context -> {
                    onQueueEnter();
                    return 0;
                })
            );
        }));

         */

    }

    public static void onQueueEnter() {
        try {

            Minecraft client = Minecraft.getInstance();

            if (client.player == null || client.level == null || client.getConnection() == null) {
                return;
            }
            System.out.println("sending chunk packets!");

            for (ClientboundLevelChunkWithLightPacket packet : chunkPackets) {
                System.out.println(packet);
                Connection.genericsFtw(packet, client.getConnection());
                System.out.println("grr");
                //set biome to plains
                LevelChunk worldChunk = client.level.getChunkAt(new ChunkPos(packet.getX(), packet.getZ()).getWorldPosition());
                Registry<Biome> biomeRegistry = client.level.registryAccess().lookupOrThrow(Registries.BIOME);
                Optional<Holder.Reference<Biome>> biome = biomeRegistry.get(Identifier.parse("plains"));
                assert biome.isPresent();
                worldChunk.fillBiomesFromNoise((x, y, z, noise) -> biome.get(), Climate.empty());
            }

            entityNbt.forEach(nbtCompound -> {
                Display.ItemDisplay entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, client.level);
                ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, HolderLookup.Provider.create(client.level.registryAccess().listRegistries()), nbtCompound);
                entity.load(input);
                //this is needed as the world was made in a version where negative y coordinates exist.
                //they however do not in wynn, so everything is shifted up by 64 blocks
                customModelFixer.fixCustomModel(entity);
                entity.setPos(entity.getX(), entity.getY() + 64, entity.getZ());
                client.level.addEntity(entity);
            });
            client.player.setPosRaw(31, 118, 28);
            client.level.setTimeFromServer(client.level.getGameTime(), 6000, true);
            client.player.removeEffect(MobEffects.BLINDNESS);
            client.player.getAttributes().resetBaseValue(Attributes.MOVEMENT_SPEED); // <- reset to default because otherwise entering queue from a high walkspeed class will make you be really fast
            client.player.setReducedDebugInfo(false);
            client.gui.setTitle(Component.empty());
            client.gui.setSubtitle(Component.empty());

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
