package com.kotori316.fluidtank.potions

import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, PotionType}
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.alchemy.{PotionContents, Potions}
import net.minecraft.world.item.{ItemStack, Items}
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{DynamicNode, DynamicTest, Nested, Test, TestFactory}

import java.util.Optional

class PotionFluidHandlerTest extends BeforeMC {

  @TestFactory
  def instanceForNonPotions(): Array[DynamicNode] = {
    val t = for {
      i <- Seq(Items.AIR, Items.STONE, Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.MILK_BUCKET)
      stack = new ItemStack(i)
    } yield DynamicTest.dynamicTest(i.getClass.getName, () => {
      val handler = assertDoesNotThrow(() => PotionFluidHandler(stack))
      assertAll(
        () => assertNotNull(handler),
        () => assertEquals(FluidAmountUtil.EMPTY, handler.getContent),
      )
    })
    t.toArray
  }

  @Test
  def instanceForEmptyBottle(): Unit = {
    val stack = new ItemStack(Items.GLASS_BOTTLE)
    val handler = assertDoesNotThrow(() => PotionFluidHandler(stack))

    assertNotNull(handler)
    assertEquals(FluidAmountUtil.EMPTY, handler.getContent)
  }

  @Test
  def dummy(): Unit = {
    assertFalse(PotionFluidHandler.handlerProvider.isEmpty)
  }

  def composedPotion = new PotionContents(Optional.of(Potions.LONG_NIGHT_VISION), Optional.empty(), Potions.LONG_WATER_BREATHING.value().getEffects, Optional.empty())

  @Nested
  class FillTest {
    val toFill: FluidAmount = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BOTTLE)

