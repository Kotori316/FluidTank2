package com.kotori316.fluidtank.connection

import cats.implicits.toFoldableOps
import com.kotori316.fluidtank.connection.ConnectionHelper.ConnectionHelperMethods
import com.kotori316.fluidtank.contents.{ChainTanksHandler, GenericAmount, GenericUnit}
import net.minecraft.util.Mth

import scala.collection.mutable.ArrayBuffer
import scala.math.Ordering.Implicits.infixOrderingOps

abstract class Connection[TileType] protected(val sortedTanks: Seq[TileType]) {
  implicit val helper: ConnectionHelper[TileType]

  val hasCreative: Boolean = sortedTanks.exists(_.isCreative)
  val hasVoid: Boolean = sortedTanks.exists(_.isVoid)
  val updateActions: ArrayBuffer[() => Unit] = ArrayBuffer(
    () => this.sortedTanks.foreach(_.setChanged())
  )
  val isDummy: Boolean = false
  protected var isValid: Boolean = true

  protected val handler: helper.Handler = helper.createHandler(this.sortedTanks)

  // Assuming head or last tank contains the content.
  protected def contentType: GenericAmount[helper.Content] =
    this.sortedTanks.headOption.flatMap(t => helper.getContent(t))
      .orElse(this.sortedTanks.lastOption.flatMap(t => helper.getContent(t)))
      .getOrElse(helper.defaultAmount)

  def capacity: GenericUnit = handler.getSumOfCapacity

  def amount: GenericUnit = this.sortedTanks.foldMap(_.getAmount)

  def getContent: Option[GenericAmount[helper.Content]] =
    Option(contentType).filter(_.nonEmpty).map(_.setAmount(this.amount))

  def remove(tank: TileType): Unit = {
    val (s1, s2) = this.sortedTanks.span(_ != tank)
    val s1Connection = this.helper.createConnection(s1)
    val s2Connection = this.helper.createConnection(s2.tail)
    this.invalidate()

    s1.foreach(this.helper.connectionSetter(s1Connection))
    s2.tail.foreach(this.helper.connectionSetter(s2Connection))
  }

  protected def invalidate(): Unit = {
    this.isValid = false
  }

  def getComparatorLevel: Int = {
    if (amount > GenericUnit.ZERO)
      Mth.floor(amount.asForgeDouble / capacity.asForgeDouble * 14) + 1
    else 0
  }

  def updateNeighbors(): Unit = {
    updateActions.foreach(_.apply())
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
        c.forall(t => t contentEqual kind)
      }
      require(s1.map(_.getContent).forall(c => c.forall(t => t contentEqual kind)))
      val connection = helper.createConnection(s1)
      // Safe cast
      val contentType = kind.setAmount(GenericUnit.MAX).asInstanceOf[GenericAmount[connection.helper.Content]]
      val content = connection.handler.drain(contentType, execute = true)
      connection.handler.fill(content, execute = true)
      s1 foreach helper.connectionSetter(connection)

      if (s2.nonEmpty) createAndInit(s2)
    }
  }
}