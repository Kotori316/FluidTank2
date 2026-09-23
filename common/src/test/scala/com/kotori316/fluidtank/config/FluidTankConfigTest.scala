package com.kotori316.fluidtank.config

import cats.data.NonEmptyChain
import cats.kernel.Eq
import com.google.gson.{GsonBuilder, JsonObject}
import com.kotori316.fluidtank.tank.Tier
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.{Assertions, Nested, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{EnumSource, ValueSource}

import java.nio.file.Files
import java.util.Locale

class FluidTankConfigTest {
  private final val gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

  private def capacityMapOf(value: BigInt): Map[Tier, BigInt] =
    Tier.values().filter(_.isNormalTankTier).map(t => t -> value).toMap ++ FluidTankConfig.builtinTierMap

  def exec(test: () => Unit): Executable = () => test()

  @Test
  def loadFromJson(): Unit = {
    // language=json
    val jsonString =
      """{
        |  "renderLowerBound": 0.2,
        |  "renderUpperBound": 0.8,
        |  "debug": false,
        |  "changeItemInCreative": true,
        |  "capacities": {
        |    "wood": "162000",
        |    "stone": "162000",
        |    "iron": "162000",
        |    "gold": "162000",
        |    "diamond": "162000",
        |    "emerald": "162000",
        |    "star": "162000",
        |    "copper": "162000",
        |    "tin": "162000",
        |    "bronze": "162000",
        |    "lead": "162000",
        |    "silver": "162000"
        |  }
        |}
        |""".stripMargin
    val json = gson.fromJson(jsonString, classOf[JsonObject])
    val config = FluidTankConfig.getConfigDataFromJson(json)
    Assertions.assertTrue(config.isRight, s"Result: $config")

    val expected = ConfigData(capacityMapOf(BigInt(162000)), 0.2, 0.8, debug = false, changeItemInCreative = true)
    Assertions.assertEquals(expected, config.getOrElse(null))
  }

  @Test
  def loadFromJson2(): Unit = {
    // language=json
    val jsonString =
      """{
        |  "renderLowerBound": "0.2",
        |  "renderUpperBound": 0.8,
        |  "debug": "true",
        |  "changeItemInCreative": "false",
        |  "capacities": {
        |    "invalid": "162000",
        |    "wood": 162000,
        |    "stone": "162000",
        |    "iron": "162000",
        |    "gold": "162000",
        |    "diamond": "162000",
        |    "emerald": "162000",
        |    "star": "162000",
        |    "creative": "162000",
        |    "void": "162000",
        |    "copper": "162000",
        |    "tin": "162000",
        |    "bronze": "162000",
        |    "lead": "162000",
        |    "silver": "162000"
        |  }
        |}
        |""".stripMargin
    val json = gson.fromJson(jsonString, classOf[JsonObject])
    val config = FluidTankConfig.getConfigDataFromJson(json)
    Assertions.assertTrue(config.isRight, s"Result: $config")

    val expected = ConfigData.DEFAULT.copy(
      capacityMapOf(BigInt(162000)), 0.2, 0.8, debug = true)
    Assertions.assertEquals(expected, config.getOrElse(null))
  }

  @Nested
  class KeyErrorTest {
    @Test
    def noCapacity(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": 0.2,
          |  "renderUpperBound": 0.8,
          |  "debug": false,
          |  "changeItemInCreative": false
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)
      Assertions.assertFalse(config.isRight)

      val expected = NonEmptyChain(FluidTankConfig.KeyNotFound("capacities"))
      config.left match {
        case Some(e) => Assertions.assertEquals(expected, e)
        case _ => Assertions.fail("Unreachable")
      }
    }

    @Test
    def noTier(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": 0.2,
          |  "renderUpperBound": 0.8,
          |  "debug": false,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected = NonEmptyChain(
        FluidTankConfig.KeyNotFound("capacities.stone"),
        FluidTankConfig.KeyNotFound("capacities.star"),
        FluidTankConfig.KeyNotFound("capacities.bronze"),
      )
      config.left match {
        case Some(e) => Assertions.assertEquals(expected, e)
        case _ => Assertions.fail("Unreachable")
      }
    }

    @Test
    def noRenderBound(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "162000",
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected = NonEmptyChain(
        FluidTankConfig.KeyNotFound("renderLowerBound"),
        FluidTankConfig.KeyNotFound("renderUpperBound"),
        FluidTankConfig.KeyNotFound("debug"),
      )
      config.left match {
        case Some(e) => Assertions.assertEquals(expected, e)
        case _ => Assertions.fail("Unreachable")
      }
    }
  }

  @Test
  def checkEq(): Unit = {
    val a1 = FluidTankConfig.Other("renderLowerBound", new NumberFormatException("a1"))
    val a2 = FluidTankConfig.Other("renderLowerBound", new NumberFormatException("a2"))
    val a3 = FluidTankConfig.Other("renderLowerBound", new IllegalArgumentException("a3"))

    val eq = Eq[FluidTankConfig.LoadError]
    Assertions.assertAll(
      () => Assertions.assertTrue(eq.eqv(a1, a2)),
      () => Assertions.assertTrue(eq.neqv(a1, a3)),
      () => Assertions.assertTrue(eq.neqv(a2, a3)),
    )
  }

  @Nested
  class ConvertErrorTest {
    @Test
    def invalidDouble(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": "test",
          |  "renderUpperBound": "test2",
          |  "debug": true,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "162000",
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected: NonEmptyChain[FluidTankConfig.LoadError] = NonEmptyChain(
        FluidTankConfig.Other("renderLowerBound", new NumberFormatException()),
        FluidTankConfig.Other("renderUpperBound", new NumberFormatException())
      )
      config.left match {
        case Some(e) =>
          Assertions.assertTrue(expected.toChain === e.toChain, s"$expected, $e")
        case _ => Assertions.fail("Unreachable")
      }
    }

    @Test
    def invalidBoolean(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": "0.1",
          |  "renderUpperBound": "0.9",
          |  "debug": "2",
          |  "capacities": {
          |    "wood": "162000",
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": 162000,
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      // For boolean, getAsBoolean returns `false` for non boolean values.
      val expected = ConfigData.DEFAULT.copy(
        capacityMapOf(BigInt(162000)), 0.1, 0.9, debug = false)
      config.right match {
        case Some(e) =>
          Assertions.assertEquals(expected, e)
        case _ => Assertions.fail("Unreachable")
      }
    }

    @Test
    def invalidCapacity(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": 0.2,
          |  "renderUpperBound": 0.8,
          |  "debug": true,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "1.6",
          |    "stone": "test",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected: NonEmptyChain[FluidTankConfig.LoadError] = NonEmptyChain(
        FluidTankConfig.Other("capacities.wood", new NumberFormatException()),
        FluidTankConfig.Other("capacities.stone", new NumberFormatException()),
      )
      config.left match {
        case Some(e) =>
          Assertions.assertTrue(expected.toChain === e.toChain, s"$expected, $e")
        case _ => Assertions.fail("Unreachable")
      }
    }
  }

  @Nested
  class InvalidRangeTest {
    @Test
    def renderLowerBound(): Unit = {

      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": -0.1,
          |  "renderUpperBound": 0.9,
          |  "debug": true,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "162000",
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected: NonEmptyChain[FluidTankConfig.LoadError] = NonEmptyChain(
        FluidTankConfig.InvalidValue("renderLowerBound", "Too small(min=0.0)")
      )
      config.left match {
        case Some(e) => Assertions.assertEquals(expected, e)
        case _ => Assertions.fail(s"Unreachable, ${config.toString}")
      }
      config.right match {
        case Some(c) => Assertions.assertEquals(0, c.renderLowerBound)
        case _ => Assertions.fail(s"Unreachable, ${config.toString}")
      }
    }

    @Test
    def capacity(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": 0.1,
          |  "renderUpperBound": 0.9,
          |  "debug": true,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": -1,
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val json = gson.fromJson(jsonString, classOf[JsonObject])
      val config = FluidTankConfig.getConfigDataFromJson(json)

      val expected: NonEmptyChain[FluidTankConfig.LoadError] = NonEmptyChain(
        FluidTankConfig.InvalidValue("capacities.wood", "Too small(min=0)")
      )
      config.left match {
        case Some(e) => Assertions.assertEquals(expected, e)
        case _ => Assertions.fail(s"Unreachable, ${config.toString}")
      }
    }
  }

  @Nested
  class SaveTest {
    @Test
    def defaultConfig(): Unit = {
      val json = ConfigData.DEFAULT.createJson
      val loaded = FluidTankConfig.getConfigDataFromJson(json)
      Assertions.assertTrue(loaded.isRight, s"The json created from complete config must return valid config, $loaded")
      Assertions.assertEquals(ConfigData.DEFAULT, loaded.getOrElse(null))
    }

    @Test
    def capacityKeysInJson(): Unit = {
      val json = ConfigData.DEFAULT.createJson
      val keys = json.getAsJsonObject("capacities").keySet()
      val expected = Tier.values().filter(_.isNormalTankTier).map(_.name().toLowerCase(Locale.ROOT)).toSet
      Assertions.assertEquals(expected, keys)
      Assertions.assertAll(
        Seq(Tier.INVALID, Tier.VOID, Tier.CREATIVE).map(t =>
          exec(() => Assertions.assertFalse(keys.contains(t.name().toLowerCase(Locale.ROOT)), s"$t must not be saved"))
        ) *
      )
    }

    @Test
    def migrationDebug(): Unit = {
      // language=json
      val jsonString =
        """{
          |  "renderLowerBound": 0.2,
          |  "renderUpperBound": 0.8,
          |  "changeItemInCreative": false,
          |  "capacities": {
          |    "wood": "162000",
          |    "stone": "162000",
          |    "iron": "162000",
          |    "gold": "162000",
          |    "diamond": "162000",
          |    "emerald": "162000",
          |    "star": "162000",
          |    "copper": "162000",
          |    "tin": "162000",
          |    "bronze": "162000",
          |    "lead": "162000",
          |    "silver": "162000"
          |  }
          |}
          |""".stripMargin
      val tempDir = Files.createTempDirectory(null)
      Files.writeString(tempDir.resolve("migrationDebug.json"), jsonString)

      val config = FluidTankConfig.loadFile(tempDir, "migrationDebug.json")
      Assertions.assertTrue(config.isBoth)
      val errorExpected = NonEmptyChain(
        FluidTankConfig.KeyNotFound("debug"),
      )
      Assertions.assertEquals(config.left, Option(errorExpected))
      val expected = ConfigData.DEFAULT.copy(
        capacityMapOf(BigInt(162000)), 0.2, 0.8, debug = false)
      Assertions.assertEquals(config.right, Option(expected))

      FluidTankConfig.createFile(tempDir, "migrationDebug.json", config.right.get)

      val migrated = FluidTankConfig.loadFile(tempDir, "migrationDebug.json")
      Assertions.assertTrue(migrated.isRight)
      Assertions.assertEquals(migrated.right, Option(expected))
    }

    @Test
    def loadFromNotExistFile(): Unit = {
      val tempDir = Files.createTempDirectory(null)
      val config = FluidTankConfig.loadFile(tempDir, "loadFromNotExistFile.json")
      config.left match {
        case Some(e) => Assertions.assertTrue(e.contains(FluidTankConfig.FileNotFound),
          s"Errors: $e")
        case None => Assertions.fail(s"How to load non-exist file without error? $config")
      }

      config.right match {
        case Some(e) => Assertions.assertEquals(ConfigData.DEFAULT, e, s"On error, we should use the default config, but $e")
        case None => Assertions.fail("Which config to use?")
      }

      FluidTankConfig.createFile(tempDir, "loadFromNotExistFile.json", config.right.get)
      Assertions.assertTrue(Files.exists(tempDir.resolve("loadFromNotExistFile.json")))

      val migrated = FluidTankConfig.loadFile(tempDir, "loadFromNotExistFile.json")
      Assertions.assertTrue(migrated.isRight)
      Assertions.assertEquals(migrated.right, Option(ConfigData.DEFAULT))
    }
  }

  @Nested
  class RangeCheckerTest {
    @ParameterizedTest
    @ValueSource(ints = Array(20, 0, 1))
    def rangeCheckerTest1(value: Int): Unit = {
      val checker = FluidTankConfig.rangeChecker("key", Option(0))
      val result1 = checker(value)
      Assertions.assertTrue(result1.isRight)
      Assertions.assertEquals(value, result1.getOrElse(Assertions.fail()))
    }

    @Test
    def rangeCheckerTest2(): Unit = {
      val checker = FluidTankConfig.rangeChecker("key", Option(0))
      val result1 = checker(-1)
      Assertions.assertTrue(result1.isBoth)
      Assertions.assertEquals(0, result1.getOrElse(Assertions.fail()))
      val message = result1.swap.getOrElse(Assertions.fail())
      Assertions.assertEquals(NonEmptyChain(FluidTankConfig.InvalidValue("key", "Too small(min=0)")), message)
    }

    @ParameterizedTest
    @ValueSource(ints = Array(20, 0, 100))
    def rangeCheckerTest3(value: Int): Unit = {
      val checker = FluidTankConfig.rangeChecker("key", max = Option(100))
      val result1 = checker(value)
      Assertions.assertTrue(result1.isRight)
      Assertions.assertEquals(value, result1.getOrElse(Assertions.fail()))
    }

    @Test
    def rangeCheckerTest4(): Unit = {
      val checker = FluidTankConfig.rangeChecker("key", max = Option(100))
      val result1 = checker(200)
      Assertions.assertTrue(result1.isBoth)
      Assertions.assertEquals(100, result1.getOrElse(Assertions.fail()))
      val message = result1.swap.getOrElse(Assertions.fail())
      Assertions.assertEquals(NonEmptyChain(FluidTankConfig.InvalidValue("key", "Too big(max=100)")), message)
    }

    @ParameterizedTest
    @ValueSource(ints = Array(20, 0, 100))
    def rangeCheckerTest5(value: Int): Unit = {
      val checker = FluidTankConfig.rangeChecker("key", min = Option(0), max = Option(100))
      val result1 = checker(value)
      Assertions.assertTrue(result1.isRight)
      Assertions.assertEquals(value, result1.getOrElse(Assertions.fail()))
    }
  }

  @Nested
  class BuiltinTierTest {
    @ParameterizedTest
    @EnumSource(value = classOf[Tier], names = Array("INVALID", "VOID", "CREATIVE"))
    def key(tier: Tier): Unit = {
      Assertions.assertTrue(FluidTankConfig.builtinTierMap.contains(tier))
    }

    @Test
    def normal(): Unit = {
      Assertions.assertFalse(FluidTankConfig.builtinTierMap.contains(Tier.BRONZE))
    }
  }
}
