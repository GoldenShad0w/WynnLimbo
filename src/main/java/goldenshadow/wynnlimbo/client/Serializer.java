package goldenshadow.wynnlimbo.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;

import static net.minecraft.world.level.block.entity.BlockEntityType.*;

public class Serializer {

    private final Gson gson;

    public Serializer() {
        gson = new Gson();
    }

    public JsonElement serializeChunks(int centerX, int centerZ, int chunkRadiusX, int chunkRadiusZ) {
        centerX -= 16 * chunkRadiusX;
        centerZ -= 16 * chunkRadiusZ;
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            JsonObject jsonObject = new JsonObject();
            JsonArray entityArray = new JsonArray();
            JsonArray chunkArray = new JsonArray();
            Iterable<Entity> entities = client.level.entitiesForRendering();
            for (int i = 0; i < chunkRadiusX*2; i++) {
                for (int j = 0; j < chunkRadiusZ*2; j++) {

                    int p1 = centerX + i*16;
                    int p2 = centerZ+ j*16;

                    LevelChunk worldChunk = client.level.getChunkAt(new BlockPos(p1, 0,  p2));

                    ChunkPos chunkPos = worldChunk.getPos();

                    for (Entity entity : entities) {
                        if (entity.chunkPosition().equals(chunkPos)) {
                            if (entity instanceof Display.ItemDisplay) {
                                TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                                entity.saveWithoutId(output);
                                entityArray.add(output.buildResult().toString());

                            }
                        }
                    }

                    chunkArray.add(serializeChunk(new ClientboundLightUpdatePacketData(worldChunk.getPos(), client.level.getLightEngine(), null, null), new ClientboundLevelChunkPacketData(worldChunk), chunkPos));

                }
            }

            jsonObject.add("entities", entityArray);
            jsonObject.add("chunks", chunkArray);
            return jsonObject;
        }
        throw new RuntimeException("You need to be in a world to serialize its chunks...");
    }

    private JsonElement serializeChunk(ClientboundLightUpdatePacketData lightData, ClientboundLevelChunkPacketData chunkData, ChunkPos pos) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chunkX", pos.x);
        jsonObject.addProperty("chunkZ", pos.z);

        jsonObject.add("lightData", serializeLightUpdatePacket(lightData));

        JsonObject jsonChunkObject = new JsonObject();

        jsonChunkObject.add("heightmaps", gson.toJsonTree(chunkData.getHeightmaps()));
        JsonArray array = new JsonArray();
        for (byte b : chunkData.buffer) {
            array.add(b);
        }
        jsonChunkObject.add("buffer", array);

        JsonArray listArray = new JsonArray();
        for (ClientboundLevelChunkPacketData.BlockEntityInfo blockEntityData : chunkData.blockEntitiesData) {
            listArray.add(serializeBlockEntityData(blockEntityData));
        }

        jsonChunkObject.add("blockEntities", listArray);

        jsonObject.add("chunkData", jsonChunkObject);
        return jsonObject;
    }

    private JsonElement serializeLightUpdatePacket(ClientboundLightUpdatePacketData lightData) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("skyYMask", serializeBitSet(lightData.getSkyYMask()));
        jsonObject.add("blockYMask", serializeBitSet(lightData.getBlockYMask()));
        jsonObject.add("emptySkyYMask", serializeBitSet(lightData.getEmptySkyYMask()));
        jsonObject.add("emptyBlockYMask", serializeBitSet(lightData.getEmptyBlockYMask()));

        JsonArray skyUpdates = new JsonArray();
        for (byte[] byteArray : lightData.getSkyUpdates()) {
            JsonArray inner = new JsonArray();
            for (byte b : byteArray) {
                inner.add(b);
            }
            skyUpdates.add(inner);
        }
        jsonObject.add("skyUpdates", skyUpdates);

        JsonArray blockUpdates = new JsonArray();
        for (byte[] byteArray : lightData.getBlockUpdates()) {
            JsonArray inner = new JsonArray();
            for (byte b : byteArray) {
                inner.add(b);
            }
            skyUpdates.add(inner);
        }
        jsonObject.add("blockUpdates", skyUpdates);

        return jsonObject;
    }

    private JsonArray serializeBitSet(BitSet bitSet) {
        JsonArray array = new JsonArray();
        for (long l : bitSet.toLongArray()) {
            array.add(l);
        }
        return array;
    }

    private JsonElement serializeBlockEntityData(ClientboundLevelChunkPacketData.BlockEntityInfo src) {
        JsonObject jsonObject = new JsonObject();

        StringTagVisitor nbtWriter = new StringTagVisitor();

        jsonObject.addProperty("localXz", src.packedXZ);
        jsonObject.addProperty("y", src.y);
        jsonObject.addProperty("type", blockEntityTypeToString(src.type));
        String nbt = "{}";
        if (src.tag != null) {
            nbtWriter.visitCompound(src.tag);
            nbt = nbtWriter.build();
        }
        jsonObject.addProperty("nbt", nbt);

        return jsonObject;
    }

    //yeah, this is very ugly, but gson was causing stack overflow errors, so we do it this way
    private String blockEntityTypeToString(BlockEntityType<?> blockEntityType) {
        if (blockEntityType.equals(FURNACE)) return "furnace";
        if (blockEntityType.equals(CHEST)) return "chest";
        if (blockEntityType.equals(TRAPPED_CHEST)) return "trapped_chest";
        if (blockEntityType.equals(ENDER_CHEST)) return "ender_chest";
        if (blockEntityType.equals(JUKEBOX)) return "jukebox";
        if (blockEntityType.equals(DISPENSER)) return "dispenser";
        if (blockEntityType.equals(DROPPER)) return "dropper";
        if (blockEntityType.equals(SIGN)) return "sign";
        if (blockEntityType.equals(HANGING_SIGN)) return "hanging_sign";
        if (blockEntityType.equals(MOB_SPAWNER)) return "mob_spawner";
        if (blockEntityType.equals(PISTON)) return "piston";
        if (blockEntityType.equals(BREWING_STAND)) return "brewing_stand";
        if (blockEntityType.equals(ENCHANTING_TABLE)) return "enchanting_table";
        if (blockEntityType.equals(END_PORTAL)) return "end_portal";
        if (blockEntityType.equals(BEACON)) return "beacon";
        if (blockEntityType.equals(SKULL)) return "skull";
        if (blockEntityType.equals(DAYLIGHT_DETECTOR)) return "daylight_detector";
        if (blockEntityType.equals(HOPPER)) return "hopper";
        if (blockEntityType.equals(COMPARATOR)) return "comparator";
        if (blockEntityType.equals(BANNER)) return "banner";
        if (blockEntityType.equals(STRUCTURE_BLOCK)) return "structure_block";
        if (blockEntityType.equals(END_GATEWAY)) return "end_gateway";
        if (blockEntityType.equals(COMMAND_BLOCK)) return "command_block";
        if (blockEntityType.equals(SHULKER_BOX)) return "shulker_box";
        if (blockEntityType.equals(BED)) return "bed";
        if (blockEntityType.equals(CONDUIT)) return "conduit";
        if (blockEntityType.equals(BARREL)) return "barrel";
        if (blockEntityType.equals(SMOKER)) return "smoker";
        if (blockEntityType.equals(BLAST_FURNACE)) return "blast_furnace";
        if (blockEntityType.equals(LECTERN)) return "lectern";
        if (blockEntityType.equals(BELL)) return "bell";
        if (blockEntityType.equals(JIGSAW)) return "jigsaw";
        if (blockEntityType.equals(CAMPFIRE)) return "campfire";
        if (blockEntityType.equals(BEEHIVE)) return "beehive";
        if (blockEntityType.equals(SCULK_SENSOR)) return "sculk_sensor";
        if (blockEntityType.equals(CALIBRATED_SCULK_SENSOR)) return "calibrated_sculk_sensor";
        if (blockEntityType.equals(SCULK_CATALYST)) return "sculk_catalyst";
        if (blockEntityType.equals(SCULK_SHRIEKER)) return "sculk_shrieker";
        if (blockEntityType.equals(CHISELED_BOOKSHELF)) return "chiseled_bookshelf";
        if (blockEntityType.equals(BRUSHABLE_BLOCK)) return "brushable_block";
        if (blockEntityType.equals(DECORATED_POT)) return "decorated_pot";
        if (blockEntityType.equals(CRAFTER)) return "crafter";
        if (blockEntityType.equals(TRIAL_SPAWNER)) return "trial_spawner";
        if (blockEntityType.equals(VAULT)) return "vault";
        return "unknown";
    }

}
