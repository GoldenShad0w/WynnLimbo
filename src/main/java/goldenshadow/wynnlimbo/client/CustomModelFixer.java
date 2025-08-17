package goldenshadow.wynnlimbo.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The goal of this class is to dynamically adjust the custom model data and base item of the display entities so that when wynncraft decides to change them, which they seem to do quite often,
 * the models will still be correct.
 * <p>
 * Please keep in mind that this only works so long as the model file path is the display name of the item. I used axiom when building the island which automatically renames the items if you
 * use its inbuilt item picker. If you want to build your own limbo world, I would recommend you to do so in the same way.
 */
public class CustomModelFixer {

    private static final String DIR_PATH = "assets/minecraft/models/item/";
    private static final String MOST_LIKELY_FILE_NAME = "assets/minecraft/models/item/potion.json";

    private static final String CONTROL_PATTERN = "item/wynn/skin";

    private final Map<String, Float> customModelIds;
    private Item baseItem = null;

    public CustomModelFixer() {
        customModelIds = new HashMap<>();
    }

    public void fixCustomModel(DisplayEntity.ItemDisplayEntity entity) {
        ItemStack itemStack = entity.getItemStack();
        if (itemStack.getCustomName() == null) return;
        String itemName = itemStack.getCustomName().getString();
        itemName = itemName.replace("minecraft:", ""); //remove minecraft: prefix if it exists
        if (customModelIds.containsKey(itemName)) {
            ItemStack newItemStack = new ItemStack(baseItem);
            newItemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(getCustomModelDataValue(itemName)), List.of(), List.of(), List.of()));
            entity.setItemStack(newItemStack);
        }
    }

    private float getCustomModelDataValue(String model) {
        return customModelIds.getOrDefault(model, 0f);
    }

    public void buildFix(File serverResourcePack) {
        try {
            ZipFile zipFile = new ZipFile(serverResourcePack.toString());

            // first we try with the file the is being used at time of writing this.
            // If they changed it again, we will go through all other files and find the correct one.
            boolean done = fillMapIfCorrectFile(zipFile.getEntry(MOST_LIKELY_FILE_NAME), zipFile);
            if (done) return;

            for (ZipEntry entry : zipFile.stream().toList()) { // we check all the files in the directory and stop if find the correct one
                if (entry.getName().startsWith(DIR_PATH)) {
                    done = fillMapIfCorrectFile(entry, zipFile);
                    if (done) return;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to build fix for resource pack custom model ids");
        }
    }

    private boolean fillMapIfCorrectFile(ZipEntry entry, ZipFile zipFile) throws IOException {
        InputStream inputStream = zipFile.getInputStream(entry);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);

        boolean correctFile = false;
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
            if (line.contains(CONTROL_PATTERN)) correctFile = true;
        }

        if (!correctFile) return false;

        baseItem = Registries.ITEM.get(Identifier.of(extractItemType(entry.getName())));

        JsonElement json = JsonParser.parseString(builder.toString());
        JsonArray array = json.getAsJsonObject().getAsJsonArray("overrides");
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("predicate")) {
                JsonObject predicate = obj.getAsJsonObject("predicate");
                if (predicate.has("custom_model_data")) {
                    int id = predicate.get("custom_model_data").getAsInt();
                    customModelIds.put(obj.get("model").getAsString(), (float) id);
                }
            }
        }
        return true;
    }

    private String extractItemType(String string) {
        int lastSlash = string.lastIndexOf("/") + 1;
        int lastDot = string.lastIndexOf(".");
        return string.substring(lastSlash, lastDot);
    }
}
