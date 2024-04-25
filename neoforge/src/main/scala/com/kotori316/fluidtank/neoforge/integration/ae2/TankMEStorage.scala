package com.kotori316.fluidtank.neoforge.integration.ae2

/*
import appeng.api.config.Actionable
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.{AEFluidKey, AEKey, KeyCounter}
import appeng.api.storage.MEStorage
import cats.implicits.catsSyntaxEq
import com.kotori316.fluidtank.MCImplicits.*
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.network.chat.Component

case class TankMEStorage(tank: TileTank) extends MEStorage {

  override def getDescription: Component = tank.getName

  override def isPreferredStorageFor(what: AEKey, source: IActionSource): Boolean = {
    what match {
      case key: AEFluidKey =>
        tank.getConnection.getContent.forall { c =>
          c.content === FluidLike.of(key.getFluid) && c.nbt === Option(key.getTag)
        }
      case _ => false
    }
  }

  override def insert(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long = {
    MEStorage.checkPreconditions(what, amount, mode, source)
    what match {
      case key: AEFluidKey =>
        val filled = this.tank.getConnection.getHandler.fill(fromAeFluid(key, amount), !mode.isSimulate)
        filled.amount.asForge
      case _ => 0
    }
  }

  override def extract(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long = {
    MEStorage.checkPreconditions(what, amount, mode, source)
    what match {
      case key: AEFluidKey =>
        val drained = this.tank.getConnection.getHandler.drain(fromAeFluid(key, amount), !mode.isSimulate)
        drained.amount.asForge
      case _ => 0
    }
  }

  override def getAvailableStacks(out: KeyCounter): Unit = {
    this.tank.getConnection.getContent.foreach { c =>
      c.content match {
        case VanillaFluid(fluid) => out.add(AEFluidKey.of(fluid, c.nbt.orNull), c.amount.asDisplay)
        case VanillaPotion(_) =>
      }
    }
  }

  private def fromAeFluid(fluidKey: AEFluidKey, amount: Long): FluidAmount = {
    FluidAmountUtil.from(fluidKey.getFluid, GenericUnit.fromForge(amount), Option(fluidKey.copyTag()))
  }
}*/
