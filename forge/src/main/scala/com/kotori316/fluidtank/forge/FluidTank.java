package com.kotori316.fluidtank.forge;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.DSL;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.forge.tank.BlockCreativeTankForge;
import com.kotori316.fluidtank.forge.tank.BlockTankForge;
import com.kotori316.fluidtank.forge.tank.BlockVoidTankForge;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.ItemBlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileCreativeTank;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.fluidtank.tank.TileVoidTank;

@Mod(FluidTankCommon.modId)
public final class FluidTank {
    public static final SideProxy proxy = SideProxy.get();

    public FluidTank() {
        ForgeMod.enableMilkFluid();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCK_REGISTER.register(modBus);
        ITEM_REGISTER.register(modBus);
        BLOCK_ENTITY_REGISTER.register(modBus);
        PlatformAccess.setInstance(new ForgePlatformAccess());
        modBus.register(this);
        modBus.register(proxy);
    }

    private static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, FluidTankCommon.modId);
    private static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, FluidTankCommon.modId);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FluidTankCommon.modId);

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
    public static final RegistryObject<BlockEntityType<TileTank>> TILE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileTank::new, TANK_MAP.values().stream().map(RegistryObject::get).toArray(BlockTank[]::new))
                .build(DSL.emptyPartType()));
    public static final RegistryObject<BlockEntityType<TileCreativeTank>> TILE_CREATIVE_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileCreativeTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileCreativeTank::new, BLOCK_CREATIVE_TANK.get()).build(DSL.emptyPartType()));
    public static final RegistryObject<BlockEntityType<TileVoidTank>> TILE_VOID_TANK_TYPE =
        BLOCK_ENTITY_REGISTER.register(TileVoidTank.class.getSimpleName().toLowerCase(Locale.ROOT), () ->
            BlockEntityType.Builder.of(TileVoidTank::new, BLOCK_VOID_TANK.get()).build(DSL.emptyPartType()));

    private static final class ForgePlatformAccess implements PlatformAccess {

        @Override
        public boolean isGaseous(Fluid fluid) {
            return fluid.getFluidType().isLighterThanAir();
        }

        @Override
        @NotNull
        public Fluid getBucketContent(BucketItem bucketItem) {
            return bucketItem.getFluid();
        }

        @Override
        public BlockEntityType<? extends TileTank> getNormalType() {
            return TILE_TANK_TYPE.get();
        }

        @Override
        public BlockEntityType<? extends TileTank> getCreativeType() {
            return TILE_CREATIVE_TANK_TYPE.get();
        }

        @Override
        public BlockEntityType<? extends TileTank> getVoidType() {
            return TILE_VOID_TANK_TYPE.get();
        }
    }

    @SubscribeEvent
    public void registerCreativeTab(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(FluidTankCommon.modId, "tab"), FluidTank::createTab);
    }

    private static void createTab(CreativeModeTab.Builder builder) {
        builder.icon(() -> TANK_MAP.get(Tier.WOOD).map(ItemStack::new).orElseThrow());
        builder.title(Component.translatable("itemGroup.fluidtank"));
        builder.displayItems((parameters, output) -> {
            // Tanks
            TANK_ITEM_MAP.values().stream().map(RegistryObject::get).sorted(Comparator.comparing(i -> i.blockTank().tier()))
                .forEach(output::accept);
        });
    }
}