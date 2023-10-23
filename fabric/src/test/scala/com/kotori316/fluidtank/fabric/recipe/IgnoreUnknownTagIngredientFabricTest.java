package com.kotori316.fluidtank.fabric.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kotori316.fluidtank.fabric.BeforeMC;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IgnoreUnknownTagIngredientFabricTest extends BeforeMC {
    @Nested
    class SerializeTest {
        @Test
        void serializeBasicItem() {
            var i = new IgnoreUnknownTagIngredientFabric(Ingredient.of(Items.APPLE));
            var result = assertDoesNotThrow(() ->
                Util.getOrThrow(Ingredient.CODEC_NONEMPTY
                    .encodeStart(JsonOps.INSTANCE, i.toVanilla()), IllegalStateException::new)
            );
            assertTrue(result.isJsonObject());
            var expected = new JsonObject();
            expected.addProperty("fabric:type", "fluidtank:ignore_unknown_tag_ingredient");
            expected.add("values", singleObject("item", "minecraft:apple"));
            assertEquals(expected, result);
        }

        @Test
        void serializeBasicTag() {
            var i = new IgnoreUnknownTagIngredientFabric(Ingredient.of(ItemTags.LOGS));
            var result = assertDoesNotThrow(() ->
                Util.getOrThrow(Ingredient.CODEC_NONEMPTY
                    .encodeStart(JsonOps.INSTANCE, i.toVanilla()), IllegalStateException::new)
            );
            assertTrue(result.isJsonObject());
            var expected = new JsonObject();
            expected.addProperty("fabric:type", "fluidtank:ignore_unknown_tag_ingredient");
            expected.add("values", singleObject("tag", "minecraft:logs"));
            assertEquals(expected, result);
        }
    }

    @Nested
    class DeserializeTest {
        @Test
        void values() {
            // language=json
            var json = """
                {
                  "fabric:type": "fluidtank:ignore_unknown_tag_ingredient",
                  "values": [
                    {"item": "minecraft:apple"},
                    {"tag": "minecraft:logs"}
                  ]
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertDoesNotThrow(() -> Util.getOrThrow(result.map(Pair::getFirst), IllegalStateException::new));
            var custom = ingredient.getCustomIngredient();
            assertInstanceOf(IgnoreUnknownTagIngredientFabric.class, custom);
        }

        @Test
        void considerNotExistTag() {
            // language=json
            var json = """
                {
                  "fabric:type": "fluidtank:ignore_unknown_tag_ingredient",
                  "values": [
                    {"item": "minecraft:apple"},
                    {"tag": "forge:ingots"}
                  ]
                }
                """;
            var result = Ingredient.CODEC.decode(JsonOps.INSTANCE, GsonHelper.parse(json));
            var ingredient = assertDoesNotThrow(() -> Util.getOrThrow(result.map(Pair::getFirst), IllegalStateException::new));
            assertEquals(1, ingredient.getItems().length);
        }
    }

    static JsonArray singleObject(String key, String value) {
        var js = new JsonObject();
        js.addProperty(key, value);
        var arr = new JsonArray();
        arr.add(js);
        return arr;
    }
}
