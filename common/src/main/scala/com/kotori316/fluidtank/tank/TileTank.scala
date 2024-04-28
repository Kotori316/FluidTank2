package com.kotori316.fluidtank.tank

import cats.implicits.toShow
import com.kotori316.fluidtank.MCImplicits.*
import com.kotori316.fluidtank.connection.Connection
import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fluids.*
import com.kotori316.fluidtank.tank.TileTank.{KEY_STACK_NAME, KEY_TANK, KEY_TIER}
import com.kotori316.fluidtank.{DebugLogging, FluidTankCommon}
import net.minecraft.core.{BlockPos, HolderLookup}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.Nameable
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import org.jetbrains.annotations.{NotNull, Nullable}

import java.util.Locale

class TileTank(var tier: Tier, t: BlockEntityType[? <: TileTank], p: BlockPos, s: BlockState)
  extends BlockEntity(t, p, s) with Nameable {

  def this(p: BlockPos, s: BlockState) = {
    this(Tier.INVALID, PlatformTankAccess.getInstance.getNormalType, p, s)
  }

  def this(tier: Tier, p: BlockPos, s: BlockState) = {
    this(tier, PlatformTankAccess.getInstance.getNormalType, p, s)
  }

  private var connection: FluidConnection = new FluidConnection(Nil)
  private var tank: Tank[FluidLike] = Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity))
  private var customName: Option[Component] = None

  def setConnection(c: FluidConnection): Unit = this.connection = c

  def getConnection: FluidConnection = this.connection

  def setTank(tank: Tank[FluidLike]): Unit = {
    this.tank = tank
    this.setChanged()
  }

  def getTank: Tank[FluidLike] = this.tank

  // Override of BlockEntity
  override def loadAdditional(tag: CompoundTag, provider: HolderLookup.Provider): Unit = {
    super.loadAdditional(tag, provider)
    this.setTank(TankUtil.load(tag.getCompound(KEY_TANK)))
    this.tier = Tier.valueOf(tag.getString(KEY_TIER))
    this.customName = Option.when(tag.contains(KEY_STACK_NAME))(
      Component.Serializer.fromJson(tag.getString(KEY_STACK_NAME), provider)
    )
  }

  override def saveAdditional(tag: CompoundTag, provider: HolderLookup.Provider): Unit = {
    tag.put(KEY_TANK, TankUtil.save(this.tank))
    tag.putString(KEY_TIER, this.tier.name())
    this.customName.foreach(c => tag.putString(KEY_STACK_NAME, Component.Serializer.toJson(c, provider)))
    super.saveAdditional(tag, provider)
  }

  override def getUpdateTag(provider: HolderLookup.Provider): CompoundTag = this.saveWithoutMetadata(provider)

  override def getUpdatePacket: ClientboundBlockEntityDataPacket = ClientboundBlockEntityDataPacket.create(this)

  // Override of Nameable
  @NotNull
  override def getName: Component = this.customName.getOrElse(Component.literal(this.tier.toString + " Tank"))

  @Nullable
  override def getCustomName: Component = this.customName.orNull

  def setCustomName(@Nullable customName: Component): Unit = {
    this.customName = Option(customName)
  }

  // Methods called from block

  def getComparatorLevel: Int = this.connection.getComparatorLevel

  def onDestroy(): Unit = {
    this.connection.remove(this)
  }

  def onBlockPlacedBy(): Unit = {
    {
      DebugLogging.LOGGER.debug(
        "Connection {} loaded in onBlockPlacedBy. At={}, connection={}",
        if (this.connection.isDummy) "will be" else "won't",
        this.getBlockPos.show, this.connection)
    }
    // Do nothing if the connection is already created.
    if (!this.connection.isDummy) return
    val downTank = Option(getLevel.getBlockEntity(getBlockPos.below())).collect { case t: TileTank => t }
    val upTank = Option(getLevel.getBlockEntity(getBlockPos.above())).collect { case t: TileTank => t }
    val newSeq = (downTank, upTank) match {
      case (Some(dT), Some(uT)) => dT.connection.getTiles :+ this :++ uT.connection.getTiles
      case (None, Some(uT)) => this +: uT.connection.getTiles
      case (Some(dT), None) => dT.connection.getTiles :+ this
      case (None, None) => Seq(this)
    }
    if (downTank.exists(_.connection.isDummy) || upTank.exists(_.connection.isDummy)) {
      // Something wrong. Reset the connection
      Connection.load(getLevel, getBlockPos, classOf[TileTank])
    } else {
      // Just add new tank to connection
      Connection.createAndInit(newSeq)
    }
  }

  def onTickLoading(): Unit = {
    // Do nothing if the connection is already created.
    if (this.connection.isDummy) {
      // Create connection if this tank has invalid one.
      DebugLogging.LOGGER.debug(
        "Connection {} loaded in onLoading. At={}, tank={}",
        "will be",
        this.getBlockPos.show, this.getTank)
      Connection.load(getLevel, getBlockPos, classOf[TileTank])
    }
  }
}

object TileTank {
  final val KEY_TANK = "tank" // Tag map
  final val KEY_TIER = "tier" // Tag map provided in Tier class (Actually, String)
  final val KEY_STACK_NAME = "stackName" // String parsed in Text

  final val registryName = "%s:%s".formatted(FluidTankCommon.modId, classOf[TileTank].getSimpleName.toLowerCase(Locale.ROOT))
}