    @Test
    def fillNoContainer(): Unit = {
      val stack = new ItemStack(Items.BUCKET)
      val handler = PotionFluidHandler(stack)
      val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(stack, result.toReplace), s"Items must match, $stack, ${result.toReplace}"),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved())
      )
    }

    @Test
    def fillEmptyBottle(): Unit = {
      val stack = new ItemStack(Items.GLASS_BOTTLE)
      val handler = PotionFluidHandler(stack)
      val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(result.toReplace, PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY))),
        () => assertEquals(toFill, result.moved())
      )
    }

    @Test
    def fillFilledBottle(): Unit = {
      val stack = PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY)
      val handler = PotionFluidHandler(stack)
      val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(stack, result.toReplace)),
        () => assertFalse(result.shouldMove(), "Filling failed, so shouldMove should be false"),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
      )
    }

    @Test
    def fillDifferentPotion(): Unit = {
      val stack = PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION)
      val handler = PotionFluidHandler(stack)
      val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(stack, result.toReplace)),
        () => assertFalse(result.shouldMove(), "Filling failed, so shouldMove should be false"),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
      )
    }

    @Test
    def fillMultiEffect(): Unit = {
      val stack = new ItemStack(Items.GLASS_BOTTLE)
      val handler = PotionFluidHandler(stack)
      val expectedStack = new ItemStack(Items.POTION)
      expectedStack.set(DataComponents.POTION_CONTENTS, composedPotion)
      val toFill = FluidAmountUtil.from(FluidLike.of(PotionType.NORMAL), GenericUnit.ONE_BOTTLE, Option(expectedStack.getComponentsPatch))

      val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertEquals(toFill, result.moved()),
        () => assertTrue(result.shouldMove()),
        () => assertTrue(ItemStack.matches(expectedStack, result.toReplace),
          s"Expect: ${expectedStack.getComponents}, Result: ${result.toReplace.getComponents}"),
      )
    }
  }

  @Nested
  class DrainTest {
    val toDrain: FluidAmount = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BOTTLE)

    @Test
    def drainNoContainer(): Unit = {
      val stack = new ItemStack(Items.BUCKET)
      val handler = PotionFluidHandler(stack)
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(stack, result.toReplace), s"Items must match, $stack, ${result.toReplace}"),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved())
      )
    }

    @Test
    def drainEmptyBottle(): Unit = {
      val stack = new ItemStack(Items.GLASS_BOTTLE)
      val handler = PotionFluidHandler(stack)
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(result.toReplace, stack)),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved())
      )
    }

    @Test
    def drainPotionSuccess(): Unit = {
      val stack = PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY)
      val handler = PotionFluidHandler(stack)
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(result.shouldMove(), "Drain succeeded, so shouldMove should be true"),
        () => assertTrue(ItemStack.matches(new ItemStack(Items.GLASS_BOTTLE), result.toReplace)),
        () => assertEquals(toDrain, result.moved()),
      )
    }

    @Test
    def drainPotionFail(): Unit = {
      val stack = PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION)
      val handler = PotionFluidHandler(stack)
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertTrue(ItemStack.matches(result.toReplace, stack)),
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
        () => assertFalse(result.shouldMove()),
      )
    }

    @Test
    def drainMultiEffectSuccess(): Unit = {
      val stack = new ItemStack(Items.POTION)
      stack.set(DataComponents.POTION_CONTENTS, composedPotion)

      val handler = PotionFluidHandler(stack)
      val toDrain = FluidAmountUtil.from(FluidLike.of(PotionType.NORMAL), GenericUnit.ONE_BOTTLE, Option(stack.getComponentsPatch))
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertEquals(toDrain, result.moved()),
        () => assertTrue(result.shouldMove()),
        () => assertTrue(ItemStack.matches(new ItemStack(Items.GLASS_BOTTLE), result.toReplace)),
      )
    }

    @Test
    def drainMultiEffectFail1(): Unit = {
      val stack = new ItemStack(Items.POTION)
      stack.set(DataComponents.POTION_CONTENTS, composedPotion)
      val handler = PotionFluidHandler(stack)
      val toDrain = FluidAmountUtil.from(PotionType.NORMAL, Potions.LONG_NIGHT_VISION, GenericUnit.ONE_BOTTLE)
      val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))

      assertAll(
        () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
        () => assertFalse(result.shouldMove()),
        () => assertTrue(ItemStack.matches(stack, result.toReplace)),
      )
    }
  }

  @Nested
  class GetContentTest {
    @TestFactory
    def potions(): Array[DynamicNode] = {
      for {
        i <- Array(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)
        potion <- Array(Potions.INVISIBILITY, Potions.WATER, Potions.NIGHT_VISION, Potions.LONG_NIGHT_VISION)
      } yield {
        val name = potion.value().name()
        DynamicTest.dynamicTest(s"${i}_$name", () => {
          val stack = PotionContents.createItemStack(i, potion)
          val handler = PotionFluidHandler(stack)
          val expected = FluidAmountUtil.from(PotionType.fromItemUnsafe(i), potion, GenericUnit.ONE_BOTTLE)

          assertEquals(expected, handler.getContent)
        })
      }
    }

    @Test
    def multiEffectTurtle(): Unit = {
      val stack = PotionContents.createItemStack(Items.POTION, Potions.STRONG_TURTLE_MASTER)
      val handler = PotionFluidHandler(stack)
      val expected = FluidAmountUtil.from(PotionType.NORMAL, Potions.STRONG_TURTLE_MASTER, GenericUnit.ONE_BOTTLE)

      assertEquals(expected, handler.getContent)
    }

    @Test
    def multiEffectUserDefine(): Unit = {
      val stack = new ItemStack(Items.POTION)
      stack.set(DataComponents.POTION_CONTENTS, composedPotion)
      val handler = PotionFluidHandler(stack)
      val content = handler.getContent
      val expected = FluidAmountUtil.from(FluidLike.of(PotionType.NORMAL), GenericUnit.ONE_BOTTLE, Option(stack.getComponentsPatch))

      assertEquals(expected, content)
    }
  }
}
