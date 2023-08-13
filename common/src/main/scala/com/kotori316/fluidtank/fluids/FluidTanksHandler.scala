package com.kotori316.fluidtank.fluids

import cats.data.Chain
import cats.implicits.catsSyntaxFoldableOps0
import com.kotori316.fluidtank.contents.{ChainTanksHandler, FluidTransferLog, Tank}
import com.kotori316.fluidtank.tank.TileTank

class FluidTanksHandler(s: Seq[TileTank]) extends ChainTanksHandler[FluidLike](true) {
  this.tanks = Chain.fromSeq(s.map(_.getTank))

  override def updateTanks(newTanks: Chain[Tank[FluidLike]]): Unit = {
    super.updateTanks(newTanks)
    s.zip(newTanks.toList).foreach {
      case (tile, value) => tile.setTank(value)
    }
  }

  def getTank: Chain[Tank[FluidLike]] = this.tanks

  override protected def outputLog(logs: Chain[FluidTransferLog], execute: Boolean): Unit = {
    if (DebugLogging.ENABLED && execute) {
      val (pos, dim) = if (s.isEmpty) {
        ("Empty Pos", "Void")
      } else {
        val p = s.head.getBlockPos
        val d = if (s.head.hasLevel) s.head.getLevel.dimension().location().getPath else "Void"
        (s"${p.getX},${p.getY},${p.getZ}", d)
      }
      DebugLogging.LOGGER.debug("({},{}); {}", dim, pos, logs.mkString_("; "))
    }
  }
}
