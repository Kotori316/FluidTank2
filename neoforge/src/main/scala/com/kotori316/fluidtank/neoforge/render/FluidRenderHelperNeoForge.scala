package com.kotori316.fluidtank.neoforge.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.{FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import com.kotori316.fluidtank.render.{FluidRenderHelper, RenderItemCodecs}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.sprite.SpriteGetter
import net.minecraft.core.component.{DataComponentMap, DataComponents}
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.client.fluid.FluidTintSources

object FluidRenderHelperNeoForge extends FluidRenderHelper {

  final lazy val reservoirUnbaked = RenderItemCodecs.reservoirModelUnbaked(this)
  final lazy val tankUnbaked = RenderItemCodecs.tankModelUnbaked(this)

  override def getFluidTexture(tank: Tank[FluidLike], materialSet: SpriteGetter): TextureAtlasSprite = {
    val fluid = FluidLike.asFluid(tank.content.content, Fluids.WATER)
    val fluidState = fluid.defaultFluidState
    Minecraft.getInstance.getModelManager.getFluidStateModelSet.get(fluidState).stillMaterial().sprite()
  }

  override def getFluidColor(tank: Tank[FluidLike]): Int = {
    val content = tank.content
    content.content match {
      case VanillaFluid(fluid) =>
        val fluidState = fluid.defaultFluidState
        val model = Minecraft.getInstance.getModelManager.getFluidStateModelSet.get(fluidState)
        val tintSource = FluidTintSources.of(model.tintSource())
        tintSource.colorAsStack(NeoForgeConverter.toStack(content))
      case VanillaPotion(_) =>
        val potionColor = for {
          c <- content.componentPatch
          potion <- Option(c.get(DataComponentMap.EMPTY, DataComponents.POTION_CONTENTS))
        } yield potion.getColor
        potionColor.getOrElse(16253176)
    }
  }
}
