package goldenshadow.wynnlimbo.client;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * The goal of this class is to dynamically adjust the custom model data and base item of the display entities so that when wynncraft decides to change them, which they seem to do quite often,
 * the models will still be correct.
 * <p>
 * Please keep in mind that this only works so long as the model file path is the display name of the item. I used axiom when building the island which automatically renames the items if you
 * use its inbuilt item picker. If you want to build your own limbo world, I would recommend you to do so in the same way.
 */
public class CustomModelFixer {

    private static final String DIR_PATH = "assets/minecraft/items/";
    private static final String MOST_LIKELY_FILE_NAME = "assets/minecraft/items/potion.json";

    private static final String CONTROL_PATTERN = "item/wynn/skin";

    private static final Pattern ITEM_PATTERN =
            Pattern.compile("item/wynn/([^\"\\\\]+)\"");

    private static final Pattern THRESHOLD_PATTERN =
            Pattern.compile("\"threshold\"\\s*:\\s*(\\d+)");

    private final Map<String, Float> customModelIds;
    private Item baseItem = null;

    public CustomModelFixer() {
        customModelIds = new HashMap<>();
    }

    public void fixCustomModel(Display.ItemDisplay entity) {
        ItemStack itemStack = entity.getItemStack();
        if (itemStack.getCustomName() == null) return;
        String itemName = itemStack.getCustomName().getString();
        itemName = itemName.replace("minecraft:", ""); //remove minecraft: prefix if it exists
        if (customModelIds.containsKey(itemName)) {
            ItemStack newItemStack = new ItemStack(baseItem);
            newItemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(getCustomModelDataValue(itemName)), List.of(), List.of(), List.of()));
            entity.setItemStack(newItemStack);
        }
    }

    private float getCustomModelDataValue(String model) {
        return customModelIds.getOrDefault(model, 0f);
    }

    public void buildFix(ZipFile zipFile) {
        try {
            // first we try with the file the is being used at time of writing this.
            // If they changed it again, we will go through all other files and find the correct one.
            InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(MOST_LIKELY_FILE_NAME));
            boolean done = fillMapIfCorrectFile(inputStream, MOST_LIKELY_FILE_NAME);
            if (done) return;

            for (ZipEntry entry : zipFile.stream().toList()) { // we check all the files in the directory and stop if we find the correct one
                if (entry.getName().startsWith(DIR_PATH)) {
                    inputStream = zipFile.getInputStream(entry);
                    done = fillMapIfCorrectFile(inputStream, entry.getName());
                    if (done) return;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to build fix for resource pack custom model ids");
        }
    }

    public void buildFixVanilla(File serverResourcePack) {
        try {
            ZipFile zipFile = new ZipFile(serverResourcePack.toString());
            buildFix(zipFile);

        } catch (IOException e) {
            throw new RuntimeException("Failed to build fix for resource pack custom model ids");
        }
    }

    public void buildFixForWynntils(Pack pack) {
        CompositePackResources resources = (CompositePackResources) pack.open();
        FilePackResources filePackResources = (FilePackResources) resources.primaryPackResources;
        ZipFile zipFile = filePackResources.zipFileAccess.getOrCreateZipFile();
        if (zipFile == null) {
            throw new RuntimeException("Failed to build fix for resource pack custom model ids");
        }
        buildFix(zipFile);
        resources.close();
    }

    private boolean fillMapIfCorrectFile(InputStream inputStream, String entryName) throws IOException {
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
        baseItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(extractItemType(entryName)));

        String input = builder.toString();
        Matcher itemMatcher = ITEM_PATTERN.matcher(input);

        while (itemMatcher.find()) {
            String fullItemPath = "item/wynn/" + itemMatcher.group(1);


            Matcher thresholdMatcher = THRESHOLD_PATTERN.matcher(input);
            thresholdMatcher.region(itemMatcher.end(), input.length());

            if (thresholdMatcher.find()) {
                int threshold = Integer.parseInt(thresholdMatcher.group(1));
                customModelIds.put(fullItemPath, (float) threshold);
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
