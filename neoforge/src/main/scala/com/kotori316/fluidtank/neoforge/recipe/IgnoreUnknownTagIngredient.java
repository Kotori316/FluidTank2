package com.kotori316.fluidtank.neoforge.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kotori316.fluidtank.DebugLogging;
import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.serialization.*;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.apache.logging.log4j.MarkerManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class IgnoreUnknownTagIngredient implements ICustomIngredient {
    public static final String NAME = "ignore_unknown_tag_ingredient";
    private static final MapCodec<IgnoreUnknownTagIngredient> CODEC = new MapC();
    public static final IngredientType<IgnoreUnknownTagIngredient> SERIALIZER = new IngredientType<>(CODEC);

    private final List<? extends Ingredient.Value> values;

    public IgnoreUnknownTagIngredient(List<? extends Ingredient.Value> values) {
        this.values = values;
    }

    public static Ingredient of(ItemLike item) {
        var ignoreUnknownTagIngredient = new IgnoreUnknownTagIngredient(List.of(new Ingredient.ItemValue(new ItemStack(item))));
        return new Ingredient(ignoreUnknownTagIngredient);
    }

    public static Ingredient of(TagKey<Item> tag) {
        var ignoreUnknownTagIngredient = new IgnoreUnknownTagIngredient(List.of(new TagValue(tag)));
        return new Ingredient(ignoreUnknownTagIngredient);
    }

    @Override
    public boolean test(ItemStack arg) {
        return getItems().anyMatch(t -> t.is(arg.getItem()));
    }

    @Override
    public Stream<ItemStack> getItems() {
        return values.stream().flatMap(v -> v.getItems().stream());
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public IngredientType<?> getType() {
        return SERIALIZER;
    }

    public List<? extends Ingredient.Value> getIngredientValues() {
        return Collections.unmodifiableList(this.values);
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class TagValue implements Ingredient.Value {
        private final TagKey<Item> tag;

        private TagValue(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemStack> getItems() {
            var manager = BuiltInRegistries.ITEM.getTag(this.tag);
            if (manager.isEmpty()) {
                DebugLogging.LOGGER().info(MarkerManager.getMarker(FluidTankCommon.MARKER_INGREDIENT.getName()), "Can't get items from tag {}", tag);
                return List.of();
            }
            return manager.stream().flatMap(HolderSet.Named::stream).map(ItemStack::new).toList();
        }

    }

    private static final class MapC extends MapCodec<IgnoreUnknownTagIngredient> {

        public static IgnoreUnknownTagIngredient parse(JsonObject json) {
            if (json.has("item") || json.has("tag")) {
                List<Ingredient.Value> valueList = List.of(getValue(json));
                return new IgnoreUnknownTagIngredient(valueList);
            } else if (json.has("values")) {
                return parse(json.getAsJsonArray("values"));
            } else {
                throw new JsonParseException("An IgnoreUnknownTagIngredient entry needs either a tag, an item or an array");
            }
        }

        public static IgnoreUnknownTagIngredient parse(JsonArray json) {
            List<Ingredient.Value> valueList = StreamSupport.stream(json.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(MapC::getValue)
                .toList();
            return new IgnoreUnknownTagIngredient(valueList);
        }

        private static Ingredient.Value getValue(JsonObject json) {
            if (json.has("item")) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(GsonHelper.getAsString(json, "item")));
                return new Ingredient.ItemValue(new ItemStack(item));
            } else if (json.has("tag")) {
                ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
                TagKey<Item> tagkey = TagKey.create(Registries.ITEM, resourcelocation);
                return new TagValue(tagkey);
            } else {
                throw new JsonParseException("An IgnoreUnknownTagIngredient entry needs either a tag or an item");
            }
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of("item", "tag", "values")
                .map(ops::createString);
        }

        @Override
        public <T> DataResult<IgnoreUnknownTagIngredient> decode(DynamicOps<T> ops, MapLike<T> input) {
            var inputAsT = ops.createMap(input.entries());
            var json = ops.convertTo(JsonOps.INSTANCE, inputAsT);
            if (json.isJsonObject()) {
                return DataResult.success(parse(json.getAsJsonObject()));
            } else {
                return DataResult.error(() -> "%s is not map. It can't be loaded as a recipe".formatted(input));
            }
        }

        @Override
        public <T> RecordBuilder<T> encode(IgnoreUnknownTagIngredient input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix.add("fabric:type", ops.createString(FluidTankCommon.modId + ":" + NAME));
            var listBuilder = ops.listBuilder();
            for (Ingredient.Value value : input.values) {
                var builder = encodeValue(value, ops, ops.mapBuilder());
                var map = builder.build(ops.empty());
                listBuilder.add(map);
            }
            var list = listBuilder.build(ops.empty());
            prefix.add("values", list);
            return prefix;
        }

        private static <T> RecordBuilder<T> encodeValue(Ingredient.Value value, DynamicOps<T> ops, RecordBuilder<T> builder) {
            switch (value) {
                case Ingredient.ItemValue itemValue -> {
                    var key = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(itemValue.item().getItem()));
                    builder.add("item", ops.createString(key.toString()));
                    return builder;
                }
                case Ingredient.TagValue tagValue -> {
                    builder.add("tag", ops.createString(tagValue.tag().location().toString()));
                    return builder;
                }
                case IgnoreUnknownTagIngredient.TagValue tagValue -> {
                    builder.add("tag", ops.createString(tagValue.tag.location().toString()));
                    return builder;
                }
                case null, default -> throw new IllegalArgumentException("Unexpected value type " + value);
            }
        }
    }
}
