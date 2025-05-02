package com.kotori316.fluidtank.fabric.gametest;

import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

@SuppressWarnings("unused") // All methods are used in other projects.
public final class GameTestUtil {
    public static void throwExceptionAt(GameTestHelper helper, BlockPos relativePos, String message)
        throws GameTestAssertPosException {
        var absolutePos = helper.absolutePos(relativePos);
        throw new GameTestAssertPosException(Component.literal(message), absolutePos, relativePos, (int) helper.getTick());
    }

    public static TileTank placeTank(GameTestHelper helper, BlockPos pos, Tier tier) {
        var block = PlatformTankAccess.getInstance().getTankBlockMap().get(tier).get();
        helper.setBlock(pos, block);
        var tile = helper.getBlockEntity(pos, TileTank.class);
        tile.onBlockPlacedBy();
        return tile;
    }
}
