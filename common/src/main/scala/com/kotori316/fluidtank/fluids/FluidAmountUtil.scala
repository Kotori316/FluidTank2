package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.contents.{GenericAccess, GenericAmount, GenericUnit}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.alchemy.{Potion, PotionUtils}
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.material.{Fluid, Fluids}
import org.jetbrains.annotations.VisibleForTesting

import scala.jdk.OptionConverters.RichOption

object FluidAmountUtil {

  final val EMPTY: FluidAmount = from(Fluids.EMPTY, GenericUnit.ZERO)
  final val BUCKET_WATER: FluidAmount = from(Fluids.WATER, GenericUnit.ONE_BUCKET)
  final val BUCKET_LAVA: FluidAmount = from(Fluids.LAVA, GenericUnit.ONE_BUCKET)

  def from(fluid: Fluid, genericUnit: GenericUnit, nbt: Option[CompoundTag]): FluidAmount = {
    from(FluidLike.of(fluid), genericUnit, nbt)
  }

  def from(fluid: Fluid, genericUnit: GenericUnit): FluidAmount = from(fluid, genericUnit, Option.empty)

  def from(fluidLike: FluidLike, genericUnit: GenericUnit, nbt: Option[CompoundTag]): FluidAmount = {
    GenericAmount(fluidLike, genericUnit, nbt)
  }

  @VisibleForTesting
  def from(potionType: PotionType, potion: Potion, genericUnit: GenericUnit): FluidAmount = {
    val tag = PotionUtils.setPotion(new ItemStack(Items.POTION), potion).getTag
    from(FluidLike.of(potionType), genericUnit, Option(tag))
  }

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

  def getTag(amount: FluidAmount): java.util.Optional[CompoundTag] = amount.nbt.toJava
}
