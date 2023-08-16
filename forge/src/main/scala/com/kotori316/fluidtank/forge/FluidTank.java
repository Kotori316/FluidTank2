package com.kotori316.fluidtank.forge;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.cat.ItemChestAsTank;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.forge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.forge.config.ForgePlatformConfigAccess;
import com.kotori316.fluidtank.forge.integration.ae2.AE2FluidTankIntegration;
import com.kotori316.fluidtank.forge.integration.top.FluidTankTopPlugin;
import com.kotori316.fluidtank.forge.message.PacketHandler;
import com.kotori316.fluidtank.forge.recipe.IgnoreUnknownTagIngredient;
import com.kotori316.fluidtank.forge.recipe.TierRecipeForge;
import com.kotori316.fluidtank.forge.tank.*;
import com.kotori316.fluidtank.tank.*;
import com.mojang.datafixers.DSL;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
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
        BLOCK_REGISTER.register(modBus);
        ITEM_REGISTER.register(modBus);
        CREATIVE_TAB_REGISTER.register(modBus);
        BLOCK_ENTITY_REGISTER.register(modBus);
        RECIPE_REGISTER.register(modBus);
        PlatformAccess.setInstance(new ForgePlatformAccess());
        setupConfig(modBus);
        modBus.register(this);
        modBus.register(proxy);
        PacketHandler.init();
        AE2FluidTankIntegration.onAPIAvailable();
        FluidTankTopPlugin.sendIMC();
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize finished {}", FluidTankCommon.modId);
    }

    private static void setupConfig(IEventBus modBus) {
        var config = new ForgePlatformConfigAccess(modBus);
        var builder = config.setupConfig();
        PlatformConfigAccess.setInstance(config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, builder.build());
    }

    private static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, FluidTankCommon.modId);
    private static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, FluidTankCommon.modId);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FluidTankCommon.modId);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_REGISTER = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FluidTankCommon.modId);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FluidTankCommon.modId);

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
    public static final LootItemFunctionType TANK_LOOT_FUNCTION = new LootItemFunctionType(new TankLootFunction.TankLootSerializer());
    public static final RegistryObject<RecipeSerializer<?>> TIER_RECIPE = RECIPE_REGISTER.register(TierRecipeForge.Serializer.LOCATION.getPath(), () -> TierRecipeForge.SERIALIZER);
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

    public static final class LazyHolder {

        private static void init(RegisterEvent event) {
            if (event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE)) {
                Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,
                    new ResourceLocation(FluidTankCommon.modId, TankLootFunction.NAME),
                    TANK_LOOT_FUNCTION);
            }
        }
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void callRegister(RegisterEvent event) {
        LazyHolder.init(event);
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            CraftingHelper.register(new ResourceLocation(FluidTankCommon.modId, "ignore_unknown_tag_ingredient"), IgnoreUnknownTagIngredient.SERIALIZER);
        }
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
        });
    }
}
