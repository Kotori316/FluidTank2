package com.kotori316.fluidtank.neoforge.gametest;

import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.testutil.common.TestFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("unused")
final class TankTest {

    private static final String BATCH = GetGameTestMethods.DEFAULT_BATCH;

    List<TestFunction> fillTest() {
        return GetGameTestMethods.getTests(getClass(), this, BATCH);
    }

    static Supplier<? extends BlockTank> getBlock(Tier tier) {
        return switch (tier) {
            case CREATIVE -> FluidTank.BLOCK_CREATIVE_TANK;
            case VOID -> FluidTank.BLOCK_VOID_TANK;
            default -> FluidTank.TANK_MAP.get(tier);
        };
    }

    static TileTank placeTank(GameTestHelper helper, BlockPos pos, Tier tier) {
        var block = getBlock(tier);
        helper.setBlock(pos, block.get());
        var tileTank = helper.getBlockEntity(pos, TileTank.class);
        tileTank.onBlockPlacedBy();
        return tileTank;
    }

    void capability1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(), Tier.STONE);

        var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(basePos), Direction.NORTH);
        assertNotNull(handler);
        assertEquals(20000, handler.getCapacityAsLong(0, FluidResource.EMPTY));
        helper.succeed();
    }
}
