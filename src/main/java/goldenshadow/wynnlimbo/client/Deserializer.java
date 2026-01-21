package goldenshadow.wynnlimbo.client;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class Deserializer {

    private final Gson gson;

    public Deserializer() {
        gson = new Gson();
    }

    public Result deserialize(JsonElement json) throws ReflectiveOperationException, CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();
        Set<ClientboundLevelChunkWithLightPacket> chunks = deserializeChunks(jsonObject.getAsJsonArray("chunks"));
        Set<CompoundTag> entities = deserializeEntities(jsonObject.getAsJsonArray("entities"));

        return new Result(chunks, entities);
    }

    private Set<ClientboundLevelChunkWithLightPacket> deserializeChunks(JsonArray chunkArray) throws ReflectiveOperationException, CommandSyntaxException {

        Set<ClientboundLevelChunkWithLightPacket> set = new HashSet<>();

        for (JsonElement jsonElement : chunkArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);

            ClientboundLevelChunkWithLightPacket packet = (ClientboundLevelChunkWithLightPacket) unsafe.allocateInstance(ClientboundLevelChunkWithLightPacket.class);


            Field chunkXField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "x" : "field_12236"); //x if yarn mapping, else field_12236
            chunkXField.setAccessible(true);
            chunkXField.set(packet, jsonObject.get("chunkX").getAsInt());

            Field chunkZField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "z" : "field_12235"); //z if yarn mapping, else field_12235
            chunkZField.setAccessible(true);
            chunkZField.set(packet, jsonObject.get("chunkZ").getAsInt());

            Field chunkDataField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "chunkData" : "field_34870"); //chunkData if yarn mapping, else field_34870
            chunkDataField.setAccessible(true);
            chunkDataField.set(packet, deserializeChunkData(jsonObject.get("chunkData")));

            Field lightDataField = packet.getClass().getDeclaredField(runningFromIntelliJ() ? "lightData" : "field_34871"); //lightData if yarn mapping, else field_34871
            lightDataField.setAccessible(true);
            lightDataField.set(packet, deserializeLightDataPacket(jsonObject.get("lightData")));

            set.add(packet);
        }
        return set;
    }


    private ClientboundLightUpdatePacketData deserializeLightDataPacket(JsonElement json)  throws ReflectiveOperationException {
        JsonObject jsonObject = json.getAsJsonObject();
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        ClientboundLightUpdatePacketData lightData = (ClientboundLightUpdatePacketData) unsafe.allocateInstance(ClientboundLightUpdatePacketData.class);

        Field skyYMaskField = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "skyYMask" : "field_34873");
        skyYMaskField.setAccessible(true);
        skyYMaskField.set(lightData, deserializeBitSet(jsonObject.getAsJsonArray("skyYMask")));

        Field blockYMaskField = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "blockYMask" : "field_34874");
        blockYMaskField.setAccessible(true);
        blockYMaskField.set(lightData, deserializeBitSet(jsonObject.getAsJsonArray("blockYMask")));

        Field emptySkyYMaskField = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "emptySkyYMask" : "field_34875");
        emptySkyYMaskField.setAccessible(true);
        emptySkyYMaskField.set(lightData, deserializeBitSet(jsonObject.getAsJsonArray("emptySkyYMask")));

        Field emptyBlockYMaskField = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "emptyBlockYMask" : "field_34876");
        emptyBlockYMaskField.setAccessible(true);
        emptyBlockYMaskField.set(lightData, deserializeBitSet(jsonObject.getAsJsonArray("emptyBlockYMask")));

        Field skyUpdates = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "skyUpdates" : "field_34877");
        skyUpdates.setAccessible(true);
        skyUpdates.set(lightData, deserializeByteArrayList(jsonObject.getAsJsonArray("skyUpdates")));

        Field blockUpdates = lightData.getClass().getDeclaredField(runningFromIntelliJ() ? "blockUpdates" : "field_34878");
        blockUpdates.setAccessible(true);
        blockUpdates.set(lightData, deserializeByteArrayList(jsonObject.getAsJsonArray("blockUpdates")));

        return lightData;
    }

    private List<byte[]> deserializeByteArrayList(JsonArray jsonArray) {
        List<byte[]> list = new ArrayList<>();
        for (JsonElement e : jsonArray) {
            JsonArray inner = e.getAsJsonArray();
            byte[] array = new byte[inner.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = inner.get(i).getAsByte();
            }
            list.add(array);
        }
        return list;
    }

    private BitSet deserializeBitSet(JsonArray jsonArray) {
        long[] array = new long[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            array[i] = jsonArray.get(i).getAsLong();
        }
        return BitSet.valueOf(array);
    }

    private ClientboundLevelChunkPacketData deserializeChunkData(JsonElement json) throws ReflectiveOperationException, CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        ClientboundLevelChunkPacketData chunkData = (ClientboundLevelChunkPacketData) unsafe.allocateInstance(ClientboundLevelChunkPacketData.class);

        for (Field field : chunkData.getClass().getDeclaredFields()) {
            if (field.getType().isAssignableFrom(Map.class)) {
                field.setAccessible(true);
                field.set(chunkData, gson.fromJson(jsonObject.get("heightmaps"), new TypeToken<Map<Heightmap.Types, long[]>>(){}.getType()));
                continue;
            }
            if (field.getType().isAssignableFrom(byte[].class)) {
                JsonArray array = jsonObject.getAsJsonArray("buffer");
                byte[] byteArray = new byte[array.size()];
                for (int i = 0; i < byteArray.length; i++) {
                    byteArray[i] = array.get(i).getAsByte();
                }
                field.setAccessible(true);
                field.set(chunkData, byteArray);
                continue;
            }
            if (field.getType().isAssignableFrom(List.class)) {
                List<ClientboundLevelChunkPacketData.BlockEntityInfo> list = new LinkedList<>();
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

    private ClientboundLevelChunkPacketData.BlockEntityInfo deserializeBlockEntityData(JsonElement json) throws CommandSyntaxException {
        JsonObject jsonObject = json.getAsJsonObject();
        CompoundTag nbtTag = TagParser.parseCompoundFully(jsonObject.get("nbt").getAsString());

        return new ClientboundLevelChunkPacketData.BlockEntityInfo(jsonObject.get("localXz").getAsInt(), jsonObject.get("y").getAsInt(), stringToBlockEntityType(jsonObject.get("type").getAsString()), nbtTag);
    }

    private Set<CompoundTag> deserializeEntities(JsonArray entityArray) throws CommandSyntaxException {

        Set<CompoundTag> set = new HashSet<>();

        for (JsonElement element : entityArray) {
            CompoundTag compound = TagParser.parseCompoundFully(element.getAsString());
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

    public record Result(Set<ClientboundLevelChunkWithLightPacket> chunkData, Set<CompoundTag> entities) {}

}
