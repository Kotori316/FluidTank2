package com.kotori316.fluidtank.neoforge.test.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank}
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, fluidAccess}
import com.kotori316.fluidtank.item.PlatformItemAccess
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.tank.TankFluidItemHandler
import com.kotori316.fluidtank.neoforge.test.BeforeMC
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.item.{Item, ItemStack, Items}
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.storage.TagValueOutput
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{DynamicTest, Nested, Test, TestFactory}

import scala.util.Using
import scala.util.chaining.scalaUtilChainingOps

class TankFluidItemHandlerTest extends BeforeMC {
  val ITEM: Item = Items.APPLE

  @Test
  def create(): Unit = {
    val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
    assertTrue(handler.getContainer.is(ITEM))
  }

  @TestFactory
  def capacity(): Array[DynamicTest] = {
    val tiers = Tier.values().filter(_.isNormalTankTier)
    tiers.map(t => DynamicTest.dynamicTest(t.toString, () => {
      val handler = new TankFluidItemHandler(t, ItemAccess.forStack(new ItemStack(ITEM)))
      assertEquals(GenericUnit(t.getCapacity).asForge, handler.getCapacityAsLong(0, FluidResource.EMPTY))
    }))
  }

  @Test
  def load(): Unit = {
    val tank = Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(16000))
    val tagValueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING)
    tagValueOutput.store(TileTank.KEY_TANK, Tank.codec, tank)
    tagValueOutput.putString(TileTank.KEY_TIER, Tier.STONE.name())
    val stack = new ItemStack(ITEM)
    PlatformItemAccess.setTileTag(stack, tagValueOutput, BlockEntityType.TEST_BLOCK)

    val handler = new TankFluidItemHandler(Tier.STONE, ItemAccess.forStack(stack))
    assertEquals(tank, handler.getTank)
  }

  @Nested
  class FillTest {
    @Test
    def fillToEmpty(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      assertTrue(handler.getTank.isEmpty)

      Using.resource(Transaction.openRoot()) { tx =>
        val filled = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
        assertEquals(1000, filled)
        assertEquals(FluidAmountUtil.BUCKET_WATER, handler.getTank.content)
        assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
      }
      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))

      Using.resource(Transaction.openRoot()) { tx =>
        val filled = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
        assertEquals(1000, filled)
        assertEquals(FluidAmountUtil.BUCKET_WATER, handler.getTank.content)
        assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
        tx.commit()
      }
      assertEquals(FluidAmountUtil.BUCKET_WATER, handler.getTank.content)
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def fillToEmpty2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      assertTrue(handler.getTank.isEmpty)

      val filled1 = Using.resource(Transaction.openRoot()) { tx =>
        val f = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
        tx.commit()
        f
      }
      assertEquals(1000, filled1)
      val filled2 = Using.resource(Transaction.openRoot()) { tx =>
        val f = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
        tx.commit()
        f
      }
      assertEquals(1000, filled2)
      assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000)), handler.getTank.content)
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def fillToFilled1(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val f1 = Using.resource(Transaction.openRoot()) { tx =>
        handler.insert(FluidAmountUtil.BUCKET_LAVA.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
      }
      assertEquals(0, f1)
    }

    @Test
    def fillToFilled2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val f1 = Using.resource(Transaction.openRoot()) { tx =>
        handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, 500, tx)
      }
      assertEquals(500, f1)
    }

    @Test
    def fillToFilled3(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3800)), GenericUnit.fromForge(4000)))

      val f1 = Using.resource(Transaction.openRoot()) { tx =>
        handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, 500, tx)
      }
      assertEquals(200, f1)
      val f2 = Using.resource(Transaction.openRoot()) { tx =>
        val r = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, 500, tx)
        tx.commit()
        r
      }
      assertEquals(200, f2)
      assertEquals(
        FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(4000)),
        handler.getTank.content
      )
    }
  }

  @Nested
  class DrainTest {
    @Test
    def drain1(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val d1 = Using.resource(Transaction.openRoot()) { tx =>
        handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, 500, tx)
      }
      assertEquals(500, d1)
      assertEquals(
        FluidAmountUtil.BUCKET_WATER,
        handler.getTank.content
      )

      val d2 = Using.resource(Transaction.openRoot()) { tx =>
        val r = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, 500, tx)
        tx.commit()
        r
      }
      assertEquals(500, d2)
      assertEquals(
        FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(500)),
        handler.getTank.content
      )
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drain2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      val d2 = Using.resource(Transaction.openRoot()) { tx =>
        val r = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, 1000, tx)
        tx.commit()
        r
      }
      assertEquals(1000, d2)

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drain3(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      val d2 = Using.resource(Transaction.openRoot()) { tx =>
        val r = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, 1500, tx)
        tx.commit()
        r
      }
      assertEquals(1000, d2)

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drainFail(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(new ItemStack(ITEM)))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val t1 = Using.resource(Transaction.openRoot()) { tx =>
        handler.extract(FluidAmountUtil.BUCKET_LAVA.asVariant, 1500, tx)
      }
      assertEquals(0, t1)
    }

    /**
     * Unknown tag will be removed when the tank is made to be empty
     */
    @Test
    def unknownTagIsRemoved(): Unit = {
      val stack = new ItemStack(ITEM)
      val handler = new TankFluidItemHandler(Tier.WOOD, ItemAccess.forStack(stack))
      handler.setTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      stack.update[TypedEntityData[BlockEntityType[?]]](DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.TEST_BLOCK, new CompoundTag()), data => {
        TypedEntityData.of(data.`type`(), data.copyTagWithoutId().tap(_.putString("unknownTag", "unknownTag")))
      })
      Using.resource(Transaction.openRoot()) { tx =>
        val extracted = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, 1500, tx)
        tx.commit()
        assertEquals(1000, extracted)
      }

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }
  }
}
