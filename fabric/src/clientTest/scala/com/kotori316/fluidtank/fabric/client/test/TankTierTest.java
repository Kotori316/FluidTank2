package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

import java.util.Locale;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public final class TankTierTest implements FluidTankClientGameTest {
    // Test with 0%, 25%, 50%, 75%, 100% of each tier's capacity
    private static final double[] FILL_RATIOS = {0.0, 0.25, 0.5, 0.75, 1.0};

    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        // Test with both water and lava
        testTierLevels(this, context, singlePlayerContext, "water", FluidAmountUtil.BUCKET_WATER());
        testTierLevels(this, context, singlePlayerContext, "lava", FluidAmountUtil.BUCKET_LAVA());
    }

    static void testTierLevels(FluidTankClientGameTest testInstance, ClientGameTestContext context,
                               TestSingleplayerContext singlePlayerContext, String fluidName,
                               GenericAmount<FluidLike> fluid) {
        Stream.of(Tier.values())
            .filter(Tier::isNormalTankTier)
            .forEach(tier -> {
                singlePlayerContext.getServer().runOnServer(server -> {
                    var c = FluidTankClientGameTest.getServerDataContext(server);
                    // Place two tanks of same tier
                    var tankBlock = PlatformTankAccess.getInstance().getTankBlockMap().get(tier).get().defaultBlockState();
                    c.placeBlockRelativeOffset(3, BlockPos.ZERO.above(), tankBlock);
                });
                singlePlayerContext.getClientWorld().waitForChunksRender();

                // Test different fill levels
                var capacity = GenericUnit.asForgeFromBigInt(tier.getCapacity()).toInt();
                for (double ratio : FILL_RATIOS) {
                    int amount = (int) (capacity * ratio);
                    singlePlayerContext.getServer().runOnServer(server -> {
                        var c = FluidTankClientGameTest.getServerDataContext(server);
                        var pos = c.getPos(3, BlockPos.ZERO.above());
                        var tankTile = (TileTank) c.level().getBlockEntity(pos);
                        if (tankTile == null) {
                            throw new IllegalStateException("No tank tile at %s (Player: %s)".formatted(
                                pos.toShortString(), c.playerOnPos().toShortString()));
                        }
                        tankTile.getConnection().getHandler().set(fluid.setAmount(GenericUnit.fromForge(amount)));
                    });

                    context.waitTicks(10);
                    singlePlayerContext.getClientWorld().waitForChunksRender();
                    testInstance.takeScreenshot(context,
                        s -> s.concat("/").concat(tier.name().toLowerCase(Locale.ROOT)),
                        "%s_%s_level_%d".formatted(fluidName, tier.name().toLowerCase(Locale.ROOT), amount));
                    if (tier == Tier.CREATIVE && amount > 0) {
                        break;
                    }
                }
            });
    }
} 
