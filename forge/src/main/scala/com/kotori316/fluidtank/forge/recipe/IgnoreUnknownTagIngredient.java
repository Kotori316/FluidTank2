package com.kotori316.fluidtank.forge.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.serialization.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.ingredients.AbstractIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class IgnoreUnknownTagIngredient extends AbstractIngredient {
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
                FluidTankCommon.LOGGER.warn("[IgnoreUnknownTagIngredient] Can't get items from tag {}", tag);
                return List.of();
            }
            return manager.getTag(this.tag).stream().map(ItemStack::new).toList();
        }

    }

    private static class Serializer implements IIngredientSerializer<IgnoreUnknownTagIngredient> {
        private static final Codec<IgnoreUnknownTagIngredient> CODEC = new MapCodec.MapCodecCodec<>(new MapC());

        @Override
        public Codec<? extends IgnoreUnknownTagIngredient> codec() {
            return CODEC;
        }

        @Override
        public void write(FriendlyByteBuf buffer, IgnoreUnknownTagIngredient ingredient) {
            buffer.writeCollection(List.of(ingredient.getItems()), FriendlyByteBuf::writeItem);
        }

        @Override
        public IgnoreUnknownTagIngredient read(FriendlyByteBuf buffer) {
            var items = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readItem);
            var values = items.stream().map(ItemValue::new).toList();
            return new IgnoreUnknownTagIngredient(values);
        }
    }

    private static final class MapC extends MapCodec<IgnoreUnknownTagIngredient> {

        public static IgnoreUnknownTagIngredient parse(JsonObject json) {
            if (json.has("item") || json.has("tag")) {
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
                ItemStack stack = CraftingHelper.getItemStack(json, true, true);
                return new ItemValue(stack);
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
            if (input.values.size() == 1) {
                return encodeValue(input.values.get(0), ops, prefix);
            } else {
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
        }

        private static <T> RecordBuilder<T> encodeValue(Value value, DynamicOps<T> ops, RecordBuilder<T> builder) {
            if (value instanceof Ingredient.ItemValue itemValue) {
                var key = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(itemValue.item().getItem()));
                builder.add("item", ops.createString(key.toString()));
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
