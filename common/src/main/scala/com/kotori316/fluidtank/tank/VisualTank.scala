package com.kotori316.fluidtank.tank

import cats.implicits.catsSyntaxEq
import com.kotori316.fluidtank.config.PlatformConfigAccess
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.render.Box
import net.minecraft.util.Mth

import scala.math.Ordering.Implicits.infixOrderingOps

class VisualTank {
  private lazy val lowerBound: Double = PlatformConfigAccess.getInstance().getConfig.renderLowerBound
  private lazy val upperBound: Double = PlatformConfigAccess.getInstance().getConfig.renderUpperBound
  var box: Box = scala.compiletime.uninitialized

  /**
   * Must be called in client only
   */
  def updateContent(capacity: GenericUnit, amount: GenericUnit, isGaseous: Boolean): Unit = {
    if (capacity =!= GenericUnit.ZERO) {
      if (amount >= GenericUnit.ZERO) {
        val d = 1d / 16d
        val (minY, maxY) = getFluidHeight(capacity.asForgeDouble, amount.asForgeDouble, lowerBound, upperBound, 0.003, isGaseous)
        box = Box(startX = d * 8, startY = minY, startZ = d * 8, endX = d * 8, endY = maxY, endZ = d * 8,
          sizeX = d * 12 - 0.01, sizeY = maxY - minY, sizeZ = d * 12 - 0.01,
          firstSide = true, endSide = true)
      } else {
        box = null
      }
    }
  }

  /**
   * @param capacity   the capacity of tank. Must not be 0.
   * @param amount     the amount in the tank, assumed to be grater than 0. (amount > 0)
   * @param lowerBound the minimum of fluid position.
   * @param upperBound the maximum of fluid position.
   * @param isGaseous  whether the fluid is gas or not.
   * @return (minY, maxY)
   */
  def getFluidHeight(capacity: Double, amount: Double, lowerBound: Double, upperBound: Double, minRatio: Double, isGaseous: Boolean): (Double, Double) = {
    val ratio = Mth.clamp(amount / capacity, minRatio, 1)
    val height = (upperBound - lowerBound) * ratio
    if (isGaseous) {
      (upperBound - height, upperBound)
    } else {
      (lowerBound, lowerBound + height)
    }
  }

}
