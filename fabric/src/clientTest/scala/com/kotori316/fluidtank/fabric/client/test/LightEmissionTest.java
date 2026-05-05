package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings("UnstableApiUsage")
public final class LightEmissionTest implements FluidTankClientGameTest {
    private static final double[] FILL_RATIOS = {0.0, 0.25, 0.5, 0.75, 1.0};

    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        // Set time to night (18,000 is midnight)
        singlePlayerContext.getServer().runOnServer(server -> {
            var overworld = server.overworld();
            overworld.dimensionType().defaultClock().ifPresent(c -> overworld.clockManager().moveToTimeMarker(c, ClockTimeMarkers.MIDNIGHT));
        });

        // Test with both water and lava
        testLightEmission(this, context, singlePlayerContext, "water", FluidAmountUtil.BUCKET_WATER());
        testLightEmission(this, context, singlePlayerContext, "lava", FluidAmountUtil.BUCKET_LAVA());
    }

    static void testLightEmission(FluidTankClientGameTest testInstance, ClientGameTestContext context,
                                  TestSingleplayerContext singlePlayerContext, String fluidName,
                                  GenericAmount<FluidLike> fluid) {
        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);
            // Clear previous tank if exists
            c.placeBlockRelativeOffset(3, BlockPos.ZERO.above(), Blocks.AIR.defaultBlockState());
            // Place wooden tank
            var tankBlock = PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState();
            c.placeBlockRelativeOffset(3, BlockPos.ZERO.above(), tankBlock);
        });
        singlePlayerContext.getClientLevel().waitForChunksRender();

        // Take screenshots for each fill level
        for (var ratio : FILL_RATIOS) {
            int amount = (int) (GenericUnit.asForgeFromBigInt(Tier.WOOD.getCapacity()).toInt() * ratio);
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

            context.waitTicks(PACKET_WAIT_TICKS);
            singlePlayerContext.getClientLevel().waitForChunksRender();
            testInstance.takeScreenshot(context,
                s -> s.concat("/night"),
                "%s_night_level_%d".formatted(fluidName, amount));
        }
    }
} 
