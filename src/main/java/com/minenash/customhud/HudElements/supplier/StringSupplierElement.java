package com.minenash.customhud.HudElements.supplier;

import com.minenash.customhud.ComplexData;
import com.minenash.customhud.HudElements.HudElement;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.text.WordUtils;

import java.util.function.Supplier;

public class StringSupplierElement implements HudElement {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static Entity cameraEntity() { return client.getCameraEntity(); }
    private static BlockPos blockPos() { return client.getCameraEntity().getBlockPos(); }

    public static final Supplier<String> VERSION = () -> SharedConstants.getGameVersion().getName();
    public static final Supplier<String> CLIENT_VERSION = client::getGameVersion;
    public static final Supplier<String> MODDED_NAME = ClientBrandRetriever::getClientModName;
    public static final Supplier<String> SERVER_BRAND = () -> client.player.getServerBrand();
    public static final Supplier<String> ADDRESS = () -> client.getCurrentServerEntry().address;

    public static final Supplier<String> DIMENSION = () -> WordUtils.capitalize(client.world.getRegistryKey().getValue().getPath().replace("_"," "));
    public static final Supplier<String> DIMENSION_ID = () -> client.world.getRegistryKey().getValue().toString();
    public static final Supplier<String> BIOME = () -> WordUtils.capitalize(client.world.getBiome(blockPos()).getKey().get().getValue().getPath().replace("_", " "));
    public static final Supplier<String> BIOME_ID = () -> client.world.getBiome(blockPos()).getKey().get().getValue().toString();

    public static final Supplier<String> TARGET_ENTITY = () -> client.targetedEntity == null ? null : I18n.translate(client.targetedEntity.getType().getTranslationKey());
    public static final Supplier<String> TARGET_ENTITY_ID = () -> client.targetedEntity == null ? null : Registry.ENTITY_TYPE.getId(client.targetedEntity.getType()).toString();

    public static final Supplier<String> TIME_AM_PM = () -> ComplexData.timeOfDay < 12000 ? "am" : "pm";

    public static final Supplier<String> FACING = () -> cameraEntity().getHorizontalFacing().getName();
    public static final Supplier<String> FACING_TOWARDS_XZ = () ->
            cameraEntity().getHorizontalFacing() == Direction.EAST || cameraEntity().getHorizontalFacing() == Direction.WEST ? "X" : "Z";

    public static final Supplier<String> JAVA_VERSION = () -> System.getProperty("java.version");

    //TODO: Item Name
    //public static final Supplier<String> ITEM_NAME = () -> client.player.getMainHandStack().getItem().getName().;



    private final Supplier<String> supplier;

    public StringSupplierElement(Supplier<String> supplier) {
        this.supplier = supplier;
    }

    @Override
    public String getString() {
        return sanitize(supplier, "-");
    }

    @Override
    public Number getNumber() {
        try {
            return supplier.get().length();
        }
        catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean getBoolean() {
        return getNumber().intValue() > 0;
    }
}
