package com.kotori316.fluidtank.forge.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.{FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.forge.fluid.ForgeConverter
import com.kotori316.fluidtank.render.{FluidRenderHelper, RenderItemCodecs}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions

import java.util.Objects
import scala.jdk.javaapi.OptionConverters

object FluidRenderHelperForge extends FluidRenderHelper {

  override def getFluidTexture(tank: Tank[FluidLike]): TextureAtlasSprite = {
    val fluid = FluidLike.asFluid(tank.content.content, Fluids.WATER)
    val attribute = IClientFluidTypeExtensions.of(fluid)
    val location = attribute.getStillTexture(fluid.defaultFluidState, Minecraft.getInstance.level, Objects.requireNonNull(Minecraft.getInstance.player).getOnPos)
    Minecraft.getInstance.getTextureAtlas(RenderItemCodecs.atlas).apply(location)
  }

  override def getFluidColor(tank: Tank[FluidLike]): Int = {
    val content = tank.content
    content.content match {
      case VanillaFluid(fluid) =>
        val attribute = IClientFluidTypeExtensions.of(fluid);
        val normal = attribute.getTintColor()
        if (attribute == IClientFluidTypeExtensions.DEFAULT) {
          normal
        } else {
          val stackColor = attribute.getTintColor(ForgeConverter.toStack(content))
          if (normal == stackColor) {
            attribute.getTintColor(fluid.defaultFluidState, Minecraft.getInstance.level, Objects.requireNonNull(Minecraft.getInstance.player).getOnPos)
          }
          else {
            stackColor
          }
        }
      case VanillaPotion(_) =>
        val potionColor = for {
          c <- content.componentPatch
          mayBePotion <- Option(c.get(DataComponents.POTION_CONTENTS))
          potion <- OptionConverters.toScala(mayBePotion)
        } yield potion.getColor
        potionColor.getOrElse(16253176)
    }
  }
}
