package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.contents.{GenericAccess, GenericUnit}
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.alchemy.{Potion, PotionContents}
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.material.{Fluid, Fluids}
import org.jetbrains.annotations.VisibleForTesting

import scala.jdk.OptionConverters.RichOption

object FluidAmountUtil {

  final val EMPTY: FluidAmount = from(Fluids.EMPTY, GenericUnit.ZERO)
  final val BUCKET_WATER: FluidAmount = from(Fluids.WATER, GenericUnit.ONE_BUCKET)
  final val BUCKET_LAVA: FluidAmount = from(Fluids.LAVA, GenericUnit.ONE_BUCKET)

  def from(fluid: Fluid, genericUnit: GenericUnit, componentPatch: Option[DataComponentPatch]): FluidAmount = {
    from(FluidLike.of(fluid), genericUnit, componentPatch)
  }

  def from(fluid: Fluid, genericUnit: GenericUnit): FluidAmount = from(fluid, genericUnit, Option.empty)

  def from(fluidLike: FluidLike, genericUnit: GenericUnit, componentPatch: Option[DataComponentPatch]): FluidAmount = {
    implicitly[GenericAccess[FluidLike]].newInstance(fluidLike, genericUnit, componentPatch)
  }

  def from(fluidLike: FluidLike, genericUnit: GenericUnit, componentPatch: DataComponentPatch): FluidAmount = {
    from(fluidLike, genericUnit, Option(componentPatch))
  }

  @VisibleForTesting
  def from(potionType: PotionType, potion: Holder[Potion], genericUnit: GenericUnit): FluidAmount = {
    val componentMap = PotionContents.createItemStack(Items.POTION, potion).getComponentsPatch
    from(FluidLike.of(potionType), genericUnit, componentMap)
  }

  @VisibleForTesting
  def fromItem(stack: ItemStack): FluidAmount = {
    stack.getItem match {
      case Items.WATER_BUCKET => BUCKET_WATER
      case Items.LAVA_BUCKET => BUCKET_LAVA
      case _ => PlatformFluidAccess.getInstance().getFluidContained(stack)
    }
  }

  def fromTag(tag: CompoundTag): FluidAmount = implicitly[GenericAccess[FluidLike]].read(tag)

  /**
   * Helper for Java code
   */
  def access: GenericAccess[FluidLike] = implicitly[GenericAccess[FluidLike]]

  /**
   * Helper for Java code
   */
  def getComponentPatch(amount: FluidAmount): java.util.Optional[DataComponentPatch] = amount.componentPatch.toJava
}
