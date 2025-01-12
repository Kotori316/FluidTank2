package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.TestSingleplayerContext;
import net.minecraft.core.Vec3i;

@SuppressWarnings("UnstableApiUsage")
public final class WaterTankTest implements FluidTankClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);

            c.placeBlock(3, PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState());
            c.placeBlockRelativeOffset(3, Vec3i.ZERO.above(), PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState());
        });
        singlePlayerContext.getClientWorld().waitForChunksRender();
        takeScreenshot(context, "water_before");

        singlePlayerContext.getServer().runOnServer(server -> {
            var c = FluidTankClientGameTest.getServerDataContext(server);
            var pos = c.getPos(3);
            var tankTile = (TileTank) c.level().getBlockEntity(pos);
            if (tankTile == null) {
                throw new IllegalStateException("No tank tile at %s (Player: %s)".formatted(pos.toShortString(), c.playerOnPos().toShortString()));
            }
            tankTile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER().setAmount(GenericUnit.fromForge(6000)), true);
        });
        context.waitTicks(10); // wait until packet is sent
        singlePlayerContext.getClientWorld().waitForChunksRender();
        takeScreenshot(context, "water_after");
    }
}
