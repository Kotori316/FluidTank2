package com.kotori316.fluidtank.neoforge;

import com.kotori316.fluidtank.DebugLogging;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.cat.ItemChestAsTank;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.neoforge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.neoforge.config.NeoForgePlatformConfigAccess;
import com.kotori316.fluidtank.neoforge.integration.top.FluidTankTopPlugin;
import com.kotori316.fluidtank.neoforge.message.PacketHandler;
import com.kotori316.fluidtank.neoforge.recipe.IgnoreUnknownTagIngredient;
import com.kotori316.fluidtank.neoforge.recipe.TierRecipeNeoForge;
import com.kotori316.fluidtank.neoforge.reservoir.ItemReservoirNeoForge;
import com.kotori316.fluidtank.neoforge.tank.*;
import com.kotori316.fluidtank.tank.*;
import com.mojang.datafixers.DSL;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.registries.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(FluidTankCommon.modId)
public final class FluidTank {
    public static final SideProxy proxy = SideProxy.get();

    public FluidTank(IEventBus modBus) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize {}", FluidTankCommon.modId);
        NeoForgeMod.enableMilkFluid();
        REGISTER_LIST.forEach(r -> r.register(modBus));
        PlatformAccess.setInstance(new NeoForgePlatformAccess());
        setupConfig(modBus);
        modBus.register(this);
        modBus.register(proxy);
        modBus.addListener(FluidTank::registerCapabilities);
        // AE2FluidTankIntegration.onAPIAvailable(modBus);
        FluidTankTopPlugin.sendIMC();
        NeoForge.EVENT_BUS.addListener(FluidTank::onServerStart);
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize finished {}", FluidTankCommon.modId);
    }

    private static void setupConfig(IEventBus modBus) {
        var config = new NeoForgePlatformConfigAccess();
        modBus.register(config);
        var builder = config.setupConfig();
        PlatformConfigAccess.setInstance(config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, builder.build());
    }

    private static final DeferredRegister.Blocks BLOCK_REGISTER = DeferredRegister.createBlocks(FluidTankCommon.modId);
    private static final DeferredRegister.Items ITEM_REGISTER = DeferredRegister.createItems(FluidTankCommon.modId);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FluidTankCommon.modId);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, FluidTankCommon.modId);
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_REGISTER = DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, FluidTankCommon.modId);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTER = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, FluidTankCommon.modId);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_TYPE_REGISTER = DeferredRegister.create(BuiltInRegistries.LOOT_FUNCTION_TYPE, FluidTankCommon.modId);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE_REGISTER = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, FluidTankCommon.modId);
    static final List<DeferredRegister<?>> REGISTER_LIST = List.of(
        BLOCK_REGISTER, ITEM_REGISTER, BLOCK_ENTITY_REGISTER, RECIPE_REGISTER, INGREDIENT_REGISTER, CREATIVE_TAB_REGISTER, LOOT_TYPE_REGISTER, DATA_COMPONENT_TYPE_REGISTER
    );

    public static final Map<Tier, DeferredBlock<BlockTankNeoForge>> TANK_MAP = Stream.of(Tier.values())
        .filter(Tier::isNormalTankTier)
        .collect(Collectors.toMap(Function.identity(), t -> BLOCK_REGISTER.register(t.getBlockName(), () -> new BlockTankNeoForge(t))));
    public static final DeferredBlock<BlockCreativeTankNeoForge> BLOCK_CREATIVE_TANK =
        BLOCK_REGISTER.register(Tier.CREATIVE.getBlockName(), BlockCreativeTankNeoForge::new);
    public static final DeferredBlock<BlockVoidTankNeoForge> BLOCK_VOID_TANK =
        BLOCK_REGISTER.register(Tier.VOID.getBlockName(), BlockVoidTankNeoForge::new);
    public static final Map<Tier, DeferredItem<ItemBlockTank>> TANK_ITEM_MAP =
        Stream.concat(TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ITEM_REGISTER.register(e.getKey().getBlockName(), () -> e.getValue().get().itemBlock())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTankNeoForge>> TILE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileTankNeoForge::new, TANK_MAP.values().stream().map(DeferredHolder::get).toArray(BlockTank[]::new))
                .build(DSL.emptyPartType()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCreativeTankNeoForge>> TILE_CREATIVE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileCreativeTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileCreativeTankNeoForge::new, BLOCK_CREATIVE_TANK.get()).build(DSL.emptyPartType()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVoidTankNeoForge>> TILE_VOID_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileVoidTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileVoidTankNeoForge::new, BLOCK_VOID_TANK.get()).build(DSL.emptyPartType()));
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<TankLootFunction>> TANK_LOOT_FUNCTION = LOOT_TYPE_REGISTER.register(TankLootFunction.NAME, () -> new LootItemFunctionType<>(TankLootFunction.CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> TIER_RECIPE = RECIPE_REGISTER.register(TierRecipeNeoForge.Serializer.LOCATION.getPath(), () -> TierRecipeNeoForge.SERIALIZER);
    public static final DeferredHolder<IngredientType<?>, IngredientType<IgnoreUnknownTagIngredient>> IU_INGREDIENT = INGREDIENT_REGISTER.register(IgnoreUnknownTagIngredient.NAME, () -> IgnoreUnknownTagIngredient.SERIALIZER);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TAB_REGISTER.register("tab", () -> {
        var b = CreativeModeTab.builder();
        createTab(b);
        return b.build();
    });
    public static final DeferredBlock<BlockChestAsTank> BLOCK_CAT = BLOCK_REGISTER.register(BlockChestAsTank.NAME(), BlockChestAsTank::new);
    public static final DeferredItem<BlockItem> ITEM_CAT = ITEM_REGISTER.register(BlockChestAsTank.NAME(), () -> new ItemChestAsTank(BLOCK_CAT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EntityChestAsTank>> TILE_CAT =
        BLOCK_ENTITY_REGISTER.register(BlockChestAsTank.NAME(), () ->
            BlockEntityType.Builder.of(EntityChestAsTank::new, BLOCK_CAT.get()).build(DSL.emptyPartType()));
    public static final Map<Tier, DeferredItem<ItemReservoirNeoForge>> RESERVOIR_MAP = Stream.of(Tier.WOOD, Tier.STONE, Tier.IRON)
        .collect(Collectors.toMap(Function.identity(), t -> ITEM_REGISTER.register("reservoir_" + t.name().toLowerCase(Locale.ROOT), () -> new ItemReservoirNeoForge(t))));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Tank<FluidLike>>> FLUID_TANK_DATA_COMPONENT = DATA_COMPONENT_TYPE_REGISTER.register(
        "fluid_tank_component", () -> DataComponentType.<Tank<FluidLike>>builder()
            .persistent(Tank.codec(FluidAmountUtil.access()))
            .build()
    );

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void setupPacket(RegisterPayloadHandlerEvent event) {
        PacketHandler.init(event);
    }

    private static void createTab(CreativeModeTab.Builder builder) {
        builder.icon(() -> new ItemStack(TANK_MAP.get(Tier.WOOD).get()));
        builder.title(Component.translatable("itemGroup.fluidtank"));
        builder.displayItems((parameters, output) -> {
            // Tanks
            TANK_ITEM_MAP.values().stream().map(DeferredHolder::get).sorted(Comparator.comparing(i -> i.blockTank().tier()))
                .forEach(output::accept);
            // Chest As Tank
            output.accept(ITEM_CAT.get());
            // Reservoir
            RESERVOIR_MAP.values().stream().map(DeferredHolder::get).sorted(Comparator.comparing(ItemReservoirNeoForge::tier))
                .forEach(output::accept);
        });
    }

    static void onServerStart(ServerStartedEvent event) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "OnServerStart {}, {}", FluidTankCommon.modId, event.getServer().getMotd());
        DebugLogging.initialLog(event.getServer());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TILE_TANK_TYPE.get(), TileTankNeoForge::getCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TILE_CREATIVE_TANK_TYPE.get(), TileCreativeTankNeoForge::getCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TILE_VOID_TANK_TYPE.get(), TileVoidTankNeoForge::getCapability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TILE_CAT.get(), EntityChestAsTank::getCapability);
        event.registerItem(Capabilities.FluidHandler.ITEM, ItemBlockTankNeoForge::initCapabilities, TANK_ITEM_MAP.values().stream().map(DeferredItem::asItem).toArray(Item[]::new));
        event.registerItem(Capabilities.FluidHandler.ITEM, ItemReservoirNeoForge::initCapabilities, RESERVOIR_MAP.values().stream().map(DeferredItem::asItem).toArray(Item[]::new));
    }
}
