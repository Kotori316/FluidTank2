package com.kotori316.fluidtank.forge.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class IgnoreUnknownTagIngredient extends AbstractIngredient {
    public static final String NAME = "ignore_unknown_tag_ingredient";
    public static final IIngredientSerializer<IgnoreUnknownTagIngredient> SERIALIZER = new Serializer();

    private final List<? extends Value> values;

    public IgnoreUnknownTagIngredient(List<? extends Value> values) {
        super(values.stream());
        this.values = values;
    }

    public static IgnoreUnknownTagIngredient of(ItemLike item) {
        return new IgnoreUnknownTagIngredient(List.of(new ItemValue(new ItemStack(item))));
    }

    public static IgnoreUnknownTagIngredient of(TagKey<Item> tag) {
        return new IgnoreUnknownTagIngredient(List.of(new TagValue(tag)));
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public List<? extends Value> getValues() {
        return Collections.unmodifiableList(this.values);
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class TagValue implements Value {
        private final TagKey<Item> tag;

        private TagValue(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemStack> getItems() {
            var manager = ForgeRegistries.ITEMS.tags();
            if (manager == null) {
                FluidTankCommon.LOGGER.warn(FluidTankCommon.MARKER_INGREDIENT, "Can't get items from tag {}", tag);
                return List.of();
            }
            return manager.getTag(this.tag).stream().map(ItemStack::new).toList();
        }

    }

    @VisibleForTesting
    static Codec<IgnoreUnknownTagIngredient> typedCodec() {
        return Serializer.CODEC.codec();
    }

    private static class Serializer implements IIngredientSerializer<IgnoreUnknownTagIngredient> {
        private static final MapCodec<IgnoreUnknownTagIngredient> CODEC = new MapC();

        @Override
        public MapCodec<? extends IgnoreUnknownTagIngredient> codec() {
            return CODEC;
        }

        @Override
        public void write(RegistryFriendlyByteBuf buffer, IgnoreUnknownTagIngredient ingredient) {
            var items = ingredient.getItems();
            buffer.writeVarInt(items.length);
            for (var item : items) {
                buffer.writeJsonWithCodec(ItemStack.CODEC, item);
            }
        }

        @Override
        public IgnoreUnknownTagIngredient read(RegistryFriendlyByteBuf buffer) {
            var count = buffer.readVarInt();
            var items = new ArrayList<ItemStack>(count);
            for (int i = 0; i < count; i++) {
                var item = buffer.readJsonWithCodec(ItemStack.CODEC);
                items.add(item);
            }
            var values = items.stream().map(ItemValue::new).toList();
            return new IgnoreUnknownTagIngredient(values);
        }
    }

    private static final class MapC extends MapCodec<IgnoreUnknownTagIngredient> {

        public static IgnoreUnknownTagIngredient parse(JsonObject json) {
            if (json.has("item") || json.has("id") || json.has("tag")) {
                List<Value> valueList = List.of(getValue(json));
                return new IgnoreUnknownTagIngredient(valueList);
            } else if (json.has("values")) {
                return parse(json.getAsJsonArray("values"));
            } else {
                throw new JsonParseException("An IgnoreUnknownTagIngredient entry needs either a tag, an item or an array");
            }
        }

        public static IgnoreUnknownTagIngredient parse(JsonArray json) {
            List<Value> valueList = StreamSupport.stream(json.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(MapC::getValue)
                .toList();
            return new IgnoreUnknownTagIngredient(valueList);
        }

        private static Value getValue(JsonObject json) {
            if (json.has("item")) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(GsonHelper.getAsString(json, "item")));
                return new Ingredient.ItemValue(new ItemStack(item));
            } else if (json.has("id")) {
                ItemStack stack = ItemStack.CODEC.decode(JsonOps.INSTANCE, json).map(Pair::getFirst).getOrThrow();
                return new Ingredient.ItemValue(stack);
            } else if (json.has("tag")) {
                ResourceLocation resourcelocation = ResourceLocation.parse(GsonHelper.getAsString(json, "tag"));
                TagKey<Item> tagkey = TagKey.create(Registries.ITEM, resourcelocation);
                return new TagValue(tagkey);
            } else {
                throw new JsonParseException("An IgnoreUnknownTagIngredient entry needs either a tag or an item");
            }
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of("id", "item", "tag", "values")
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
            for (Value value : input.values) {
                var builder = encodeValue(value, ops, ops.mapBuilder());
                var map = builder.build(ops.empty());
                listBuilder.add(map);
            }
            var list = listBuilder.build(ops.empty());
            prefix.add("values", list);
            return prefix;
        }

        private static <T> RecordBuilder<T> encodeValue(Value value, DynamicOps<T> ops, RecordBuilder<T> builder) {
            if (value instanceof Ingredient.ItemValue itemValue) {
                var stack = itemValue.item();
                if (stack.getComponentsPatch().isEmpty()) {
                    ResourceLocation key = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem()));
                    builder.add("item", ops.createString(key.toString()));
                } else {
                    ItemStack.CODEC.encodeStart(ops, stack)
                        .flatMap(ops::getMapEntries)
                        .getOrThrow()
                        .accept(builder::add);
                }
                return builder;
            } else if (value instanceof Ingredient.TagValue tagValue) {
                builder.add("tag", ops.createString(tagValue.tag().location().toString()));
                return builder;
            } else if (value instanceof IgnoreUnknownTagIngredient.TagValue tagValue) {
                builder.add("tag", ops.createString(tagValue.tag.location().toString()));
                return builder;
            } else {
                throw new IllegalArgumentException("Unexpected value type " + value);
            }
        }
    }
}
