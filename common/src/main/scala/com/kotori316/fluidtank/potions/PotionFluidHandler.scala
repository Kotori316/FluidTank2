package com.kotori316.fluidtank.potions

import com.kotori316.fluidtank.contents.{GenericAmount, GenericUnit}
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, PlatformFluidAccess, PotionType, VanillaPotion}
import com.kotori316.fluidtank.reservoir.{ItemReservoir, ReservoirPotionFluidHandler}
import net.minecraft.world.item.{ItemStack, Items, PotionItem}

trait PotionFluidHandler {

  /**
   * This method just simulates the result. You need to replace the item and fluid in caller method
   *
   * @param toFill        the fluid(must be potion) to be filled
   * @param vanillaPotion the potion instance. You need to ensure the toFill contains potion and get the VanillaPotion instance
   * @return TransferStack referring the moved amount and filled container
   */
  def fill(toFill: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack

  /**
   * This method just simulates the result. You need to replace the item and fluid in caller method
   *
   * @param toDrain       the fluid(must be potion) to be drained. The amount is checked in this method.
   * @param vanillaPotion the potion instance, This is here to check the type before calling this method.
   * @return TransferStack referring the moved amount and drained container
   */
  def drain(toDrain: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack

  /**
   * Get the content in this handler
   *
   * @return the holding potion
   */
  def getContent: GenericAmount[FluidLike]

  def isValidHandler: Boolean = true
}

object PotionFluidHandler {

  final val handlerProvider: scala.collection.mutable.Seq[PartialFunction[ItemStack, PotionFluidHandler]] = scala.collection.mutable.Seq(
    s => s.getItem match {
      case Items.GLASS_BOTTLE => new VanillaEmptyBottle(s)
      case _: PotionItem => new VanillaPotionBottle(s)
      case itemReservoir: ItemReservoir => new ReservoirPotionFluidHandler(itemReservoir, s)
    }
  )

  def apply(stack: ItemStack): PotionFluidHandler = {
    handlerProvider.reduce(_ orElse _)
      .applyOrElse(stack, s => new NotContainer(s))
  }

  private def transferStack(moved: FluidAmount, stack: ItemStack): PlatformFluidAccess.TransferStack =
    new PlatformFluidAccess.TransferStack(moved, stack)

  private def transferFailed(stack: ItemStack): PlatformFluidAccess.TransferStack =
    new PlatformFluidAccess.TransferStack(FluidAmountUtil.EMPTY, stack, false)

  private class NotContainer(stack: ItemStack) extends PotionFluidHandler {
    override def fill(toFill: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack =
      transferFailed(stack)

    override def drain(toDrain: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack =
      transferFailed(stack)

    override def getContent: GenericAmount[FluidLike] = FluidAmountUtil.EMPTY

    override def isValidHandler: Boolean = false
  }

  private class VanillaEmptyBottle(stack: ItemStack) extends PotionFluidHandler {
    override def fill(toFill: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack = {
      if (toFill.hasOneBottle) {
        val filledItem = new ItemStack(vanillaPotion.potionType.getItem)
        toFill.componentPatch.foreach(c => filledItem.applyComponents(c))
        val moved = toFill.setAmount(GenericUnit.ONE_BOTTLE)
        transferStack(moved, filledItem)
      } else {
        transferFailed(stack)
      }
    }

    override def drain(toDrain: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack = {
      // Nothing can be drained from empty bottle
      transferFailed(stack)
    }

    override def getContent: GenericAmount[FluidLike] = FluidAmountUtil.EMPTY
  }

  private class VanillaPotionBottle(stack: ItemStack) extends PotionFluidHandler {
    override def fill(toFill: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack = {
      // Nothing can be filled into the potion
      transferFailed(stack)
    }

    override def drain(toDrain: FluidAmount, vanillaPotion: VanillaPotion): PlatformFluidAccess.TransferStack = {
      if (toDrain.isEmpty || !toDrain.hasOneBottle) return transferFailed(stack)

      val content = getContent
      if (!content.hasOneBottle || !toDrain.contentEqual(content)) return transferFailed(stack)

      val drainedItem = Items.GLASS_BOTTLE.getDefaultInstance
      val moved = toDrain.setAmount(GenericUnit.ONE_BOTTLE)
      transferStack(moved, drainedItem)
    }

    override def getContent: GenericAmount[FluidLike] = {
      val potionType = PotionType.fromItemUnsafe(stack.getItem)
      val componentMap = stack.getComponentsPatch
      FluidAmountUtil.from(FluidLike.of(potionType), GenericUnit.ONE_BOTTLE, componentMap)
    }
  }
}
