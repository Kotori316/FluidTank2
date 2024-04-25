package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.DebugLogging;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.cat.ItemChestAsTank;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fabric.cat.ChestAsTankStorage;
import com.kotori316.fluidtank.fabric.config.FabricPlatformConfigAccess;
import com.kotori316.fluidtank.fabric.integration.ae2.AE2FluidTankIntegration;
import com.kotori316.fluidtank.fabric.message.PacketHandler;
import com.kotori316.fluidtank.fabric.recipe.IgnoreUnknownTagIngredientFabric;
import com.kotori316.fluidtank.fabric.recipe.TierRecipeFabric;
import com.kotori316.fluidtank.fabric.reservoir.ReservoirFluidStorage;
import com.kotori316.fluidtank.fabric.tank.*;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.recipe.TierRecipe;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.*;
import com.mojang.datafixers.DSL;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FluidTank implements ModInitializer {
    @Override
    public void onInitialize() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize {}", FluidTankCommon.modId);
        PacketHandler.Server.initServer();
        PlatformAccess.setInstance(new FabricPlatformAccess());
        PlatformConfigAccess.setInstance(new FabricPlatformConfigAccess());
        registerObjects();
        ConnectionStorage.register();
        ChestAsTankStorage.register();
        FabricTankItemStorage.register();
        ReservoirFluidStorage.register();
        AE2FluidTankIntegration.onAPIAvailable();
        ServerLifecycleEvents.SERVER_STARTED.register(FluidTank::onServerStart);
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize finished {}", FluidTankCommon.modId);
    }

    public static final Map<Tier, BlockTankFabric> TANK_MAP = Stream.of(Tier.values())
        .filter(Tier::isNormalTankTier)
        .collect(Collectors.toMap(Function.identity(), BlockTankFabric::new));
    public static final BlockCreativeTankFabric BLOCK_CREATIVE_TANK = new BlockCreativeTankFabric();
    public static final BlockVoidTank BLOCK_VOID_TANK = new BlockVoidTank();
    public static final Map<Tier, ItemBlockTank> TANK_ITEM_MAP =
        Stream.concat(TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().itemBlock()));
    public static final BlockEntityType<TileTankFabric> TILE_TANK_TYPE =
        BlockEntityType.Builder.of(TileTankFabric::new, TANK_MAP.values().toArray(BlockTank[]::new)).build(DSL.emptyPartType());
    public static final BlockEntityType<TileCreativeTankFabric> TILE_CREATIVE_TANK_TYPE =
        BlockEntityType.Builder.of(TileCreativeTankFabric::new, BLOCK_CREATIVE_TANK).build(DSL.emptyPartType());
    public static final BlockEntityType<TileVoidTank> TILE_VOID_TANK_TYPE =
        BlockEntityType.Builder.of(TileVoidTank::new, BLOCK_VOID_TANK).build(DSL.emptyPartType());
    public static final LootItemFunctionType<TankLootFunction> TANK_LOOT_FUNCTION = new LootItemFunctionType<>(TankLootFunction.CODEC);
    public static final RecipeSerializer<TierRecipe> TIER_RECIPE_SERIALIZER = TierRecipeFabric.SERIALIZER;
    public static final BlockChestAsTank BLOCK_CAT = new BlockChestAsTank();
    public static final BlockItem ITEM_CAT = new ItemChestAsTank(BLOCK_CAT);
    public static final Map<Tier, ItemReservoir> RESERVOIR_MAP = Stream.of(Tier.WOOD, Tier.STONE, Tier.IRON)
        .collect(Collectors.toMap(Function.identity(), ItemReservoir::new));
    public static final DataComponentType<Tank<FluidLike>> FLUID_TANK_DATA_COMPONENT = DataComponentType.<Tank<FluidLike>>builder()
        .persistent(Tank.codec(FluidAmountUtil.access()))
        .build();

    private static void registerObjects() {
        Stream.concat(TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, BLOCK_VOID_TANK)))
            .forEach(e -> Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(FluidTankCommon.modId, e.getKey().getBlockName()), e.getValue()));
        TANK_ITEM_MAP.forEach((tier, itemBlockTank) -> Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(FluidTankCommon.modId, tier.getBlockName()), itemBlockTank));
        Map.of(TileTank.class, TILE_TANK_TYPE, TileCreativeTank.class, TILE_CREATIVE_TANK_TYPE, TileVoidTank.class, TILE_VOID_TANK_TYPE)
            .forEach((c, t) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(FluidTankCommon.modId, c.getSimpleName().toLowerCase(Locale.ROOT)), t));
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, new ResourceLocation(FluidTankCommon.modId, TankLootFunction.NAME), TANK_LOOT_FUNCTION);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, TierRecipe.SerializerBase.LOCATION, TIER_RECIPE_SERIALIZER);
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(FluidTankCommon.modId, BlockChestAsTank.NAME()), BLOCK_CAT);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(FluidTankCommon.modId, BlockChestAsTank.NAME()), ITEM_CAT);
        RESERVOIR_MAP.values().forEach(e -> Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(FluidTankCommon.modId, "reservoir_" + e.tier().name().toLowerCase(Locale.ROOT)), e));
        var builder = FabricItemGroup.builder();
        createTab(builder);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new ResourceLocation(FluidTankCommon.modId, FluidTankCommon.modId), builder.build());
        CustomIngredientSerializer.register(IgnoreUnknownTagIngredientFabric.SERIALIZER);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, new ResourceLocation(FluidTankCommon.modId, Tank.COMPONENT_NAME()), FLUID_TANK_DATA_COMPONENT);
    }

    private static void createTab(CreativeModeTab.Builder builder) {
        builder.icon(() -> new ItemStack(TANK_MAP.get(Tier.WOOD)));
        builder.title(Component.translatable("itemGroup.fluidtank"));
        builder.displayItems((parameters, output) -> {
            // Tanks
            TANK_ITEM_MAP.values().stream().sorted(Comparator.comparing(i -> i.blockTank().tier()))
                .forEach(output::accept);
            output.accept(ITEM_CAT);
            RESERVOIR_MAP.values().stream().sorted(Comparator.comparing(ItemReservoir::tier))
                .forEach(output::accept);
        });
    }

    static void onServerStart(MinecraftServer server) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "OnServerStart {}, {}", FluidTankCommon.modId, server.getMotd());
        DebugLogging.initialLog(server);
    }
}
