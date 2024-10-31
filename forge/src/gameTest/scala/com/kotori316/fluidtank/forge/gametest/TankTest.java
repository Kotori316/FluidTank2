package com.kotori316.fluidtank.forge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.forge.FluidTank;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@GameTestHolder(FluidTankCommon.modId)
final class TankTest {

    private static final String BATCH = "defaultBatch";

    @GameTestGenerator
    List<TestFunction> fillTest() {
        return GetGameTestMethods.getTests(getClass(), this, BATCH, TankTest::wrapDefaultConfig);
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

    static void wrapDefaultConfig(Runnable r) {
        var currentConfig = PlatformConfigAccess.getInstance();
        PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
        try {
            r.run();
        } finally {
            PlatformConfigAccess.setInstance(currentConfig);
        }
    }

    void capability1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile1 = placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(), Tier.STONE);

        var cap = tile1.getCapability(ForgeCapabilities.FLUID_HANDLER);
        assertTrue(cap.isPresent());
        var handler = cap.orElseThrow(AssertionError::new);
        assertEquals(20000, handler.getTankCapacity(0));
        helper.succeed();
    }
}
