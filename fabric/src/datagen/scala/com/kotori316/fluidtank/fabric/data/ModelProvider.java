package com.kotori316.fluidtank.fabric.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.fabric.FluidTank;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TankPos;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.model.*;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class ModelProvider extends FabricModelProvider {
    private final ModelTemplate tankBlockParent;
    private final ModelTemplate tankItemParent;
    private final PropertyDispatch tankPosDispatch;

    public ModelProvider(FabricDataOutput output) {
        super(output);
        this.tankBlockParent = new ModelTemplate(
            Optional.of(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/tanks")),
            Optional.empty(),
            TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.PARTICLE
        );
        this.tankItemParent = new ModelTemplate(
            Optional.of(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "item/tanks")),
            Optional.empty(),
            TextureSlot.TOP, TextureSlot.SIDE
        );
        this.tankPosDispatch = PropertyDispatch.property(TankPos.TANK_POS_PROPERTY)
            .generate(tankPos -> Variant.variant());
    }

    private ResourceLocation blockTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/" + name);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        catBlock(blockStateModelGenerator);
        tankBase(blockStateModelGenerator);
        FluidTank.TANK_MAP.values().forEach(t -> tank(t, blockStateModelGenerator));
        Stream.of(FluidTank.BLOCK_CREATIVE_TANK, FluidTank.BLOCK_VOID_TANK).forEach(t -> tank(t, blockStateModelGenerator));
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        reservoirBase(itemModelGenerator);
        FluidTank.RESERVOIR_MAP.values().forEach(r -> reservoir(r, itemModelGenerator));
    }

    void catBlock(BlockModelGenerators generators) {
        var textureMapping = new TextureMapping().put(TextureSlot.SIDE, blockTexture("cat_side")).put(TextureSlot.TOP, blockTexture("cat_front"));
        var blockCat = FluidTank.BLOCK_CAT;
        var modelLocation = ModelTemplates.CUBE_TOP.create(blockCat, textureMapping, generators.modelOutput);
        generators.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(blockCat, modelLocation)
            .with(generators.createColumnWithFacing()));
        generators.delegateItemModel(blockCat, modelLocation);
    }

    void tankBase(BlockModelGenerators generators) {
        var gson = new Gson();
        try (var inputStream = ModelProvider.class.getResourceAsStream("/template/block/tanks.json");
             var reader = new InputStreamReader(Objects.requireNonNull(inputStream))) {
            var modelJson = gson.fromJson(reader, JsonObject.class);
            generators.modelOutput.accept(
                // No need to add extension
                ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/tanks"),
                () -> modelJson
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (var inputStream = ModelProvider.class.getResourceAsStream("/template/item/tanks.json");
             var reader = new InputStreamReader(Objects.requireNonNull(inputStream))) {
            var modelJson = gson.fromJson(reader, JsonObject.class);
            generators.modelOutput.accept(
                // No need to add extension
                ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "item/tanks"),
                () -> modelJson
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void tank(BlockTank blockTank, BlockModelGenerators generators) {
        var tier = blockTank.tier();
        var textureMapping = new TextureMapping()
            .put(TextureSlot.SIDE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
            .put(TextureSlot.PARTICLE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
            .put(TextureSlot.TOP, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "2"));
        var modelLocation = this.tankBlockParent.create(ModelLocationUtils.getModelLocation(blockTank), textureMapping, generators.modelOutput, (resourceLocation, map) -> {
            var jsonObject = this.tankBlockParent.createBaseTemplate(resourceLocation, map);
            // required in forge
            jsonObject.addProperty("render_type", "minecraft:cutout");
            return jsonObject;
        });
        generators.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(blockTank, modelLocation)
                .with(this.tankPosDispatch)
        );
        this.tankItemParent.create(ModelLocationUtils.getModelLocation(blockTank.asItem()), textureMapping, generators.modelOutput);
    }

    void reservoirBase(ItemModelGenerators generators) {
        var gson = new Gson();
        try (var inputStream = ModelProvider.class.getResourceAsStream("/template/item/reservoirs.json");
             var reader = new InputStreamReader(Objects.requireNonNull(inputStream))) {
            var modelJson = gson.fromJson(reader, JsonObject.class);
            generators.output.accept(
                // No need to add extension
                ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "item/reservoirs"),
                () -> modelJson
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void reservoir(ItemReservoir reservoir, ItemModelGenerators generators) {
        generators.output.accept(ModelLocationUtils.getModelLocation(reservoir), new DelegatedModel(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "item/reservoirs")));
    }
}
