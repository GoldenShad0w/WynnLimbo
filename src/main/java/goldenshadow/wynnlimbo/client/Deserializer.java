package goldenshadow.wynnlimbo.client;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Deserializer {

    private final Gson gson;

    public Deserializer() {
        gson = new Gson();
    }

    public Result deserialize(JsonElement json) throws ReflectiveOperationException, CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();
        Set<ChunkDataS2CPacket> chunks = deserializeChunks(jsonObject.getAsJsonArray("chunks"));
        Set<NbtCompound> entities = deserializeEntities(jsonObject.getAsJsonArray("entities"));

        return new Result(chunks, entities);
    }

    private Set<ChunkDataS2CPacket> deserializeChunks(JsonArray chunkArray) throws ReflectiveOperationException, CommandSyntaxException {

        Set<ChunkDataS2CPacket> set = new HashSet<>();

        for (JsonElement jsonElement : chunkArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);

            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) unsafe.allocateInstance(ChunkDataS2CPacket.class);


            Field chunkXField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "chunkX" : "field_12236"); //chunkX if yarn mapping, else field_12236
            chunkXField.setAccessible(true);
            chunkXField.set(packet, jsonObject.get("chunkX").getAsInt());

            Field chunkZField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "chunkZ" : "field_12235"); //chunkZ if yarn mapping, else field_12235
            chunkZField.setAccessible(true);
            chunkZField.set(packet, jsonObject.get("chunkZ").getAsInt());

            Field chunkDataField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "chunkData" : "field_34870"); //chunkData if yarn mapping, else field_34870
            chunkDataField.setAccessible(true);
            chunkDataField.set(packet, deserializeChunkData(jsonObject.get("chunkData")));

            Field lightDataField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "lightData" : "field_34871"); //lightData if yarn mapping, else field_34871
            lightDataField.setAccessible(true);
            lightDataField.set(packet, gson.fromJson(jsonObject.get("lightData"), new TypeToken<LightData>(){}.getType()));

            set.add(packet);
        }
        return set;
    }


    private ChunkData deserializeChunkData(JsonElement json) throws ReflectiveOperationException, CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        ChunkData chunkData = (ChunkData) unsafe.allocateInstance(ChunkData.class);

        for (Field field : chunkData.getClass().getDeclaredFields()) {
            if (field.getType().isAssignableFrom(NbtCompound.class)) {
                field.setAccessible(true);
                StringReader stringReader = new StringReader(jsonObject.get("heightmap").toString());
                String jsonString = stringReader.readString();
                field.set(chunkData, StringNbtReader.parse(jsonString));
                continue;
            }
            if (field.getType().isAssignableFrom(byte[].class)) {
                JsonArray array = jsonObject.getAsJsonArray("sectionsData");
                byte[] byteArray = new byte[array.size()];
                for (int i = 0; i < byteArray.length; i++) {
                    byteArray[i] = array.get(i).getAsByte();
                }
                field.setAccessible(true);
                field.set(chunkData, byteArray);
                continue;
            }
            if (field.getType().isAssignableFrom(List.class)) {
                List<ChunkData.BlockEntityData> list = new LinkedList<>();
                JsonArray array = jsonObject.getAsJsonArray("blockEntities");

                for (int i = 0; i < array.size(); i++) {
                    list.add(deserializeBlockEntityData(array.get(i)));
                }

                field.setAccessible(true);
                field.set(chunkData, list);
            }
        }

        return chunkData;
    }

    private ChunkData.BlockEntityData deserializeBlockEntityData(JsonElement json) throws CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();
        StringNbtReader nbtReader = new StringNbtReader(new StringReader(jsonObject.get("nbt").getAsString()));

        return new ChunkData.BlockEntityData(jsonObject.get("localXz").getAsInt(), jsonObject.get("y").getAsInt(), stringToBlockEntityType(jsonObject.get("type").getAsString()), nbtReader.parseCompound());
    }

    private Set<NbtCompound> deserializeEntities(JsonArray entityArray) throws CommandSyntaxException {

        Set<NbtCompound> set = new HashSet<>();

        for (JsonElement element : entityArray) {
            StringNbtReader nbtReader = new StringNbtReader(new StringReader(element.getAsString()));
            NbtCompound compound = nbtReader.parseCompound();
            set.add(compound);
        }
        return set;
    }

    //yeah, this is very ugly, but gson was causing stack overflow errors, so we do it this way
    private BlockEntityType<?> stringToBlockEntityType(String type) {
        return switch (type) {
            case "furnace" -> BlockEntityType.FURNACE;
            case "chest" -> BlockEntityType.CHEST;
            case "trapped_chest" -> BlockEntityType.TRAPPED_CHEST;
            case "ender_chest" -> BlockEntityType.ENDER_CHEST;
            case "jukebox" -> BlockEntityType.JUKEBOX;
            case "dispenser" -> BlockEntityType.DISPENSER;
            case "dropper" -> BlockEntityType.DROPPER;
            case "sign" -> BlockEntityType.SIGN;
            case "hanging_sign" -> BlockEntityType.HANGING_SIGN;
            case "mob_spawner" -> BlockEntityType.MOB_SPAWNER;
            case "piston" -> BlockEntityType.PISTON;
            case "brewing_stand" -> BlockEntityType.BREWING_STAND;
            case "enchanting_table" -> BlockEntityType.ENCHANTING_TABLE;
            case "end_portal" -> BlockEntityType.END_PORTAL;
            case "beacon" -> BlockEntityType.BEACON;
            case "skull" -> BlockEntityType.SKULL;
            case "daylight_detector" -> BlockEntityType.DAYLIGHT_DETECTOR;
            case "hopper" -> BlockEntityType.HOPPER;
            case "comparator" -> BlockEntityType.COMPARATOR;
            case "banner" -> BlockEntityType.BANNER;
            case "structure_block" -> BlockEntityType.STRUCTURE_BLOCK;
            case "end_gateway" -> BlockEntityType.END_GATEWAY;
            case "command_block" -> BlockEntityType.COMMAND_BLOCK;
            case "shulker_box" -> BlockEntityType.SHULKER_BOX;
            case "bed" -> BlockEntityType.BED;
            case "conduit" -> BlockEntityType.CONDUIT;
            case "barrel" -> BlockEntityType.BARREL;
            case "smoker" -> BlockEntityType.SMOKER;
            case "blast_furnace" -> BlockEntityType.BLAST_FURNACE;
            case "lectern" -> BlockEntityType.LECTERN;
            case "bell" -> BlockEntityType.BELL;
            case "jigsaw" -> BlockEntityType.JIGSAW;
            case "campfire" -> BlockEntityType.CAMPFIRE;
            case "beehive" -> BlockEntityType.BEEHIVE;
            case "sculk_sensor" -> BlockEntityType.SCULK_SENSOR;
            case "calibrated_sculk_sensor" -> BlockEntityType.CALIBRATED_SCULK_SENSOR;
            case "sculk_catalyst" -> BlockEntityType.SCULK_CATALYST;
            case "sculk_shrieker" -> BlockEntityType.SCULK_SHRIEKER;
            case "chiseled_bookshelf" -> BlockEntityType.CHISELED_BOOKSHELF;
            case "brushable_block" -> BlockEntityType.BRUSHABLE_BLOCK;
            case "decorated_pot" -> BlockEntityType.DECORATED_POT;
            case "crafter" -> BlockEntityType.CRAFTER;
            case "trial_spawner" -> BlockEntityType.TRIAL_SPAWNER;
            case "vault" -> BlockEntityType.VAULT;
            default -> throw new RuntimeException("invalid block entity type!");
        };
    }

    private boolean runningFromIntelliJ() {
        return false;
    }

    public record Result(Set<ChunkDataS2CPacket> chunkData, Set<NbtCompound> entities) {}

}
