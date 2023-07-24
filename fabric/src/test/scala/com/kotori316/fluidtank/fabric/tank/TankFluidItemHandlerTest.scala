package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fabric.recipe.RecipeInventoryUtil
import com.kotori316.fluidtank.fabric.{BeforeMC, FluidTank}
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, fluidAccess}
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidConstants, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{Nested, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import java.util.Objects
import scala.util.Using

@SuppressWarnings(Array("UnstableApiUsage"))
//noinspection UnstableApiUsage
final class TankFluidItemHandlerTest extends BeforeMC {
  @Test
  def createInstance(): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD))
    val handler = new TankFluidItemHandler(Tier.WOOD, stack)
    assertAll(
      () => assertEquals(0, handler.getAmount),
      () => assertEquals(FluidVariant.blank, handler.getResource),
      () => assertEquals(4 * FluidConstants.BUCKET, handler.getCapacity)
    )
  }

  @Test
  def initialState1(): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD))
    val tag = stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG)
    tag.putString(TileTank.KEY_TIER, Tier.WOOD.name)
    val tank = Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit(Tier.WOOD.getCapacity))
    tag.put(TileTank.KEY_TANK, TankUtil.save(tank))
    val handler = new TankFluidItemHandler(Tier.WOOD, stack)
    assertAll(
      () => assertEquals(FluidConstants.BUCKET, handler.getAmount),
      () => assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource),
      () => assertEquals(4 * FluidConstants.BUCKET, handler.getCapacity)
    )
  }

  @Test
  def initialState2(): Unit = {
    val tier = Tier.STONE
    val stack = new ItemStack(FluidTank.TANK_MAP.get(tier))
    val tag = stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG)
    tag.putString(TileTank.KEY_TIER, tier.name)
    val tank = Tank.apply(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(3000)), GenericUnit(tier.getCapacity))
    tag.put(TileTank.KEY_TANK, TankUtil.save(tank))
    val handler = new TankFluidItemHandler(tier, stack)
    assertAll(
      () => assertEquals(FluidConstants.BUCKET * 3, handler.getAmount),
      () => assertEquals(FluidVariant.of(Fluids.LAVA), handler.getResource),
      () => assertEquals(16 * FluidConstants.BUCKET, handler.getCapacity)
    )
  }

  @Nested
  class UtilTest {
    @ParameterizedTest
    @EnumSource(value = classOf[Tier], names = Array("WOOD", "STONE", "IRON", "GOLD"))
    def filled(tier: Tier): Unit = {
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_LAVA)
      val tag = BlockItem.getBlockEntityData(stack)
      assertNotNull(tag)
      val expected = Tank.apply(FluidAmountUtil.BUCKET_LAVA, GenericUnit(tier.getCapacity))
      val actual = TankUtil.load(tag.getCompound(TileTank.KEY_TANK))
      assertAll(
        () => assertEquals(expected, actual),
        () => assertEquals(tier.name, tag.getString(TileTank.KEY_TIER))
      )
    }
  }

  @Nested
  class FillTest {
    @Test
    def fillExecute(): Unit = {
      val tier = Tier.WOOD
      val stack = new ItemStack(FluidTank.TANK_MAP.get(tier))
      val handler = new TankFluidItemHandler(tier, stack)
      assertNull(BlockItem.getBlockEntityData(stack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.blank, handler.getResource)
        handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
        val tag = BlockItem.getBlockEntityData(stack)
        assertNotNull(tag)
        transaction.commit()
      }

      assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
      assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
      val tag = BlockItem.getBlockEntityData(stack)
      assertNotNull(tag)
      val expected = Tank.apply(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000)), GenericUnit(tier.getCapacity))
      val actual = TankUtil.load(tag.getCompound(TileTank.KEY_TANK))
      assertAll(() => assertEquals(expected, actual), () => assertEquals(tier.name, tag.getString(TileTank.KEY_TIER)))
    }

    @Test
    def fillExecute2(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER)
      val before = Objects.requireNonNull(BlockItem.getBlockEntityData(stack)).copy
      val handler = new TankFluidItemHandler(tier, stack)
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(FluidConstants.BUCKET, handler.getAmount)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(4 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        transaction.commit()
      }

      val tag = BlockItem.getBlockEntityData(stack)
      assertNotEquals(before, tag)
      assertNotNull(tag)
      val expected = Tank.apply(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(4000)), GenericUnit(tier.getCapacity))
      val actual = TankUtil.load(tag.getCompound(TileTank.KEY_TANK))
      assertAll(() => assertEquals(expected, actual), () => assertEquals(tier.name, tag.getString(TileTank.KEY_TIER)))
    }

    @Test
    def fillSimulate(): Unit = {
      val tier = Tier.WOOD
      val stack = new ItemStack(FluidTank.TANK_MAP.get(tier))
      val handler = new TankFluidItemHandler(tier, stack)
      assertNull(BlockItem.getBlockEntityData(stack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.blank, handler.getResource)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        val tag = BlockItem.getBlockEntityData(stack)
        assertNotNull(tag)
        transaction.abort()
      }

      assertEquals(FluidVariant.blank, handler.getResource)
      assertEquals(0, handler.getAmount)
      val tag = BlockItem.getBlockEntityData(stack)
      assertNull(tag)
    }

    @Test
    def fillSimulate2(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER)
      val before = Objects.requireNonNull(BlockItem.getBlockEntityData(stack)).copy
      val handler = new TankFluidItemHandler(tier, stack)
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(FluidConstants.BUCKET, handler.getAmount)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(4 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        transaction.abort()
      }

      assertEquals(before, BlockItem.getBlockEntityData(stack))
    }
  }
}