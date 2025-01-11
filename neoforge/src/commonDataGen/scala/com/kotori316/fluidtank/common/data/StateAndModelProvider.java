package com.kotori316.fluidtank.common.data;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.neoforge.render.FluidRenderHelperNeoForge;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TankPos;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.Variant;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.ElementBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class StateAndModelProvider extends ModelProvider {

    static final String ITEM_TANK_BASE = "item/tanks";
    static final String ITEM_GAS_TANK_BASE = "item/gas_item_tank";
    static final String ITEM_RESERVOIR_BASE = "item/reservoirs";

    StateAndModelProvider(PackOutput output) {
        super(output, FluidTankCommon.modId);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of();
    }

    private ResourceLocation blockTexture(String name) {
        return modLocation("block/" + name);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        FluidTankCommon.LOGGER.info("Generating state and model");
        catBlock(blockModels);
        // sourceBlock();
        var tankTemplates = tankBase(blockModels, itemModels);
        FluidTank.TANK_MAP.values().stream().map(Supplier::get).forEach(b -> tank(blockModels, itemModels, b, tankTemplates));
        Stream.of(FluidTank.BLOCK_CREATIVE_TANK, FluidTank.BLOCK_VOID_TANK).map(Supplier::get).forEach(b -> tank(blockModels, itemModels, b, tankTemplates));
        // StreamConverters.asJavaSeqStream(ModObjects.gasTanks()).forEach(this::gasTank);
        // pipeBase();
        // pipe(ModObjects.blockFluidPipe(), "fluid_pipe");
        // pipe(ModObjects.blockItemPipe(), "item_pipe");
        reservoirBase(itemModels);
        FluidTank.RESERVOIR_MAP.values().stream().map(Supplier::get).forEach(i -> reservoir(itemModels, i));
    }

    void catBlock(BlockModelGenerators blockModels) {
        var location = TexturedModel.CUBE_TOP.updateTexture(t -> t.put(TextureSlot.SIDE, blockTexture("cat_side")).put(TextureSlot.TOP, blockTexture("cat_front")))
            .create(FluidTank.BLOCK_CAT.get(), blockModels.modelOutput);
        blockModels.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(FluidTank.BLOCK_CAT.get(), location)
                .with(blockModels.createColumnWithFacing())
        );
        blockModels.registerSimpleItemModel(FluidTank.BLOCK_CAT.get(), location);
    }

    /*void sourceBlock() {
        var builder = getVariantBuilder(ModObjects.blockSource());
        builder.setModels(builder.partialState().with(FluidSourceBlock.CHEAT_MODE(), false),
            new ConfiguredModel(models().cubeColumn(FluidSourceBlock.NAME(), blockTexture("fluid_source"), blockTexture("white"))));
        builder.setModels(builder.partialState().with(FluidSourceBlock.CHEAT_MODE(), true),
            new ConfiguredModel(models().cubeColumn(FluidSourceBlock.NAME() + "_inf", blockTexture("fluid_source_inf"), blockTexture("pink"))));
        ResourceLocation cheat = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "source_cheat");
        itemModels().getBuilder(ModObjects.blockSource().registryName().getPath())
            .override()
            .predicate(cheat, 0)
            .model(models().getExistingFile(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/" + FluidSourceBlock.NAME())))
            .end()
            .override()
            .predicate(cheat, 1)
            .model(models().getExistingFile(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/" + FluidSourceBlock.NAME() + "_inf")))
            .end();
    }*/

    TankModelTemplates tankBase(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Consumer<ElementBuilder> elementBuilderConsumer = b -> b
            .from(2.0f, 0.0f, 2.0f)
            .to(14.0f, 16.0f, 14.0f)
            .allFaces((direction, faceBuilder) -> {
                if (direction.getAxis() == Direction.Axis.Y) {
                    faceBuilder.texture(TextureSlot.TOP).uvs(0.0f, 0.0f, 12.0f, 12.0f);
                } else {
                    faceBuilder.texture(TextureSlot.SIDE).uvs(0.0f, 0.0f, 12.0f, 16.0f);
                }
            });
        var tankBlockTemplate = ExtendedModelTemplateBuilder.builder()
            .parent(mcLocation("block/block"))
            .element(elementBuilderConsumer)
            .build();
        var blockModelLocation = tankBlockTemplate.create(modLocation("block/tanks"), new TextureMapping(), blockModels.modelOutput);

        var itemTemplate = ExtendedModelTemplateBuilder.builder()
            .parent(blockModelLocation)
            .guiLight(UnbakedModel.GuiLight.SIDE)
            .transform(ItemDisplayContext.GUI, b -> b.scale(0.625f).translation(0, 0, 0).rotation(30, 225, 0))
            .transform(ItemDisplayContext.GROUND, b -> b.scale(0.25f).translation(0, 3, 0).rotation(0, 0, 0))
            .transform(ItemDisplayContext.FIXED, b -> b.scale(0.5f).translation(0, 0, 0).rotation(0, 0, 0))
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, b -> b.scale(0.375f).translation(0, 2.5f, 0).rotation(75, 45, 0))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, b -> b.scale(0.4f).translation(0, 0, 0).rotation(0, 45, 0))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, b -> b.scale(0.4f).translation(0, 0, 0).rotation(0, 225, 0))
            .ambientOcclusion(false)
            .build();
        var itemModelLocation = itemTemplate.create(modLocation(ITEM_TANK_BASE), new TextureMapping(), itemModels.modelOutput);

        var gasTankBlockTemplate = ExtendedModelTemplateBuilder.builder()
            .parent(mcLocation("block/block"))
            .ambientOcclusion(false)
            .element(elementBuilderConsumer)
            .build();

        var gasBlockModelLocation = gasTankBlockTemplate.create(modLocation(ITEM_GAS_TANK_BASE), new TextureMapping(), itemModels.modelOutput);

        return new TankModelTemplates(blockModelLocation, itemModelLocation, gasBlockModelLocation);
    }

    void tank(BlockModelGenerators blockModels, ItemModelGenerators itemModels, BlockTank blockTank, TankModelTemplates templates) {
        var tier = blockTank.tier();
        var tankPosProperty = PropertyDispatch.property(TankPos.TANK_POS_PROPERTY)
            .generate(tankPos -> Variant.variant());
        var blockModel = ExtendedModelTemplateBuilder.builder()
            .parent(templates.tankBlock())
            .renderType(renderTypeName(RenderType.cutout()))
            .build();

        var blockModelLocation = blockModel.create(ModelLocationUtils.getModelLocation(blockTank),
            new TextureMapping()
                .putForced(TextureSlot.PARTICLE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
                .putForced(TextureSlot.SIDE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
                .putForced(TextureSlot.TOP, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "2"))
            ,
            blockModels.modelOutput
        );
        blockModels.blockStateOutput.accept(
            BlockModelGenerators.createSimpleBlock(blockTank, blockModelLocation)
                .with(tankPosProperty)
        );

        var itemModel = ExtendedModelTemplateBuilder.builder()
            .parent(templates.tankItem())
            .build();
        var itemModelLocation = itemModel.create(ModelLocationUtils.getModelLocation(blockTank.asItem()),
            new TextureMapping()
                .putForced(TextureSlot.PARTICLE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
                .putForced(TextureSlot.SIDE, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "1"))
                .putForced(TextureSlot.TOP, blockTexture(tier.name().toLowerCase(Locale.ROOT) + "2"))
            ,
            itemModels.modelOutput
        );
        itemModels.itemModelOutput.accept(blockTank.asItem(), ItemModelUtils.specialModel(itemModelLocation, FluidRenderHelperNeoForge.tankUnbaked()));
    }

    /*void gasTank(BlockGasTank blockGasTank) {
        var tier = blockGasTank.tier();
        getVariantBuilder(blockGasTank)
            .forAllStates(blockState -> new ConfiguredModel[]{
                new ConfiguredModel(models().withExistingParent("gas_" + tier.getBlockName(), ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/tanks"))
                    .texture("particle", blockTexture("gas_%s1".formatted(tier.name().toLowerCase(Locale.ROOT))))
                    .texture("side", blockTexture("gas_%s1".formatted(tier.name().toLowerCase(Locale.ROOT))))
                    .texture("top", blockTexture("gas_%s2".formatted(tier.name().toLowerCase(Locale.ROOT))))
                    .renderType(renderTypeName(RenderType.cutout()))
                )
            });
        itemModels().withExistingParent(blockGasTank.registryName().getPath(), ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, ITEM_GAS_TANK_BASE))
            .texture("1", blockTexture("gas_%s1".formatted(tier.name().toLowerCase(Locale.ROOT))))
            .texture("2", blockTexture("gas_%s2".formatted(tier.name().toLowerCase(Locale.ROOT))));
    }*/

    /*@SuppressWarnings("SpellCheckingInspection")
    void pipeBase() {
        // Center Model
        models().getBuilder("block/" + "pipe_center")
            .renderType(renderTypeName(RenderType.cutout()))
            .element().from(4.0f, 4.0f, 4.0f).to(12.0f, 12.0f, 12.0f)
            .allFaces((direction, faceBuilder) -> faceBuilder.uvs(4.0f, 4.0f, 12.0f, 12.0f).texture("#texture"));
        // Side Model
        models().getBuilder("block/" + "pipe_side")
            .renderType(renderTypeName(RenderType.cutout()))
            .element().from(4.0f, 4.0f, 0.0f).to(12.0f, 12.0f, 4.0f)
            .face(Direction.SOUTH).uvs(4.0f, 4.0f, 12.0f, 12.0f).texture("#texture").cullface(Direction.SOUTH).end()
            .face(Direction.DOWN).uvs(4.0f, 6.0f, 12.0f, 10.0f).texture("#texture").end()
            .face(Direction.UP).uvs(4.0f, 6.0f, 12.0f, 10.0f).texture("#texture").end()
            .face(Direction.WEST).uvs(6.0f, 4.0f, 10.0f, 12.0f).texture("#texture").end()
            .face(Direction.EAST).uvs(6.0f, 4.0f, 10.0f, 12.0f).texture("#texture").end();

        // In-Out Model
        models().getBuilder("block/" + "pipe_in_out")
            .renderType(renderTypeName(RenderType.cutout()))
            // Inside
            .element().from(4, 4, 2).to(12, 12, 4)
            .face(Direction.SOUTH).uvs(4.0f, 4.0f, 12.0f, 12.0f).texture("#texture").cullface(Direction.SOUTH).end()
            .face(Direction.DOWN).uvs(4.0f, 6.0f, 12.0f, 10.0f).texture("#texture").end()
            .face(Direction.UP).uvs(4.0f, 6.0f, 12.0f, 10.0f).texture("#texture").end()
            .face(Direction.WEST).uvs(6.0f, 4.0f, 10.0f, 12.0f).texture("#texture").end()
            .face(Direction.EAST).uvs(6.0f, 4.0f, 10.0f, 12.0f).texture("#texture").end()
            .end()
            // Outside
            .element().from(2, 2, 0).to(14, 14, 2)
            .face(Direction.SOUTH).uvs(4.0f, 4.0f, 12.0f, 12.0f).texture("#side").end()
            .face(Direction.DOWN).uvs(2, 14, 14, 16).texture("#side").end()
            .face(Direction.UP).uvs(2, 0, 14, 2).texture("#side").end()
            .face(Direction.WEST).uvs(0, 2, 2, 14).texture("#side").end()
            .face(Direction.EAST).uvs(14, 2, 16, 14).texture("#side").end()
        ;

        // Item
        itemModels().withExistingParent("item/" + "pipe_base", "block/block")
            .transforms()
            .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.8f).end()
            .transform(ItemDisplayContext.FIXED).scale(0.8f).end()
            .end()
            .ao(false)
            .element()
            .from(4, 4, 4).to(12, 12, 12)
            .allFaces((direction, faceBuilder) ->
                faceBuilder.uvs(4, 4, 12, 12).texture("#texture")
            );
    }*/

    /*void pipe(PipeBlock pipeBlock, String modelBaseName) {
        String prefix = pipeBlock.registryName.getPath().replace("pipe", "");
        ResourceLocation frameTexture = blockTexture(prefix + "frame");
        var centerModel = models().withExistingParent("block/" + modelBaseName + "_center", ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/pipe_center"))
            .texture("particle", frameTexture)
            .texture("texture", frameTexture);
        var sideModel = models().withExistingParent("block/" + modelBaseName + "_side", ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/pipe_side"))
            .texture("particle", frameTexture)
            .texture("texture", frameTexture);
        var outModel = models().withExistingParent("block/" + modelBaseName + "_output", ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/pipe_in_out"))
            .texture("particle", frameTexture)
            .texture("texture", frameTexture)
            .texture("side", blockTexture(prefix + "frame_output"));
        var inModel = models().withExistingParent("block/" + modelBaseName + "_input", ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "block/pipe_in_out"))
            .texture("particle", frameTexture)
            .texture("texture", frameTexture)
            .texture("side", blockTexture(prefix + "frame_input"));
        getMultipartBuilder(pipeBlock).part()
            .modelFile(centerModel).addModel().end().part()
            // Connected
            .modelFile(sideModel).uvLock(true).addModel().condition(PipeBlock.NORTH, PipeBlock.Connection.CONNECTED).end().part()
            .modelFile(sideModel).uvLock(true).rotationY(90).addModel().condition(PipeBlock.EAST, PipeBlock.Connection.CONNECTED).end().part()
            .modelFile(sideModel).uvLock(true).rotationY(180).addModel().condition(PipeBlock.SOUTH, PipeBlock.Connection.CONNECTED).end().part()
            .modelFile(sideModel).uvLock(true).rotationY(270).addModel().condition(PipeBlock.WEST, PipeBlock.Connection.CONNECTED).end().part()
            .modelFile(sideModel).uvLock(true).rotationX(270).addModel().condition(PipeBlock.UP, PipeBlock.Connection.CONNECTED).end().part()
            .modelFile(sideModel).uvLock(true).rotationX(90).addModel().condition(PipeBlock.DOWN, PipeBlock.Connection.CONNECTED).end().part()
            // OUTPUT
            .modelFile(outModel).uvLock(true).addModel().condition(PipeBlock.NORTH, PipeBlock.Connection.OUTPUT).end().part()
            .modelFile(outModel).uvLock(true).rotationY(90).addModel().condition(PipeBlock.EAST, PipeBlock.Connection.OUTPUT).end().part()
            .modelFile(outModel).uvLock(true).rotationY(180).addModel().condition(PipeBlock.SOUTH, PipeBlock.Connection.OUTPUT).end().part()
            .modelFile(outModel).uvLock(true).rotationY(270).addModel().condition(PipeBlock.WEST, PipeBlock.Connection.OUTPUT).end().part()
            .modelFile(outModel).uvLock(true).rotationX(270).addModel().condition(PipeBlock.UP, PipeBlock.Connection.OUTPUT).end().part()
            .modelFile(outModel).uvLock(true).rotationX(90).addModel().condition(PipeBlock.DOWN, PipeBlock.Connection.OUTPUT).end().part()
            // INPUT
            .modelFile(inModel).uvLock(true).addModel().condition(PipeBlock.NORTH, PipeBlock.Connection.INPUT).end().part()
            .modelFile(inModel).uvLock(true).rotationY(90).addModel().condition(PipeBlock.EAST, PipeBlock.Connection.INPUT).end().part()
            .modelFile(inModel).uvLock(true).rotationY(180).addModel().condition(PipeBlock.SOUTH, PipeBlock.Connection.INPUT).end().part()
            .modelFile(inModel).uvLock(true).rotationY(270).addModel().condition(PipeBlock.WEST, PipeBlock.Connection.INPUT).end().part()
            .modelFile(inModel).uvLock(true).rotationX(270).addModel().condition(PipeBlock.UP, PipeBlock.Connection.INPUT).end().part()
            .modelFile(inModel).uvLock(true).rotationX(90).addModel().condition(PipeBlock.DOWN, PipeBlock.Connection.INPUT).end()
        ;

        itemModels().withExistingParent("item/" + pipeBlock.registryName.getPath(), ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "item/pipe_base"))
            .texture("texture", frameTexture);
    }*/

    void reservoirBase(ItemModelGenerators itemModels) {
        var template = ExtendedModelTemplateBuilder.builder()
            .guiLight(BlockModel.GuiLight.FRONT)
            .transform(ItemDisplayContext.FIXED, b -> b.scale(1f).translation(0, 0, 0).rotation(0, 180, 0))
            .transform(ItemDisplayContext.GROUND, b -> b.scale(0.5f).translation(0, 0, 0).rotation(0, 180, 0))
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, b -> b.scale(0.85f).translation(0f, 4.0f, 0.5f).rotation(0, 0, 0))
            .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, b -> b.scale(0.85f).translation(0f, 4.0f, 0.5f).rotation(0, 0, 0))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, b -> b.scale(0.68f).translation(1.13f, 3.2f, -1.13f).rotation(0, -90, 0))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, b -> b.scale(0.68f).translation(1.13f, 3.2f, -1.13f).rotation(0, 90, 0))
            .build();
        template.create(modLocation(ITEM_RESERVOIR_BASE), new TextureMapping(), itemModels.modelOutput);
    }

    void reservoir(ItemModelGenerators itemModels, ItemReservoir reservoirItem) {
        var key = ModelLocationUtils.getModelLocation(reservoirItem);
        var template = ExtendedModelTemplateBuilder.builder()
            .parent(modLocation(ITEM_RESERVOIR_BASE))
            .build();
        template.create(key,
            new TextureMapping()
                .putForced(TextureSlot.PARTICLE, blockTexture(reservoirItem.tier().name().toLowerCase(Locale.ROOT) + "1")),
            itemModels.modelOutput);
        var unbaked = ItemModelUtils.specialModel(key, FluidRenderHelperNeoForge.reservoirUnbaked());
        itemModels.itemModelOutput.accept(reservoirItem, unbaked);
    }

    private static String renderTypeName(RenderStateShard type) {
        try {
            var field = RenderStateShard.class.getDeclaredField("name");
            field.setAccessible(true);
            return (String) field.get(type);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    record TankModelTemplates(ResourceLocation tankBlock, ResourceLocation tankItem, ResourceLocation gasTankBlock) {
    }
}
