package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fabric.fluid.FabricConverter
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, fluidAccess}
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.transaction.{Transaction, TransactionContext}
import net.minecraft.core.component.DataComponents
import net.minecraft.core.{HolderLookup, RegistryAccess}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.material.Fluids
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.util.Using

//noinspection UnstableApiUsage
class TankFluidItemHandler(tier: Tier, stack: ItemStack) extends SingleFluidStorage {
  readNbt(Option(stack.get(DataComponents.BLOCK_ENTITY_DATA)).map(_.copyTag()).orNull, RegistryAccess.EMPTY)

  override def getCapacity(variant: FluidVariant): Long = {
    getTank.capacity.asFabric
  }

  override def insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val moved = super.insert(insertedVariant, maxAmount, transaction)
    saveTag()
    moved
  }

  override def extract(extractedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val moved = super.extract(extractedVariant, maxAmount, transaction)
    saveTag()
    moved
  }

  private def saveTag(): Unit = {
    CustomData.update(DataComponents.BLOCK_ENTITY_DATA, getContainer, tag => writeNbt(tag, RegistryAccess.EMPTY))
  }

  override def readNbt(@Nullable nbt: CompoundTag, wrapperLookup: HolderLookup.Provider): Unit = {
    val tank = {
      if (nbt == null || !nbt.contains(TileTank.KEY_TANK)) Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity))
      else TankUtil.load(nbt.getCompound(TileTank.KEY_TANK))
    }
    this.variant = FabricConverter.toVariant(tank.content, Fluids.EMPTY)
    this.amount = tank.content.amount.asFabric
  }

  override def writeNbt(@NotNull nbt: CompoundTag, wrapperLookup: HolderLookup.Provider): Unit = {
    val tank = Tank(FabricConverter.fromVariant(this.variant, this.amount), GenericUnit.fromFabric(getCapacity))
    if (!tank.isEmpty) {
      val tankTag = TankUtil.save(tank)
      nbt.put(TileTank.KEY_TANK, tankTag)
      nbt.putString(TileTank.KEY_TIER, tier.name())
    } else {
      nbt.remove(TileTank.KEY_TANK)
      nbt.remove(TileTank.KEY_TIER)
    }
  }

  override def readSnapshot(snapshot: ResourceAmount[FluidVariant]): Unit = {
    super.readSnapshot(snapshot)
    saveTag()
  }

  def getContainer: ItemStack = stack

  def getTank: Tank[FluidLike] = {
    val maybeTank = for {
      blockEntityData <- Option(getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
      if blockEntityData.contains(TileTank.KEY_TANK)
      customTag = blockEntityData.copyTag()
      tankTag <- Option(customTag.getCompound(TileTank.KEY_TANK))
    } yield TankUtil.load(tankTag)

    maybeTank.getOrElse(Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity)))
  }

  def fill(amount: FluidAmount, execute: Boolean): Unit = {
    Using(Transaction.openOuter()) { transaction =>
      this.insert(FabricConverter.toVariant(amount, Fluids.EMPTY), amount.amount.asFabric, transaction)
      if (execute) transaction.commit()
      else transaction.abort()
    }
  }
}
