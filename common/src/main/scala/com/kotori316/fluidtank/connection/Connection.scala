package com.kotori316.fluidtank.connection

import cats.implicits.{toFoldableOps, toShow}
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.MCImplicits.*
import com.kotori316.fluidtank.connection.ConnectionHelper.ConnectionHelperMethods
import com.kotori316.fluidtank.contents.{ChainTanksHandler, GenericAmount, GenericUnit}
import com.kotori316.fluidtank.tank.TankPos
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.entity.BlockEntity

import scala.math.Ordering.Implicits.infixOrderingOps

abstract class Connection[TileType] protected(protected val sortedTanks: Seq[TileType]) {
  implicit val helper: ConnectionHelper[TileType]

  val hasCreative: Boolean = sortedTanks.exists(_.isCreative)
  val hasVoid: Boolean = sortedTanks.exists(_.isVoid)
  final val isDummy: Boolean = sortedTanks.isEmpty

  protected final val handler: helper.Handler = helper.createHandler(this.sortedTanks)

  def getTiles: Seq[TileType] = this.sortedTanks

  // Assuming head or last tank contains the content.
  protected def contentType: GenericAmount[helper.Content] =
    this.sortedTanks.headOption.flatMap(t => helper.getContent(t))
      .orElse(this.sortedTanks.lastOption.flatMap(t => helper.getContent(t)))
      .getOrElse(helper.defaultAmount)

  def capacity: GenericUnit = handler.getSumOfCapacity

  def amount: GenericUnit = this.sortedTanks.foldMap(_.getAmount) min GenericUnit.CREATIVE_TANK

  def getContent: Option[GenericAmount[helper.Content]] =
    Option(contentType).filter(_.nonEmpty).map(_.setAmount(this.amount))

  def remove(tank: TileType): Unit = {
    // Do nothing for dummy connection
    if (this.isDummy) return

    val (s1, s2) = this.sortedTanks.span(_ != tank)
    val s1Connection = this.helper.createConnection(s1)
    val s2Connection = this.helper.createConnection(s2.tail)
    // tank itself should mark handlers invalid.

    s1.foreach(this.helper.connectionSetter(s1Connection))
    s2.tail.foreach(this.helper.connectionSetter(s2Connection))
  }

  def getComparatorLevel: Int = {
    if (amount > GenericUnit.ZERO)
      Mth.floor(amount.asForgeDouble / capacity.asForgeDouble * 14) + 1
    else 0
  }

  override def toString: String = {
    s"${getClass.getSimpleName}{tanks=${sortedTanks.size},content=$contentType}"
  }
}

object Connection {

  @scala.annotation.tailrec
  def createAndInit[TankType, ContentType, HandlerType <: ChainTanksHandler[ContentType]]
  (tankSeq: Seq[TankType])(implicit helper: ConnectionHelper.Aux[TankType, ContentType, HandlerType]): Unit = {
    if (tankSeq.nonEmpty) {
      val sorted = tankSeq.sortBy(_.getPos.getY)
      val kind = sorted.flatMap(_.getContent).find(_.nonEmpty).getOrElse(helper.defaultAmount)
      val (s1, s2) = sorted.span { t =>
        val c = t.getContent
        // c is option, so empty tank is ignored in this context
        c.forall(t => t.contentEqual(kind))
      }
      require(s1.map(_.getContent).forall(c => c.forall(t => t.contentEqual(kind))))
      val connection = helper.createConnection(s1)
      // Safe cast
      val contentType = kind.setAmount(GenericUnit.MAX).asInstanceOf[GenericAmount[connection.helper.Content]]
      val content = connection.handler.drain(contentType, execute = true)
      connection.handler.fill(content, execute = true)
      s1 foreach helper.connectionSetter(connection)

      if (s2.nonEmpty) createAndInit(s2)
    }
  }

  def load[TankType <: BlockEntity, ContentType, HandlerType <: ChainTanksHandler[ContentType]]
  (level: BlockGetter, pos: BlockPos, tankClass: Class[TankType])(implicit helper: ConnectionHelper.Aux[TankType, ContentType, HandlerType]): Unit = {
    val lowest = Iterator.iterate(pos)(_.below())
      .takeWhile(p => tankClass.isInstance(level.getBlockEntity(p)))
      .toList.lastOption.getOrElse {
        FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_CONNECTION, f"No lowest tank at ${pos.show}, ${level.getBlockState(pos)}", new IllegalStateException("No lowest tank"))
        pos
      }
    val tanks = Iterator.iterate(lowest)(_.above())
      .map(level.getBlockEntity)
      .takeWhile(tankClass.isInstance)
      .map(tankClass.cast)
      .toList
    if (tanks.isEmpty) {
      FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_CONNECTION,
        s"Tried to create connection with no tanks at ${pos.show}, ${level.getBlockState(pos)}, class=$tankClass",
        new IllegalStateException("No valid tanks"))
    }
    createAndInit(tanks)
  }

  /**
   * Constructor wrapper to modify block state of tanks.
   */
  def updatePosPropertyAndCreateConnection[TileType <: BlockEntity, ConnectionType <: Connection[TileType]]
  (s: Seq[TileType], constructor: Seq[TileType] => ConnectionType): ConnectionType = {
    if (s.isEmpty) {
      constructor(Nil)
    } else {
      val seq = s.sortBy(_.getBlockPos.getY)
      // Property update
      if (seq.lengthIs > 1) {
        // HEAD
        val head = seq.head
        head.getLevel.setBlockAndUpdate(head.getBlockPos, head.getBlockState.setValue(TankPos.TANK_POS_PROPERTY, TankPos.BOTTOM))
        // LAST
        val last = seq.last
        last.getLevel.setBlockAndUpdate(last.getBlockPos, last.getBlockState.setValue(TankPos.TANK_POS_PROPERTY, TankPos.TOP))
        // MIDDLE
        seq.tail.init.foreach(t => t.getLevel.setBlockAndUpdate(t.getBlockPos, t.getBlockState.setValue(TankPos.TANK_POS_PROPERTY, TankPos.MIDDLE)))
      } else {
        // SINGLE
        seq.foreach(t => t.getLevel.setBlockAndUpdate(t.getBlockPos, t.getBlockState.setValue(TankPos.TANK_POS_PROPERTY, TankPos.SINGLE)))
      }
      constructor(seq)
    }
  }
}
