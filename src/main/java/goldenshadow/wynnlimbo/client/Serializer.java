package goldenshadow.wynnlimbo.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.network.packet.s2c.play.ChunkBiomeDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.LinkedList;
import java.util.List;

import static net.minecraft.block.entity.BlockEntityType.*;

public class Serializer {

    private final Gson gson;

    public Serializer() {
        gson = new Gson();
    }

    public JsonElement serializeChunks(int centerX, int centerZ, int chunkRadiusX, int chunkRadiusZ) {
        centerX -= 16 * chunkRadiusX;
        centerZ -= 16 * chunkRadiusZ;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            JsonObject jsonObject = new JsonObject();
            JsonArray entityArray = new JsonArray();
            JsonArray chunkArray = new JsonArray();
            Iterable<Entity> entities = client.world.getEntities();
            for (int i = 0; i < chunkRadiusX*2; i++) {
                for (int j = 0; j < chunkRadiusZ*2; j++) {

                    int p1 = centerX + i*16;
                    int p2 = centerZ+ j*16;

                    WorldChunk worldChunk = client.world.getWorldChunk(new BlockPos(p1, 0,  p2));

                    ChunkPos chunkPos = worldChunk.getPos();

                    for (Entity entity : entities) {
                        if (entity.getChunkPos().equals(chunkPos)) {
                            if (entity instanceof DisplayEntity.ItemDisplayEntity) {
                                NbtCompound compound = new NbtCompound();
                                compound = entity.writeNbt(compound);
                                entityArray.add(compound.toString());
                            }
                        }
                    }

                    chunkArray.add(serializeChunk(new LightData(worldChunk.getPos(), client.world.getLightingProvider(), null, null), new ChunkData(worldChunk), chunkPos));

                }
            }

            jsonObject.add("entities", entityArray);
            jsonObject.add("chunks", chunkArray);
            return jsonObject;
        }
        throw new RuntimeException("You need to be in a world to serialize its chunks...");
    }

    private JsonElement serializeChunk(LightData lightData, ChunkData chunkData, ChunkPos pos) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chunkX", pos.x);
        jsonObject.addProperty("chunkZ", pos.z);

        jsonObject.add("lightData", gson.toJsonTree(lightData));

        JsonObject jsonChunkObject = new JsonObject();
        StringNbtWriter nbtWriter = new StringNbtWriter();
        jsonChunkObject.addProperty("heightmap", nbtWriter.apply(chunkData.heightmap));
        JsonArray array = new JsonArray();
        for (byte b : chunkData.sectionsData) {
            array.add(b);
        }
        jsonChunkObject.add("sectionsData", array);

        JsonArray listArray = new JsonArray();
        for (ChunkData.BlockEntityData blockEntityData : chunkData.blockEntities) {
            listArray.add(serializeBlockEntityData(blockEntityData, nbtWriter));
        }

        jsonChunkObject.add("blockEntities", listArray);

        jsonObject.add("chunkData", jsonChunkObject);
        return jsonObject;
    }

    private JsonElement serializeBlockEntityData(ChunkData.BlockEntityData src, StringNbtWriter nbtWriter) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("localXz", src.localXz);
        jsonObject.addProperty("y", src.y);
        jsonObject.addProperty("type", blockEntityTypeToString(src.type));
        jsonObject.addProperty("nbt", src.nbt != null ? nbtWriter.apply(src.nbt) : "{}");

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
