package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.connection.Connection
import com.kotori316.fluidtank.contents.{GenericUnit, Tank}
import com.kotori316.fluidtank.fluids.*
import com.kotori316.fluidtank.tank.TileTankTest.{BlockCreativeTankForTest, BlockTankForTest, TileCreativeTankForTest, TileTankForTest}
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{Nested, Test}
import org.mockito.{ArgumentMatchers, Mockito}

class TileTankTest extends BeforeMC {
  private val tankBlock = new BlockTankForTest()
  private val creativeTankBlock = new BlockCreativeTankForTest
  private val voidTankBlock = new BlockVoidTank

  def createTile(tier: Tier, pos: BlockPos): TileTank = {
    val tileType: BlockEntityType[? <: TileTank] = Mockito.mock(classOf[BlockEntityType[? <: TileTank]])
    Mockito.when(tileType.isValid(ArgumentMatchers.any())).thenReturn(true)
    new TileTankForTest(tier, tileType, pos, tankBlock.defaultBlockState())
  }

  def createCreativeTile(pos: BlockPos): TileCreativeTank = {
    val tileType: BlockEntityType[? <: TileTank] = Mockito.mock(classOf[BlockEntityType[? <: TileTank]])
    Mockito.when(tileType.isValid(ArgumentMatchers.any())).thenReturn(true)
    new TileCreativeTankForTest(tileType, pos, creativeTankBlock.defaultBlockState())
  }

  def createVoidTile(pos: BlockPos): TileVoidTank = {
    val tileType: BlockEntityType[? <: TileTank] = Mockito.mock(classOf[BlockEntityType[? <: TileTank]])
    Mockito.when(tileType.isValid(ArgumentMatchers.any())).thenReturn(true)
    new TileVoidTank(tileType, pos, voidTankBlock.defaultBlockState())
  }

  @Test
  def create(): Unit = {
    val tile = createTile(Tier.WOOD, BlockPos.ZERO)
    assertNotNull(tile)
  }

  @Nested
  class InitialTest {
    @Test
    def connection(): Unit = {
      val tile = createTile(Tier.WOOD, BlockPos.ZERO)
      val c = tile.getConnection
      assertTrue(c.isDummy)
    }

    @Test
    def defaultTank(): Unit = {
      val tile = createTile(Tier.WOOD, BlockPos.ZERO)
      assertTrue(tile.getTank.isEmpty)
      assertEquals(GenericUnit.ZERO, tile.getConnection.amount)
      assertEquals(Option.empty, tile.getConnection.getContent)
    }

    @Test
    def initialCapacity(): Unit = {
      val tile = createTile(Tier.WOOD, BlockPos.ZERO)
      assertEquals(GenericUnit.fromForge(4000), tile.getTank.capacity)
      assertNotEquals(GenericUnit.fromForge(4000), tile.getConnection.capacity)
    }
  }

  @Nested
  class EstablishConnection {
    @Test
    def createConnection(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      Connection.createAndInit(Seq(tile1, tile2))

      assertSame(tile1.getConnection, tile2.getConnection)
      val c = tile1.getConnection
      assertEquals(2, c.getHandler.getTank.size)
    }

    @Test
    def createConnectionWithCreative(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val tile3 = createCreativeTile(BlockPos.ZERO.above(2))
      Connection.createAndInit(Seq(tile1, tile2, tile3))

      assertSame(tile1.getConnection, tile2.getConnection)
      assertSame(tile3.getConnection, tile2.getConnection)
      val c = tile1.getConnection
      assertEquals(3, c.getHandler.getTank.size)
      assertEquals(3, c.getTiles.size)
    }

    @Test
    def createConnectionWithVoid(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val tile3 = createVoidTile(BlockPos.ZERO.above(2))
      Connection.createAndInit(Seq(tile1, tile2, tile3))

      assertSame(tile1.getConnection, tile2.getConnection)
      assertSame(tile3.getConnection, tile2.getConnection)
      val c = tile1.getConnection
      assertEquals(3, c.getHandler.getTank.size)
      assertEquals(3, c.getTiles.size)
    }

    @Test
    def connectionAmount(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      Connection.createAndInit(Seq(tile1, tile2))
      val c = tile1.getConnection

      assertEquals(GenericUnit.fromForge(20000), c.capacity)
    }

