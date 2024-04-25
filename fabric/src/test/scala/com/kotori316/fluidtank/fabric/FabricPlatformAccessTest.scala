package com.kotori316.fluidtank.fabric

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, FluidLike, PotionType}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.alchemy.{Potion, PotionContents, Potions}
import net.minecraft.world.item.{ItemStack, Items}
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.{DynamicTest, Nested, Test, TestFactory}

import java.util
import scala.jdk.OptionConverters.RichOption
import scala.jdk.javaapi.CollectionConverters

final class FabricPlatformAccessTest extends BeforeMC {
  val ACCESS = new FabricPlatformAccess

  @Nested
  class FluidContentTest {
    @Test
    def waterBucket(): Unit = {
      val fluid = ACCESS.getFluidContained(new ItemStack(Items.WATER_BUCKET))
      assertEquals(FluidAmountUtil.BUCKET_WATER, fluid)
    }

    @Test
    def lavaBucket(): Unit = {
      val fluid = ACCESS.getFluidContained(new ItemStack(Items.LAVA_BUCKET))
      assertEquals(FluidAmountUtil.BUCKET_LAVA, fluid)
    }

    @Test
    def emptyBucket(): Unit = {
      val fluid = ACCESS.getFluidContained(new ItemStack(Items.BUCKET))
      assertEquals(FluidAmountUtil.EMPTY, fluid)
    }

    @Test
    def emptyBottle(): Unit = {
      val fluid = ACCESS.getFluidContained(new ItemStack(Items.GLASS_BOTTLE))
      assertEquals(FluidAmountUtil.EMPTY, fluid)
    }

    @TestFactory
    def getPotion: util.List[DynamicTest] = {
      CollectionConverters.asJava(for {
        p <- Seq(Potions.WATER, Potions.AWKWARD, Potions.NIGHT_VISION)
        i <- Seq(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)
        name = i.toString + " " + Potion.getName(Option(p).toJava, "")
      } yield DynamicTest.dynamicTest(name, () => {
        val potion = PotionContents.createItemStack(i, p)
        val fluid = ACCESS.getFluidContained(potion)
        val expected = FluidAmountUtil.from(FluidLike.of(PotionType.fromItemUnsafe(i)), GenericUnit.ONE_BOTTLE, potion.getComponentsPatch)
        assertEquals(expected, fluid)
      }))
    }
  }

  @Nested
  class IsContainerTest {
    @TestFactory
    def containers: util.List[DynamicTest] = {
      CollectionConverters.asJava(for {
        s <- Seq.concat(
          Seq(Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.GLASS_BOTTLE).map(i => new ItemStack(i)),
          Seq(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION).flatMap { i =>
            CollectionConverters.asScala(BuiltInRegistries.POTION.stream().iterator())
              .map(p => PotionContents.createItemStack(i, BuiltInRegistries.POTION.wrapAsHolder(p)))
          }
        )
        name = s.toString + " " + s.getComponentsPatch
      } yield DynamicTest.dynamicTest(name, () => assertTrue(ACCESS.isFluidContainer(s))))
    }
  }
}
