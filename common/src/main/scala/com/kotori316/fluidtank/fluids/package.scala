package com.kotori316.fluidtank

import cats.Hash
import com.kotori316.fluidtank.connection.ConnectionHelper
import com.kotori316.fluidtank.contents.{GenericAccess, GenericAmount, GenericUnit}
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.material.Fluid

import scala.reflect.ClassTag

package object fluids {

  type FluidAmount = GenericAmount[FluidLike]
  implicit final val fluidHash: Hash[Fluid] = Hash.fromUniversalHashCode
  implicit final val fluidAccess: GenericAccess[FluidLike] = new FluidAccess
  implicit final val fluidConnectionHelper: ConnectionHelper.Aux[TileTank, FluidLike, FluidTanksHandler] = FluidConnection.fluidConnectionHelper

  private class FluidAccess extends GenericAccess[FluidLike] {
    override def isEmpty(a: FluidLike): Boolean = a == empty

    override def isGaseous(a: FluidLike): Boolean = a.isGaseous

    override def getKey(a: FluidLike): ResourceLocation = a.getKey

    override def fromKey(key: ResourceLocation): FluidLike = FluidLike.fromResourceLocation(key)

    override def empty: FluidLike = FluidLike.FLUID_EMPTY

    override def classTag: ClassTag[FluidLike] = implicitly[ClassTag[FluidLike]]

    override def newInstance(content: FluidLike, amount: GenericUnit, componentMap: Option[DataComponentPatch]): FluidAmount =
      GenericAmount(content, amount, componentMap.filterNot(_.isEmpty))
  }
}
