package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.{BeforeMC, FluidTankCommon}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidLikeTest extends BeforeMC {
  @Nested
  class FromNameTest {
    @TestFactory
    def test(): Array[DynamicNode] = {
      val expected = Seq(
        "minecraft:empty" -> FluidLike.FLUID_EMPTY,
        "minecraft:water" -> FluidLike.FLUID_WATER,
        "minecraft:lava" -> FluidLike.FLUID_LAVA,
        s"${FluidTankCommon.modId}:potion_normal" -> FluidLike.POTION_NORMAL,
        s"${FluidTankCommon.modId}:potion_splash" -> FluidLike.POTION_SPLASH,
        s"${FluidTankCommon.modId}:potion_lingering" -> FluidLike.POTION_LINGERING,
      )

      expected.map { case (str, like) =>
        DynamicTest.dynamicTest(str, () => {
          val fromName = FluidLike.fromResourceLocation(ResourceLocation.parse(str))
          assertEquals(like, fromName)
        })
      }.toArray
    }
  }

  @Nested
  class AsFluidTest {
    @Test
    def fromWater(): Unit = {
      assertEquals(Fluids.WATER, FluidLike.asFluid(FluidLike.FLUID_WATER, null))
    }

    @Test
    def fromEmpty(): Unit = {
      assertEquals(Fluids.EMPTY, FluidLike.asFluid(FluidLike.FLUID_EMPTY, null))
    }

    @Test
    def fromPotion(): Unit = {
      assertNull(FluidLike.asFluid(FluidLike.POTION_NORMAL, null))
    }
  }

  @Nested
  class UseCacheTest {
    @TestFactory
    def fluid(): Array[DynamicNode] = {
      Seq(FluidLike.FLUID_EMPTY, FluidLike.FLUID_WATER, FluidLike.FLUID_LAVA)
        .map(f => DynamicTest.dynamicTest(f.toString, () =>
          assertSame(f, FluidLike.of(f.fluid))
        ))
        .toArray
    }

    @TestFactory
    def potion(): Array[DynamicNode] = {
      Seq(FluidLike.POTION_NORMAL, FluidLike.POTION_SPLASH, FluidLike.POTION_LINGERING)
        .map(f => DynamicTest.dynamicTest(f.toString, () =>
          assertSame(f, FluidLike.of(f.potionType))
        ))
        .toArray
    }
  }
}
