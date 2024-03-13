package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.fabric.recipe.IgnoreUnknownTagIngredientFabric;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BeforeMC {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @BeforeAll
    public static void setup() {
        if (!initialized.getAndSet(true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            PlatformConfigAccess.setInstance(ConfigData::DEFAULT);
            CustomIngredientSerializer.register(IgnoreUnknownTagIngredientFabric.SERIALIZER);
        }
    }

    public static void assertEqualHelper(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, "Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static void assertEqualStack(ItemStack expected, ItemStack actual) {
        Assertions.assertTrue(ItemStack.matches(expected, actual),
            "Expected: %s(%s), Actual: %s(%s)".formatted(expected, expected.getComponents(), actual, actual.getComponents()));
    }

}
