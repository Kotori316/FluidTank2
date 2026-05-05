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
import net.minecraft.core.Vec3i;

@SuppressWarnings("UnstableApiUsage")
public final class WaterTankTest implements FluidTankClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        String testName = "water";
        var fluid = FluidAmountUtil.BUCKET_WATER();

        testFluid(this, context, singlePlayerContext, testName, fluid);
    }

    static void testFluid(FluidTankClientGameTest testInstance, ClientGameTestContext context, TestSingleplayerContext singlePlayerContext, String testName, GenericAmount<FluidLike> fluid) {
        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);

            c.placeBlock(3, PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState());
            c.placeBlockRelativeOffset(3, Vec3i.ZERO.above(), PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState());
        });
        singlePlayerContext.getClientLevel().waitForChunksRender();
        testInstance.takeScreenshot(context, testName + "_before");

        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);
            var pos = c.getPos(3);
            var tankTile = (TileTank) c.level().getBlockEntity(pos);
            if (tankTile == null) {
                throw new IllegalStateException("No tank tile at %s (Player: %s)".formatted(pos.toShortString(), c.playerOnPos().toShortString()));
            }
            tankTile.getConnection().getHandler().fill(fluid.setAmount(GenericUnit.fromForge(6000)), true);
        });
        context.waitTicks(PACKET_WAIT_TICKS); // wait until packet is sent
        singlePlayerContext.getClientLevel().waitForChunksRender();
        testInstance.takeScreenshot(context, testName + "_after");
    }
}
