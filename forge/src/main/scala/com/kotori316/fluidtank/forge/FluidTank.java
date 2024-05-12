package com.kotori316.fluidtank.forge;

import com.kotori316.fluidtank.DebugLogging;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.cat.ItemChestAsTank;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.forge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.forge.config.ForgePlatformConfigAccess;
import com.kotori316.fluidtank.forge.integration.top.FluidTankTopPlugin;
import com.kotori316.fluidtank.forge.message.PacketHandler;
import com.kotori316.fluidtank.forge.recipe.IgnoreUnknownTagIngredient;
import com.kotori316.fluidtank.forge.recipe.TierRecipeForge;
import com.kotori316.fluidtank.forge.reservoir.ItemReservoirForge;
import com.kotori316.fluidtank.forge.tank.*;
import com.kotori316.fluidtank.tank.*;
import com.mojang.datafixers.DSL;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    public FluidTank() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize {}", FluidTankCommon.modId);
        ForgeMod.enableMilkFluid();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTER_LIST.forEach(r -> r.register(modBus));
        PlatformAccess.setInstance(new ForgePlatformAccess());
        setupConfig(modBus);
        modBus.register(this);
        modBus.register(proxy);
        PacketHandler.init();
        // AE2FluidTankIntegration.onAPIAvailable();
        FluidTankTopPlugin.sendIMC();
        MinecraftForge.EVENT_BUS.addListener(FluidTank::onServerStart);
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize finished {}", FluidTankCommon.modId);
    }

    private static void setupConfig(IEventBus modBus) {
        var config = new ForgePlatformConfigAccess();
        modBus.register(config);
        var builder = config.setupConfig();
        PlatformConfigAccess.setInstance(config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, builder.build());
    }

    private static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, FluidTankCommon.modId);
    private static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, FluidTankCommon.modId);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FluidTankCommon.modId);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_REGISTER = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FluidTankCommon.modId);
    private static final DeferredRegister<IIngredientSerializer<?>> INGREDIENT_REGISTER = DeferredRegister.create(ForgeRegistries.INGREDIENT_SERIALIZERS, FluidTankCommon.modId);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FluidTankCommon.modId);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_TYPE_REGISTER = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, FluidTankCommon.modId);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE_REGISTER = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FluidTankCommon.modId);
    static final List<DeferredRegister<?>> REGISTER_LIST = List.of(
        BLOCK_REGISTER, ITEM_REGISTER, BLOCK_ENTITY_REGISTER, RECIPE_REGISTER, INGREDIENT_REGISTER, CREATIVE_TAB_REGISTER, LOOT_TYPE_REGISTER
    );

    public static final Map<Tier, RegistryObject<BlockTankForge>> TANK_MAP = Stream.of(Tier.values())
        .filter(Tier::isNormalTankTier)
        .collect(Collectors.toMap(Function.identity(), t -> BLOCK_REGISTER.register(t.getBlockName(), () -> new BlockTankForge(t))));
    public static final RegistryObject<BlockCreativeTankForge> BLOCK_CREATIVE_TANK =
        BLOCK_REGISTER.register(Tier.CREATIVE.getBlockName(), BlockCreativeTankForge::new);
    public static final RegistryObject<BlockVoidTankForge> BLOCK_VOID_TANK =
        BLOCK_REGISTER.register(Tier.VOID.getBlockName(), BlockVoidTankForge::new);
    public static final Map<Tier, RegistryObject<ItemBlockTank>> TANK_ITEM_MAP =
        Stream.concat(TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ITEM_REGISTER.register(e.getKey().getBlockName(), () -> e.getValue().get().itemBlock())));
    public static final RegistryObject<BlockEntityType<TileTankForge>> TILE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileTankForge::new, TANK_MAP.values().stream().map(RegistryObject::get).toArray(BlockTank[]::new))
                .build(DSL.emptyPartType()));
    public static final RegistryObject<BlockEntityType<TileCreativeTankForge>> TILE_CREATIVE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileCreativeTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileCreativeTankForge::new, BLOCK_CREATIVE_TANK.get()).build(DSL.emptyPartType()));
    public static final RegistryObject<BlockEntityType<TileVoidTankForge>> TILE_VOID_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileVoidTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileVoidTankForge::new, BLOCK_VOID_TANK.get()).build(DSL.emptyPartType()));
    public static final RegistryObject<LootItemFunctionType<TankLootFunction>> TANK_LOOT_FUNCTION = LOOT_TYPE_REGISTER.register(TankLootFunction.NAME, () -> new LootItemFunctionType<>(TankLootFunction.CODEC));
    public static final RegistryObject<RecipeSerializer<?>> TIER_RECIPE = RECIPE_REGISTER.register(TierRecipeForge.Serializer.LOCATION.getPath(), () -> TierRecipeForge.SERIALIZER);
    public static final RegistryObject<IIngredientSerializer<IgnoreUnknownTagIngredient>> IU_INGREDIENT = INGREDIENT_REGISTER.register(IgnoreUnknownTagIngredient.NAME, () -> IgnoreUnknownTagIngredient.SERIALIZER);
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_TAB_REGISTER.register("tab", () -> {
        var b = CreativeModeTab.builder();
        createTab(b);
        return b.build();
    });
    public static final RegistryObject<BlockChestAsTank> BLOCK_CAT = BLOCK_REGISTER.register(BlockChestAsTank.NAME(), BlockChestAsTank::new);
    public static final RegistryObject<BlockItem> ITEM_CAT = ITEM_REGISTER.register(BlockChestAsTank.NAME(), () -> new ItemChestAsTank(BLOCK_CAT.get()));
    public static final RegistryObject<BlockEntityType<EntityChestAsTank>> TILE_CAT =
        BLOCK_ENTITY_REGISTER.register(BlockChestAsTank.NAME(), () ->
            BlockEntityType.Builder.of(EntityChestAsTank::new, BLOCK_CAT.get()).build(DSL.emptyPartType()));
    public static final Map<Tier, RegistryObject<ItemReservoirForge>> RESERVOIR_MAP = Stream.of(Tier.WOOD, Tier.STONE, Tier.IRON)
        .collect(Collectors.toMap(Function.identity(), t -> ITEM_REGISTER.register("reservoir_" + t.name().toLowerCase(Locale.ROOT), () -> new ItemReservoirForge(t))));
    public static final RegistryObject<DataComponentType<Tank<FluidLike>>> FLUID_TANK_DATA_COMPONENT = DATA_COMPONENT_TYPE_REGISTER.register(
        "fluid_tank_component", () -> DataComponentType.<Tank<FluidLike>>builder()
            .persistent(Tank.codec(FluidAmountUtil.access()))
            .build()
    );

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
    }

    private static void createTab(CreativeModeTab.Builder builder) {
        builder.icon(() -> TANK_MAP.get(Tier.WOOD).map(ItemStack::new).orElseThrow());
        builder.title(Component.translatable("itemGroup.fluidtank"));
        builder.displayItems((parameters, output) -> {
            // Tanks
            TANK_ITEM_MAP.values().stream().map(RegistryObject::get).sorted(Comparator.comparing(i -> i.blockTank().tier()))
                .forEach(output::accept);
            // Chest As Tank
            output.accept(ITEM_CAT.get());
            // Reservoir
            RESERVOIR_MAP.values().stream().map(RegistryObject::get).sorted(Comparator.comparing(ItemReservoirForge::tier))
                .forEach(output::accept);
        });
    }

    static void onServerStart(ServerStartedEvent event) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "OnServerStart {}, {}", FluidTankCommon.modId, event.getServer().getMotd());
        DebugLogging.initialLog(event.getServer());
    }
}
