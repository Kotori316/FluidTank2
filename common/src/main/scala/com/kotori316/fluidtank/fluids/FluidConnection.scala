package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.connection.{Connection, ConnectionHelper}
import com.kotori316.fluidtank.contents.{CreativeTank, GenericAmount, VoidTank}
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.material.Fluid

class FluidConnection(s: Seq[TileTank])(override implicit val helper: ConnectionHelper.Aux[TileTank, FluidLike, FluidTanksHandler]) extends Connection[TileTank](s) {
  def getHandler: FluidTanksHandler = this.handler

  def getTextComponent: Component = {
    if (hasCreative)
      Component.translatable("chat.fluidtank.connection_creative",
        getContent.map(c => PlatformFluidAccess.getInstance().getDisplayName(c)).getOrElse(Component.translatable("chat.fluidtank.empty")),
        Int.box(getComparatorLevel))
    else
      Component.translatable("chat.fluidtank.connection",
        getContent.map(c => PlatformFluidAccess.getInstance().getDisplayName(c)).getOrElse(Component.translatable("chat.fluidtank.empty")),
        Long.box(amount.asDisplay),
        Long.box(capacity.asDisplay),
        Int.box(getComparatorLevel))
  }
}

object FluidConnection {
  final val fluidConnectionHelper: ConnectionHelper.Aux[TileTank, FluidLike, FluidTanksHandler] = new FluidConnectionHelper

  private final class FluidConnectionHelper extends ConnectionHelper[TileTank] {
    override type Content = FluidLike
    override type Handler = FluidTanksHandler
    override type ConnectionType = FluidConnection

    override def getPos(t: TileTank): BlockPos = t.getBlockPos

    override def isCreative(t: TileTank): Boolean = t.getTank.isInstanceOf[CreativeTank[?]]

    override def isVoid(t: TileTank): Boolean = t.getTank.isInstanceOf[VoidTank[?]]

    override def getContentRaw(t: TileTank): GenericAmount[FluidLike] = t.getTank.content

    override def defaultAmount: GenericAmount[FluidLike] = FluidAmountUtil.EMPTY

    override def createHandler(s: Seq[TileTank]): FluidTanksHandler = new FluidTanksHandler(s)

    override def createConnection(s: Seq[TileTank]): FluidConnection = {
      if (s.forall(_.hasLevel)) {
        Connection.updatePosPropertyAndCreateConnection[TileTank, FluidConnection](s, s => new FluidConnection(s)(fluidConnectionHelper))
      } else {
        // Maybe in test
        new FluidConnection(s)(fluidConnectionHelper)
      }
    }

    override def connectionSetter(connection: FluidConnection): TileTank => Unit = t => t.setConnection(connection)
  }
}
