package com.kotori316.fluidtank.config

import cats.data.{Ior, IorNec, NonEmptyChain}
import cats.implicits.*
import cats.{Hash, Show}
import com.google.gson.{GsonBuilder, JsonElement, JsonObject}
import com.kotori316.fluidtank.tank.Tier

import java.nio.file.{Files, Path}
import java.util.Locale
import scala.util.{Failure, Success, Try, Using}

object FluidTankConfig {
  type E = LoadError

  def loadFile(basePath: Path, fileName: String, forTest: Boolean = false): IorNec[E, ConfigData] = {
    val configPath = basePath.resolve(fileName)
    val json = getFileContent(configPath)
    json match {
      case Ior.Left(a: NonEmptyChain[E]) => Ior.both(a, if (forTest) ConfigData.FOR_TEST else ConfigData.DEFAULT)
      case r@Ior.Right(_) => r.flatMap(getConfigDataFromJson)
      case b@Ior.Both(_, _) => b.flatMap(getConfigDataFromJson) // should be unreachable
    }
  }

  def getConfigDataFromJson(j: JsonObject): IorNec[E, ConfigData] = {
    (
      getCapacity(j),
      getDouble(j, "renderLowerBound", ConfigData.DEFAULT.renderLowerBound)
        .flatMap(rangeChecker("renderLowerBound", Option(0d), Option(1d))),
      getDouble(j, "renderUpperBound", ConfigData.DEFAULT.renderUpperBound)
        .flatMap(rangeChecker("renderUpperBound", Option(0d), Option(1d))),
      getValue[Boolean](j, "debug", _.getAsBoolean, ConfigData.DEFAULT.debug, Nil),
      getValue[Boolean](j, "changeItemInCreative", _.getAsBoolean, ConfigData.DEFAULT.changeItemInCreative, Nil),
    ).mapN(ConfigData.apply)
  }

  def createFile(basePath: Path, fileName: String, config: ConfigData): Unit = {
    val configPath = basePath.resolve(fileName)
    val gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val json = config.createJson

    Using(Files.newBufferedWriter(configPath)) { w =>
      gson.toJson(json, w)
    }
  }

  private def getFileContent(filePath: Path): IorNec[E, JsonObject] = {
    if (Files.exists(filePath)) {
      val gson = new GsonBuilder().create()
      val json = Using(Files.newBufferedReader(filePath)) { reader =>
        gson.fromJson(reader, classOf[JsonObject])
      }
      json match {
        case Failure(exception) => Ior.leftNec(Other("File loading", exception))
        case Success(value) => Ior.right(value)
      }
    } else {
      Ior.leftNec(FileNotFound)
    }
  }

  private def getValue[T](json: JsonObject, key: String, extractor: JsonElement => T, defaultValue: T, keyPrefix: Seq[String]): IorNec[E, T] = {
    val prefix = if (keyPrefix.isEmpty) "" else keyPrefix.mkString("", ".", ".")

    if (json.has(key)) {
      val element = json.get(key)
      Try(extractor(element)) match {
        case Failure(exception) => Ior.bothNec(Other(prefix + key, exception), defaultValue)
        case Success(value) => Ior.right(value)
      }
    } else {
      Ior.bothNec(KeyNotFound(prefix + key), defaultValue)
    }
  }

  private def getDouble(json: JsonObject, key: String, defaultValue: Double, keyPrefix: Seq[String] = Nil): IorNec[E, Double] = {
    getValue(json, key, _.getAsDouble, defaultValue, keyPrefix)
  }

  private def getBigInt(json: JsonObject, key: String, defaultValue: BigInt, keyPrefix: Seq[String] = Nil): IorNec[E, BigInt] = {
    getValue(json, key, e => BigInt(e.getAsString), defaultValue, keyPrefix)
  }

  private def getCapacity(jsonObject: JsonObject): IorNec[E, Map[Tier, BigInt]] = {
    val defaultValues = ConfigData.DEFAULT.capacityMap
    val capacityMap = getValue(jsonObject, "capacities", _.getAsJsonObject, new JsonObject, Seq.empty)

    capacityMap match {
      case r@Ior.Right(_) => capacityMap.flatMap(j =>
        Tier.values().toSeq
          .map(t => getBigInt(j, t.name().toLowerCase(Locale.ROOT), defaultValues(t), Seq("capacities"))
            .flatMap(rangeChecker("capacities." + t.name().toLowerCase(Locale.ROOT), min = Option(BigInt(0))))
            .map(b => Seq(t -> b)))
          .reduce((a, b) => a.product(b).map { case (a, b) => a ++ b })
          .map(_.toMap)
      )
      case _ =>
        capacityMap.map(_ => defaultValues)
    }
  }

  def rangeChecker[A: Ordering](key: String, min: Option[A] = None, max: Option[A] = None): A => IorNec[E, A] = v => {
    import Ordering.Implicits.*
    (min.exists(t => v < t), max.exists(t => t < v)) match {
      case (true, false) => Ior.bothNec(InvalidValue(key, s"Too small(min=${min.get})"), min.get)
      case (false, true) => Ior.bothNec(InvalidValue(key, s"Too big(max=${max.get})"), max.get)
      case (true, true) => Ior.leftNec(Other(s"Condition was wrong. min=$min, max=$max", new IllegalArgumentException()))
      case (false, false) => Ior.right(v)
    }
  }

  implicit final val hashLoadError: Hash[LoadError] = new Hash[LoadError] {
    override def hash(x: LoadError): Int = x.##

    override def eqv(x: LoadError, y: LoadError): Boolean = {
      (x, y) match {
        case (Other(key1, e1), Other(key2, e2)) => key1 == key2 && e1.getClass == e2.getClass
        case _ => x == y
      }
    }
  }

  implicit final val showLoadError: Show[LoadError] = Show.fromToString

  sealed trait LoadError

  object FileNotFound extends LoadError {
    override def toString: String = "FileNotFound"
  }

  case class KeyNotFound(key: String) extends LoadError

  case class InvalidValue(key: String, message: String) extends LoadError

  case class Other(key: String, exception: Throwable) extends LoadError

  /**
   * Just to ignore syntax error in this class.
   */
  @inline
  final def cast[A](x: AnyRef): A = {
    x.asInstanceOf[A]
  }
}
