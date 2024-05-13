package com.kotori316.fluidtank

import cats.implicits.toFunctorOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MacroHelperTest {

  @Test
  def testMethodName1(): Unit = {
    assertEquals("testMethodName1", MacroHelper.getCallerMethod)
  }

  @Test
  def testMethodNameOtherName(): Unit = {
    assertEquals("testMethodNameOtherName", MacroHelper.getCallerMethod)
  }

  @Test
  def inLambdaName(): Unit = {
    val name = cats.Id(1235).map(_ => MacroHelper.getCallerMethod)
    assertEquals("inLambdaName", name)
  }
}