    @Test
    def move(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val fluid = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000))
      tile2.setTank(tile2.getTank.copy(content = fluid))
      Connection.createAndInit(Seq(tile1, tile2))
      val c = tile1.getConnection
      assertEquals(
        Option(fluid),
        c.getContent,
      )

      assertEquals(fluid, tile1.getTank.content)
      assertTrue(tile2.getTank.isEmpty)
    }
  }

  @Nested
  class FillDrainTest {
    @Test
    def fill1(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val fluid = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000))
      tile1.setTank(tile1.getTank.copy(content = fluid))
      Connection.createAndInit(Seq(tile1, tile2))

      val c = tile1.getConnection
      val filled = c.getHandler.fill(fluid, execute = false)
      assertEquals(fluid, filled)
      val filled2 = c.getHandler.fill(filled, execute = true)
      assertEquals(filled2, filled)
      assertEquals(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(4000)), GenericUnit.fromForge(4000)), tile1.getTank)
      assertEquals(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000)), GenericUnit.fromForge(16000)), tile2.getTank)
    }

    @Test
    def fillWithCreative(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val tile3 = createCreativeTile(BlockPos.ZERO.above(2))
      Connection.createAndInit(Seq(tile1, tile2, tile3))
      val c = tile1.getConnection

      val filled1 = c.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = false)
      assertEquals(FluidAmountUtil.BUCKET_WATER, filled1)
      assertTrue(c.getHandler.getTank.forall(_.isEmpty), s"Simulate, ${c.getHandler.getTank}")

      val filled2 = c.getHandler.fill(filled1, execute = true)
      assertEquals(filled2, filled1)
      assertEquals(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit(Tier.WOOD.getCapacity)), GenericUnit(Tier.WOOD.getCapacity)), tile1.getTank)
      assertEquals(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit(Tier.STONE.getCapacity)), GenericUnit(Tier.STONE.getCapacity)), tile2.getTank)
    }

    @Test
    def drain1(): Unit = {
      val tile1 = createTile(Tier.WOOD, BlockPos.ZERO)
      val tile2 = createTile(Tier.STONE, BlockPos.ZERO.above())
      val fluid = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000))
      tile1.setTank(tile1.getTank.copy(content = fluid))
      Connection.createAndInit(Seq(tile1, tile2))
      val c = tile1.getConnection

      val drained = c.getHandler.drain(FluidAmountUtil.BUCKET_WATER, execute = false)
      assertEquals(FluidAmountUtil.BUCKET_WATER, drained)
      val d2 = c.getHandler.drain(drained, execute = true)
      assertEquals(drained, d2)
      assertEquals(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000)), GenericUnit.fromForge(4000)), tile1.getTank)
      assertTrue(tile2.getTank.isEmpty)
    }
  }

  @Nested
  class NameTest {
    @Test
    def noCustomName(): Unit = {
      val tile = createTile(Tier.WOOD, BlockPos.ZERO)
      assertNotNull(tile.getName)
      assertNull(tile.getCustomName)
    }

    @Test
    def setName(): Unit = {
      val name = Component.literal("CustomName")
      val tile = createTile(Tier.WOOD, BlockPos.ZERO)
      tile.setCustomName(name)
      assertEquals(name, tile.getName)
      assertEquals(name, tile.getCustomName)
      assertEquals(name, tile.getDisplayName)
    }
  }
}

object TileTankTest {

  class BlockTankForTest extends BlockTank(Tier.WOOD) {

    override protected def createBlockInstance(): BlockTank = throw new UnsupportedOperationException("BlockTankForTest#createBlockInstance")

    override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = new TileTankForTest(this.tier, null, pos, state)
  }

  class TileTankForTest(tier: Tier, t: BlockEntityType[? <: TileTank], p: BlockPos, s: BlockState) extends TileTank(tier, t, p, s) {
    override def getVisualTank: VisualTank = new VisualTank

    override def isValidBlockState(blockState: BlockState): Boolean = true
  }

  class BlockCreativeTankForTest extends BlockCreativeTank {
    override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = throw new UnsupportedOperationException("BlockCreativeTankForTest#createBlockInstance")
  }

  class TileCreativeTankForTest(t: BlockEntityType[? <: TileTank], p: BlockPos, s: BlockState) extends TileCreativeTank(t, p, s) {
    override def isValidBlockState(blockState: BlockState): Boolean = true
  }
}
