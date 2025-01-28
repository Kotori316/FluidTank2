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
import net.minecraft.core.Vec3i;

@SuppressWarnings("UnstableApiUsage")
public final class FluidLevelTest implements FluidTankClientGameTest {
    private static final int[] FILL_LEVELS = {1000, 3000, 6000, 9000, 12000, 15000, 18000, 20000};

    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        // Test with both water and lava
        testFluidLevels(this, context, singlePlayerContext, "water", FluidAmountUtil.BUCKET_WATER());
        testFluidLevels(this, context, singlePlayerContext, "lava", FluidAmountUtil.BUCKET_LAVA());
    }

    static void testFluidLevels(FluidTankClientGameTest testInstance, ClientGameTestContext context,
                                TestSingleplayerContext singlePlayerContext, String fluidName,
                                GenericAmount<FluidLike> fluid) {
        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);
            // Place two tanks
            c.placeBlock(3, PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState());
            c.placeBlockRelativeOffset(3, Vec3i.ZERO.above(), PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.STONE).get().defaultBlockState());
        });
        singlePlayerContext.getClientWorld().waitForChunksRender();

        // Take screenshots for each fill level
        for (int amount : FILL_LEVELS) {
            singlePlayerContext.getServer().runOnServer(server -> {
                var c = FluidTankClientGameTest.getServerDataContext(server);
                var pos = c.getPos(3);
                var tankTile = (TileTank) c.level().getBlockEntity(pos);
                if (tankTile == null) {
                    throw new IllegalStateException("No tank tile at %s (Player: %s)".formatted(
                        pos.toShortString(), c.playerOnPos().toShortString()));
                }
                tankTile.getConnection().getHandler().set(fluid.setAmount(GenericUnit.fromForge(amount)));
            });

            context.waitTicks(PACKET_WAIT_TICKS); // Wait for packet transmission
            singlePlayerContext.getClientWorld().waitForChunksRender();
            testInstance.takeScreenshot(context, s -> s.concat("/").concat(fluidName), "%s_level_%d".formatted(fluidName, amount));
        }
    }
} 
