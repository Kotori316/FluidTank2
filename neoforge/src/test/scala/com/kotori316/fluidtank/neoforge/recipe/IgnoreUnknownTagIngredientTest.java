package com.kotori316.fluidtank.neoforge.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.neoforge.BeforeMC;
import com.mojang.serialization.JsonOps;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class IgnoreUnknownTagIngredientTest extends BeforeMC {
    static void assertIngredientEqual(Ingredient expected, Ingredient actual) {
        if (Objects.equals(expected, actual))
            return;
        var e = expected.getItems();
        var a = actual.getItems();
        if (e.length != a.length) {
            fail("The length of ingredient doesn't match");
        }
        for (int i = 0; i < e.length; i++) {
            var e1 = e[i];
            var a1 = a[i];
            assertTrue(ItemStack.matches(e1, a1), "Item at %d".formatted(i));
        }
    }

    @Nested
    class VanillaCheck {
        @Test
        void vanillaItemIngredient() {
            // language=json
            var json = """
                {
                  "item": "minecraft:apple"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertDoesNotThrow(() -> result.getOrThrow().getFirst());
            var expected = Ingredient.of(Items.APPLE);

            assertIngredientEqual(expected, ingredient);
        }

        @Test
        void vanillaTagIngredient() {
            // language=json
            var json = """
                {
                  "tag": "minecraft:logs"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertDoesNotThrow(() -> result.getOrThrow().getFirst());
            var expected = Ingredient.of(ItemTags.LOGS);

            assertIngredientEqual(expected, ingredient);
        }

        @Test
        void vanillaUnknownTag() {
            // language=json
            var json = """
                {
                  "tag": "c:logs"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertDoesNotThrow(() -> result.getOrThrow().getFirst());
            assertNotEquals(0, ingredient.getItems().length);
        }

        @Test
        void compound() {
            // language=json
            var json = """
                [
                  {
                    "item": "minecraft:apple"
                  },
                  {
                    "item": "minecraft:stone"
                  },
                  {
                    "tag": "c:logs"
                  }
                ]
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parseArray(json));
            var ingredient = assertDoesNotThrow(() -> result.getOrThrow().getFirst());
            // including error item
            assertEquals(3, ingredient.getItems().length);
        }
    }

    @Nested
    @Disabled("Archtectury doesn't support NeoForge JUnit support system")
    class DeserializeTest {

        @Test
        void customItemIngredient() {
            // language=json
            var json = """
                {
                  "type": "fluidtank:ignore_unknown_tag_ingredient",
                  "item": "minecraft:apple"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertInstanceOf(Ingredient.class, assertDoesNotThrow(() -> result.getOrThrow().getFirst()));
            var expected = IgnoreUnknownTagIngredient.of(Items.APPLE);

            assertIngredientEqual(expected, ingredient);
        }

        @Test
        void customTagIngredient() {
            // language=json
            var json = """
                {
                  "type": "fluidtank:ignore_unknown_tag_ingredient",
                  "tag": "minecraft:logs"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertInstanceOf(Ingredient.class, assertDoesNotThrow(() -> result.getOrThrow().getFirst()));
            var expected = IgnoreUnknownTagIngredient.of(ItemTags.LOGS);

            assertIngredientEqual(expected, ingredient);
        }

        @Test
        void customUnknownTag() {
            // language=json
            var json = """
                {
                  "type": "fluidtank:ignore_unknown_tag_ingredient",
                  "tag": "c:logs"
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertInstanceOf(Ingredient.class, assertDoesNotThrow(() -> result.getOrThrow().getFirst()));
            assertEquals(0, ingredient.getItems().length);
        }

        @Test
        void values1() {
            // language=json
            var json = """
                {
                  "type": "fluidtank:ignore_unknown_tag_ingredient",
                  "values": [
                    {"item": "minecraft:apple"},
                    {"tag": "minecraft:logs"}
                  ]
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertInstanceOf(Ingredient.class, assertDoesNotThrow(() -> result.getOrThrow().getFirst()));
            var custom = assertInstanceOf(IgnoreUnknownTagIngredient.class, ingredient.getCustomIngredient());
            assertEquals(2, custom.getIngredientValues().size());
        }
    }

    @Nested
    @Disabled("Archtectury doesn't support NeoForge JUnit support system")
    class SerializeTest {
        @Test
        void singleItemInternal() {
            var ingredient = IgnoreUnknownTagIngredient.of(Items.APPLE);
            var codec = IgnoreUnknownTagIngredient.SERIALIZER.codec().codec();
            var encoded = codec.encodeStart(JsonOps.INSTANCE, (IgnoreUnknownTagIngredient) ingredient.getCustomIngredient());
            var json = assertDoesNotThrow(() -> encoded.getOrThrow());
            assertTrue(json.isJsonObject());

            var expected = singleObj("item", "minecraft:apple");
            expected.addProperty("fabric:type", FluidTankCommon.modId + ":ignore_unknown_tag_ingredient");
            assertEquals(expected, json);
        }

        @Test
        void singleItemWithType() {
            var ingredient = IgnoreUnknownTagIngredient.of(Items.APPLE);
            var encoded = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient);
            var json = assertDoesNotThrow(() -> encoded.getOrThrow());
            assertTrue(json.isJsonObject());

            var expected = singleObj("item", "minecraft:apple");
            expected.addProperty("type", FluidTankCommon.modId + ":ignore_unknown_tag_ingredient");
            expected.addProperty("fabric:type", FluidTankCommon.modId + ":ignore_unknown_tag_ingredient");
            assertEquals(expected, json);
        }

        @Test
        void singleTag() {
            var ingredient = IgnoreUnknownTagIngredient.of(ItemTags.LOGS);
            var encoded = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient);
            var json = assertDoesNotThrow(() -> encoded.getOrThrow());
            assertTrue(json.isJsonObject());

            var expected = singleObj("tag", "minecraft:logs");
            expected.addProperty("type", FluidTankCommon.modId + ":ignore_unknown_tag_ingredient");
            expected.addProperty("fabric:type", FluidTankCommon.modId + ":ignore_unknown_tag_ingredient");
            assertEquals(expected, json);
        }
    }

    static JsonObject singleObj(String key, String value) {
        var parent = new JsonObject();
        var arr = new JsonArray();
        var obj = new JsonObject();
        obj.addProperty(key, value);
        arr.add(obj);
        parent.add("values", arr);
        return parent;
    }
}
