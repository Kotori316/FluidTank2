package com.kotori316.fluidtank.neoforge.gametest

import com.kotori316.fluidtank.config.PlatformConfigAccess
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, PlatformFluidAccess, PotionType}
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.fluidtank.{FluidTankCommon, PlatformAccess}
import com.kotori316.testutil.GameTestUtil
import com.mojang.serialization.JsonOps
import net.minecraft.core.Holder
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.alchemy.{Potion, PotionContents, Potions}
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.GameType
import net.neoforged.neoforge.gametest.GameTestHolder
import org.junit.jupiter.api.Assertions.*

import java.util.Locale
import scala.jdk.OptionConverters.RichOption
import scala.jdk.javaapi.CollectionConverters

//noinspection ScalaUnusedSymbol
@GameTestHolder(FluidTankCommon.modId)
class PlatformAccessTest {
  private final val ACCESS: PlatformAccess = PlatformFluidAccess.getInstance().asInstanceOf[PlatformAccess]
  private final val BATCH_NAME = "platform_test"

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH_NAME)
  }

  def configAccess(helper: GameTestHelper): Unit = {
    assertNotNull(PlatformConfigAccess.getInstance())
    assertDoesNotThrow(() => PlatformConfigAccess.getInstance().getConfig)
    helper.succeed()
  }

  def fillBucketWater(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.BUCKET)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.fillItem(FluidAmountUtil.BUCKET_WATER, stack, player, InteractionHand.MAIN_HAND, true)
    assertEquals(FluidAmountUtil.BUCKET_WATER, transferred.moved)
    assertEquals(Items.WATER_BUCKET, transferred.toReplace.getItem, "Transfer result")

    helper.succeed()
  }

  def fillBucketLava(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.BUCKET)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.fillItem(FluidAmountUtil.BUCKET_LAVA, stack, player, InteractionHand.MAIN_HAND, true)
    assertTrue(transferred.shouldMove, "Forge module didn't move items")
    assertEquals(FluidAmountUtil.BUCKET_LAVA, transferred.moved)
    assertEquals(Items.LAVA_BUCKET, transferred.toReplace.getItem, "Transfer result")

    helper.succeed()
  }

  def drainWaterBucket(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.WATER_BUCKET)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.drainItem(FluidAmountUtil.BUCKET_WATER, stack, player, InteractionHand.MAIN_HAND, true)
    assertTrue(transferred.shouldMove, "Forge module didn't move items")
    assertEquals(FluidAmountUtil.BUCKET_WATER, transferred.moved)
    assertEquals(Items.BUCKET, transferred.toReplace.getItem, "Transfer result")

    helper.succeed()
  }

  def drainLavaBucket(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.LAVA_BUCKET)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.drainItem(FluidAmountUtil.BUCKET_LAVA, stack, player, InteractionHand.MAIN_HAND, true)
    assertTrue(transferred.shouldMove, "Forge module didn't move items")
    assertEquals(FluidAmountUtil.BUCKET_LAVA, transferred.moved)
    assertEquals(Items.BUCKET, transferred.toReplace.getItem, "Transfer result")

    helper.succeed()
  }

  private def potionFluid(potionType: PotionType, potion: Holder[Potion]): FluidAmount = {
    FluidAmountUtil.from(potionType, potion, GenericUnit.ONE_BOTTLE)
  }

  def fillFail1(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.BUCKET)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.drainItem(potionFluid(PotionType.NORMAL, Potions.WATER), stack, player, InteractionHand.MAIN_HAND, true)

    assertTrue(transferred.moved.isEmpty, "Nothing moved")
    assertEquals(Items.BUCKET, transferred.toReplace.getItem, "Transfer result")
    assertEquals(Items.BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem, "Player item")

    helper.succeed()
  }

  def fillFail2(helper: GameTestHelper): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.GLASS_BOTTLE)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val transferred = ACCESS.drainItem(FluidAmountUtil.BUCKET_WATER, stack, player, InteractionHand.MAIN_HAND, true)

    assertTrue(transferred.moved.isEmpty, "Nothing moved")
    assertEquals(Items.GLASS_BOTTLE, transferred.toReplace.getItem, "Transfer result")
    assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem, "Player item")

    helper.succeed()
  }

  private def potions(): Seq[(PotionType, Holder[Potion])] = {
    for {
      t <- PotionType.values().toSeq
      p <- Seq(Potions.WATER, Potions.AWKWARD, Potions.NIGHT_VISION, Potions.LONG_NIGHT_VISION)
    } yield (t, p)
  }

  @GameTestGenerator
  def fillPotion(): java.util.List[TestFunction] = CollectionConverters.asJava(
    potions().map { case (potionType, potion) =>
      GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH_NAME,
        "fill_potion_%s_%s".formatted(potionType.name(), Potion.getName(Option(potion).toJava, "")).toLowerCase(Locale.ROOT),
        GameTestUtil.NO_PLACE_STRUCTURE,
        g => fillPotion(g, potionType, potion))
    }
  )

  @GameTestGenerator
  def fillFailPotionWithAmount(): java.util.List[TestFunction] = CollectionConverters.asJava(
    potions().map { case (potionType, potion) =>
      GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH_NAME,
        "fill_fail1_potion_%s_%s".formatted(potionType.name(), Potion.getName(Option(potion).toJava, "")).toLowerCase(Locale.ROOT),
        GameTestUtil.NO_PLACE_STRUCTURE,
        g => fillFailPotionWithAmount(g, potionType, potion))
    }
  )

  @GameTestGenerator
  def drainPotion(): java.util.List[TestFunction] = CollectionConverters.asJava(
    potions().map { case (potionType, potion) =>
      GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH_NAME,
        "drain_potion_%s_%s".formatted(potionType.name(), Potion.getName(Option(potion).toJava, "")).toLowerCase(Locale.ROOT),
        GameTestUtil.NO_PLACE_STRUCTURE,
        g => drainPotion(g, potionType, potion))
    }
  )

  @GameTestGenerator
  def drainFailPotionWithAmount(): java.util.List[TestFunction] = CollectionConverters.asJava(
    potions().map { case (potionType, potion) =>
      GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH_NAME,
        "drain_fail1_potion_%s_%s".formatted(potionType.name(), Potion.getName(Option(potion).toJava, "")).toLowerCase(Locale.ROOT),
        GameTestUtil.NO_PLACE_STRUCTURE,
        g => drainFailPotionWithAmount(g, potionType, potion))
    }
  )

  private def fillPotion(helper: GameTestHelper, potionType: PotionType, potion: Holder[Potion]): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.GLASS_BOTTLE)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val toFill = potionFluid(potionType, potion)
    val transferred = ACCESS.fillItem(toFill, stack, player, InteractionHand.MAIN_HAND, true)
    assertTrue(transferred.shouldMove, "Forge module didn't move items")
    assertEquals(toFill, transferred.moved)
    val expected = PotionContents.createItemStack(potionType.getItem, potion)
    assertTrue(ItemStack.isSameItemSameComponents(expected, transferred.toReplace), "transferred, Ex: %s, Ac: %s".formatted(expected.getComponents, transferred.toReplace.getComponents))
    helper.succeed()
  }

  private def fillFailPotionWithAmount(helper: GameTestHelper, potionType: PotionType, potion: Holder[Potion]): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = new ItemStack(Items.GLASS_BOTTLE)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val toFill = potionFluid(potionType, potion).setAmount(GenericUnit.fromFabric(26999))
    val transferred = ACCESS.fillItem(toFill, stack, player, InteractionHand.MAIN_HAND, true)
    assertFalse(transferred.shouldMove, "Transfer failed, so nothing to move")
    assertTrue(transferred.moved.isEmpty)
    val expected = new ItemStack(Items.GLASS_BOTTLE)
    assertTrue(ItemStack.isSameItemSameComponents(expected, transferred.toReplace), "transferred, Ex: %s, Ac: %s".formatted(expected.getComponents, transferred.toReplace.getComponents))
    helper.succeed()
  }

  private def drainPotion(helper: GameTestHelper, potionType: PotionType, potion: Holder[Potion]): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = PotionContents.createItemStack(potionType.getItem, potion)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val toDrain = potionFluid(potionType, potion)
    val transferred = ACCESS.drainItem(toDrain, stack, player, InteractionHand.MAIN_HAND, true)
    assertTrue(transferred.shouldMove, "Forge module didn't move items")
    assertEquals(toDrain, transferred.moved)
    val expected = Items.GLASS_BOTTLE.getDefaultInstance
    assertTrue(ItemStack.isSameItemSameComponents(expected, transferred.toReplace), "transferred, Ex: %s, Ac: %s".formatted(expected.getComponents, transferred.toReplace.getComponents))
    helper.succeed()
  }

  private def drainFailPotionWithAmount(helper: GameTestHelper, potionType: PotionType, potion: Holder[Potion]): Unit = {
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    val stack = PotionContents.createItemStack(potionType.getItem, potion)
    val expected = stack.copy
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val toDrain = potionFluid(potionType, potion).setAmount(GenericUnit.fromFabric(26999))
    val transferred = ACCESS.drainItem(toDrain, stack, player, InteractionHand.MAIN_HAND, true)
    assertFalse(transferred.shouldMove(), "Transfer failed, so nothing to move")
    assertTrue(transferred.moved.isEmpty)
    assertTrue(ItemStack.isSameItemSameComponents(expected, transferred.toReplace), "transferred, Ex: %s, Ac: %s".formatted(expected.getComponents, transferred.toReplace.getComponents))
    helper.succeed()
  }

  def serializeTankItem(helper: GameTestHelper): Unit = {
    val stack = RecipeInventoryUtil.getFilledTankStack(Tier.GOLD, FluidAmountUtil.BUCKET_WATER)
    val result = assertDoesNotThrow(() => ItemStack.STRICT_CODEC.encodeStart(JsonOps.INSTANCE, stack))
    assertTrue(result.isSuccess)
    helper.succeed()
  }
}
