package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fabric.recipe.{ModifiableSingleItemStorage, RecipeInventoryUtil}
import com.kotori316.fluidtank.fabric.{BeforeMC, FluidTank}
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, fluidAccess}
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidConstants, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals, assertNotEquals, assertNotNull, assertNull, assertTrue}
import org.junit.jupiter.api.{Nested, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import java.util.Objects
import scala.util.Using

@SuppressWarnings(Array("UnstableApiUsage"))
//noinspection UnstableApiUsage
final class FabricTankItemStorageTest extends BeforeMC {
  @Test
  def instance(): Unit = {
    val storage = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD))))
    assertNotNull(storage)
    assertAll(
      () => assertTrue(storage.isResourceBlank),
      () => assertEquals(0, storage.getAmount),
      () => assertEquals(4 * FluidConstants.BUCKET, storage.getCapacity)
    )
  }

  @Test
  def initialState1(): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD))
    val tag = stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG)
    tag.putString(TileTank.KEY_TIER, Tier.WOOD.name)
    val tank = Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit(Tier.WOOD.getCapacity))
    tag.put(TileTank.KEY_TANK, TankUtil.save(tank))
    val storage = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
    assertAll(
      () => assertEquals(FluidConstants.BUCKET, storage.getAmount),
      () => assertEquals(FluidVariant.of(Fluids.WATER), storage.getResource),
      () => assertEquals(4 * FluidConstants.BUCKET, storage.getCapacity),
      () => assertEquals(tank, storage.getTank),
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
    val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
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
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      assertNull(BlockItem.getBlockEntityData(handler.getStack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.blank, handler.getResource)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        val tag = BlockItem.getBlockEntityData(handler.getStack)
        assertNotNull(tag)
        transaction.commit()
      }

      assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
      assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
      val tag = BlockItem.getBlockEntityData(handler.getStack)
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
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(FluidConstants.BUCKET, handler.getAmount)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(4 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        transaction.commit()
      }

      val tag = BlockItem.getBlockEntityData(handler.getStack)
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
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      assertNull(BlockItem.getBlockEntityData(handler.getStack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.blank, handler.getResource)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(3 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        val tag = BlockItem.getBlockEntityData(handler.getStack)
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
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        assertEquals(FluidConstants.BUCKET, handler.getAmount)
        val inserted = handler.insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, transaction)
        assertEquals(4 * FluidConstants.BUCKET, handler.getAmount)
        assertEquals(3 * FluidConstants.BUCKET, inserted)
        transaction.abort()
      }

      assertEquals(before, BlockItem.getBlockEntityData(handler.getStack))
    }
  }

  @Nested
  class DrainTest {
    @Test
    def unknownTagAdded(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER)
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG)
        .putString("unknownTag", "unknownTag")
      Using(Transaction.openOuter()) { transaction =>
        val drained = handler.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction)
        assertEquals(FluidConstants.BUCKET, drained)
        transaction.commit()
      }

      assertTrue(handler.getTank.isEmpty, "Tank: " + handler.getTank)
      assertNull(BlockItem.getBlockEntityData(handler.getStack))
    }

    @Test
    def drainSimulate1(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER)
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))

      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        val drained = handler.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction)
        assertEquals(FluidConstants.BUCKET, drained)
        transaction.abort()
      }

      assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
      assertNotNull(BlockItem.getBlockEntityData(handler.getStack))
    }

    @Test
    def drainExecute1(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER)
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))

      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        val drained = handler.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction)
        assertEquals(FluidConstants.BUCKET, drained)
        transaction.commit()
      }

      assertEquals(FluidVariant.blank, handler.getResource)
      assertTrue(handler.getTank.isEmpty)
      assertNull(BlockItem.getBlockEntityData(handler.getStack))
    }

    @Test
    def drainFail1(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_LAVA)
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))

      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        val drained = handler.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction)
        assertEquals(0, drained)
        transaction.commit()
      }
      assertEquals(FluidVariant.of(Fluids.LAVA), handler.getResource)
      assertEquals(FluidAmountUtil.BUCKET_LAVA, handler.getTank.content)
    }

    @Test
    def drainExecute2(): Unit = {
      val tier = Tier.WOOD
      val stack = RecipeInventoryUtil.getFilledTankStack(tier, FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000)))
      val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
      Using(Transaction.openOuter()) { transaction =>
        assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
        val drained = handler.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction)
        assertEquals(FluidConstants.BUCKET, drained)
        transaction.commit()
      }
      assertEquals(FluidVariant.of(Fluids.WATER), handler.getResource)
      assertEquals(FluidAmountUtil.BUCKET_WATER, handler.getTank.content)
    }
  }
}
