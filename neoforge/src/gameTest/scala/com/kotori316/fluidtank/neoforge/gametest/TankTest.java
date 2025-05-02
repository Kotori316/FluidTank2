package com.kotori316.fluidtank.neoforge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("unused")
@GameTestHolder(FluidTankCommon.modId)
final class TankTest {

    private static final String BATCH = "defaultBatch";

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
        var tile = helper.getBlockEntity(pos);
        if (tile instanceof TileTank tileTank) {
            tileTank.onBlockPlacedBy();
            return tileTank;
        } else {
            throw new GameTestAssertPosException("Expect tank tile", helper.absolutePos(pos), pos, helper.getTick());
        }
    }

    void capability1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(), Tier.STONE);

        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(basePos), Direction.NORTH);
        assertNotNull(handler);
        assertEquals(20000, handler.getTankCapacity(0));
        helper.succeed();
    }
}
