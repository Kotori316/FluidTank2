package com.kotori316.fluidtank.connection

import cats.data.Chain
import com.kotori316.fluidtank.contents.{GenericAmount, GenericUnit, TanksHandler}
import net.minecraft.core.BlockPos

trait ConnectionHelper[TileType] {
  type Content
  type Handler <: TanksHandler[Content, Chain]
  type ConnectionType <: Connection[TileType]

  def getPos(t: TileType): BlockPos

  def isCreative(t: TileType): Boolean

  def isVoid(t: TileType): Boolean

  /**
   * @param t the tile
   * @return the amount in the tank. None if it contains empty amount.
   */
  final def getContent(t: TileType): Option[GenericAmount[Content]] = {
    val c = getContentRaw(t)
    if (c.isEmpty) Option.empty
    else Option(c)
  }

  /**
   * Get the content in the tank. If the tank contains nothing, returns empty amount.
   *
   * @param t the tile
   * @return the content. Empty amount if the tank contains nothing.
   */
  def getContentRaw(t: TileType): GenericAmount[Content]

  def defaultAmount: GenericAmount[Content]

  def createHandler(s: Seq[TileType]): Handler

  def createConnection(s: Seq[TileType]): ConnectionType

  def connectionSetter(connection: ConnectionType): TileType => Unit
}

object ConnectionHelper {
  type Aux[TileType, ContentType, HandlerType <: TanksHandler[ContentType, Chain]] = ConnectionHelper[TileType] {
    type Content = ContentType
    type Handler = HandlerType
  }

  implicit final class ConnectionHelperMethods[T](private val t: T)(implicit val helper: ConnectionHelper[T]) {

    def getPos: BlockPos = helper.getPos(t)

    def isCreative: Boolean = helper.isCreative(t)

    def isVoid: Boolean = helper.isVoid(t)

    /**
     * @param h implicit parameter of the connection helper, required to get the type of content type.
     *          [[helper]] doesn't have the concrete type info.
     * @return Some(content) if the tank contains valid amount. None if it contains nothing or invalid(amount <= 0) amount.
     */
    def getContent[C](implicit h: ConnectionHelper.Aux[T, C, ?]): Option[GenericAmount[C]] = h.getContent(t)

    def getAmount: GenericUnit = helper.getContentRaw(t).amount
  }
}
