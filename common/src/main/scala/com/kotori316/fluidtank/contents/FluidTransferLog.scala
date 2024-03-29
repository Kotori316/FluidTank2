package com.kotori316.fluidtank.contents

import cats.Show
import cats.implicits.showInterpolator

sealed trait FluidTransferLog {
  def logString: String

  override final def toString: String = this.logString

  def isValidTransfer: Boolean
}

object FluidTransferLog {

  case class FillFluid[A](toFill: GenericAmount[A], filled: GenericAmount[A], before: Tank[A], after: Tank[A]) extends FluidTransferLog {
    override def logString: String = show"FillFluid[Filled=$filled, ToFill=$toFill, Before=${before.content}, After=${after.content}]"

    override def isValidTransfer: Boolean = toFill.nonEmpty
  }

  case class FillFailed[A](fluid: GenericAmount[A], tank: Tank[A]) extends FluidTransferLog {
    override def logString: String = show"FillFailed[FailedToFill=$fluid, Tank=${tank.content}]"

    override def isValidTransfer: Boolean = fluid.nonEmpty
  }

  case class FillAll[A](fluid: GenericAmount[A], tank: Tank[A]) extends FluidTransferLog {
    override def logString: String = show"FillAll[Filled=$fluid, Tank=${tank.content}]"

    override def isValidTransfer: Boolean = fluid.nonEmpty
  }

  case class DrainFluid[A](toDrain: GenericAmount[A], drained: GenericAmount[A], before: Tank[A], after: Tank[A]) extends FluidTransferLog {
    override def logString: String = show"DrainFluid[Drained=$drained, ToDrain=$toDrain, Before=${before.content}, After=${after.content}]"

    override def isValidTransfer: Boolean = toDrain.nonEmpty
  }

  case class DrainFailed[A](fluid: GenericAmount[A], tank: Tank[A]) extends FluidTransferLog {
    override def logString: String = show"DrainFailed[FailedToDrain=$fluid, Tank=${tank.content}]"

    override def isValidTransfer: Boolean = fluid.nonEmpty && tank.hasContent
  }

  implicit val showFluidTransferLog: Show[FluidTransferLog] = f => {
    if (f.isValidTransfer) f.logString
    else f.getClass.getSimpleName
  }
}
