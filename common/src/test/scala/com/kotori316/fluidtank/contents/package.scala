package com.kotori316.fluidtank

import cats.data.Chain
import cats.{Monad, MonoidK}
import net.minecraft.resources.ResourceLocation

import scala.reflect.ClassTag

package object contents {
  implicit final val gaString: GenericAccess[String] = new StringGenericAccess

  def createTanks(settings: (String, Long, Long)*): Chain[Tank[String]] = {
    val monoidK = implicitly[MonoidK[Chain]]
    val monad = implicitly[Monad[Chain]]
    monoidK.combineAllK(settings.map { case (value, l, l1) => Tank(GenericAmount(value, GenericUnit(l), None), GenericUnit(l1)) }
      .map(t => monad.pure(t)))
  }

  private final class StringGenericAccess extends GenericAccess[String] {
    override def isEmpty(a: String): Boolean = a.isEmpty

    override def isGaseous(a: String): Boolean = a.contains("gas")

    override def getKey(a: String): ResourceLocation = new ResourceLocation(FluidTankCommon.modId, a)

    override def fromKey(key: ResourceLocation): String = key.getPath

    override def asString(a: String): String = a

    override def empty: String = ""

    override def classTag: ClassTag[String] = implicitly[ClassTag[String]]

    override def newInstance(content: String, amount: GenericUnit, componentMap: Option[DataComponentPatch]): GenericAmount[String] =
      GenericAmount(content, amount, componentMap)
  }
}
