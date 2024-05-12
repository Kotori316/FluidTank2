package com.kotori316.fluidtank.reservoir

import cats.implicits.catsSyntaxGroup
import com.kotori316.fluidtank.contents.{GenericUnit, Tank}
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, PlatformFluidAccess, PotionType, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.item.PlatformItemAccess
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{Item, ItemStack, ItemUtils, TooltipFlag, UseAnim}
import net.minecraft.world.level.block.BucketPickup
import net.minecraft.world.level.{ClipContext, Level}
import net.minecraft.world.phys.{BlockHitResult, HitResult}
import net.minecraft.world.{InteractionHand, InteractionResultHolder}

import java.util
import java.util.Locale
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.jdk.OptionConverters.RichOptional

class ItemReservoir(val tier: Tier) extends Item(new Item.Properties().stacksTo(1)) {
  override def toString: String = s"ItemReservoir(${tier.name().toLowerCase(Locale.ROOT)})"

  override def getUseAnimation(stack: ItemStack): UseAnim = {
    getTank(stack).content.content match {
      case v: VanillaPotion if v.potionType == PotionType.NORMAL => UseAnim.DRINK
      case _ => super.getUseAnimation(stack)
    }
  }

  override def getUseDuration(stack: ItemStack): Int = {
    getTank(stack).content.content match {
      case v: VanillaPotion if v.potionType == PotionType.NORMAL => 32 // See PotionItem
      case _ => super.getUseDuration(stack)
    }
  }

  override def use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val stack = player.getItemInHand(usedHand)
    val content = getTank(stack).content
    content.content match {
      case v: VanillaPotion if v.potionType == PotionType.NORMAL => ItemUtils.startUsingInstantly(level, player, usedHand);
      case _: VanillaFluid =>
        val hitResult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY)
        if (hitResult.getType != HitResult.Type.BLOCK) {
          InteractionResultHolder.pass(stack)
        } else {
          fillOrDrainFluidInLevel(hitResult, stack, level, player, content, usedHand)
        }
      case _ => super.use(level, player, usedHand)
    }
  }

  override def finishUsingItem(stack: ItemStack, level: Level, livingEntity: LivingEntity): ItemStack = {
    val tank = getTank(stack)
    val content = tank.content
    content.content match {
      case _: VanillaPotion if content.hasOneBottle =>
        for {
          patch <- content.componentPatch.iterator
          content <- Option(patch.get(DataComponents.POTION_CONTENTS)).flatMap(_.toScala).iterator
          e <- content.getAllEffects.asScala
        } {
          if (e.getEffect.value().isInstantenous) {
            e.getEffect.value().applyInstantenousEffect(livingEntity, livingEntity, livingEntity, e.getAmplifier, 1.0)
          } else {
            livingEntity.addEffect(e)
          }
        }
        val newTank = tank.copy(content = content.setAmount(content.amount |-| GenericUnit.ONE_BOTTLE))
        this.saveTank(stack, newTank)
        stack
      case _ => stack
    }
  }

  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], isAdvanced: TooltipFlag): Unit = {
    super.appendHoverText(stack, context, tooltip, isAdvanced)
    val tank = getTank(stack)
    if (tank.isEmpty) {
      tooltip.add(Component.translatable("fluidtank.waila.capacity", GenericUnit.asForgeFromBigInt(tier.getCapacity)))
    } else {
      val fluid = tank.content
      val capacity = tank.capacity
      tooltip.add(Component.translatable("fluidtank.waila.short",
        PlatformFluidAccess.getInstance().getDisplayName(fluid), fluid.amount.asDisplay, capacity.asDisplay))
    }
  }

  def getTank(stack: ItemStack): Tank[FluidLike] = {
    stack.getOrDefault(PlatformItemAccess.getInstance().fluidTankComponentType(), Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity)))
  }

  def saveTank(stack: ItemStack, tank: Tank[FluidLike]): Unit = {
    if (tank.isEmpty) {
      stack.remove(PlatformItemAccess.getInstance().fluidTankComponentType())
    } else {
      stack.set(PlatformItemAccess.getInstance().fluidTankComponentType(), tank)
    }
  }

  private def fillOrDrainFluidInLevel(hitResult: BlockHitResult, stack: ItemStack, level: Level, player: Player, content: FluidAmount, hand: InteractionHand): InteractionResultHolder[ItemStack] = {
    val hitPos = hitResult.getBlockPos
    val hitFace = hitResult.getDirection
    if (level.mayInteract(player, hitPos) && player.mayUseItemAt(hitPos.relative(hitFace), hitFace, stack)) {
      val blockState = level.getBlockState(hitPos)
      val simulateFluid = FluidAmountUtil.from(level.getFluidState(hitPos).getType, GenericUnit.ONE_BUCKET)
      blockState.getBlock match {
        case pickUp: BucketPickup if content.isEmpty || content.contentEqual(simulateFluid) =>
          val simulation = PlatformFluidAccess.getInstance().fillItem(simulateFluid, stack, player, hand, false)
          if (simulation.moved.nonEmpty) {
            val picked = pickUp.pickupBlock(null, level, hitPos, blockState)
            val actualFluid = PlatformFluidAccess.getInstance().getFluidContained(picked)
            val result = PlatformFluidAccess.getInstance().fillItem(actualFluid, stack, player, hand, true)
            InteractionResultHolder.sidedSuccess(result.toReplace, level.isClientSide)
          } else {
            InteractionResultHolder.pass(stack)
          }
        case _ =>
          InteractionResultHolder.pass(stack)
      }
    } else {
      InteractionResultHolder.fail(stack)
    }
  }
}
