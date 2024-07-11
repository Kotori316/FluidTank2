package com.kotori316.fluidtank.fabric.data

import cats.data.Chain
import com.google.gson.{JsonArray, JsonObject}
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.resource.conditions.v1.{ResourceCondition, ResourceConditions}
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

import scala.jdk.OptionConverters.RichOptional

private[data] sealed trait PlatformCondition {
  def fabricCondition: Option[JsonObject]

  def forgeCondition: Option[JsonObject]

  def neoforgeCondition: Option[JsonObject]
}

private[data] object PlatformCondition {
  // type: array
  private final val NEOFORGE_CONDITION_KEY = "neoforge:conditions"
  // type: array
  private final val FABRIC_CONDITION_KEY = ResourceConditions.CONDITIONS_KEY
  // type: array
  private final val FORGE_CONDITION_KEY = "forge:condition"

  def addPlatformConditions(obj: JsonObject, conditions: Chain[PlatformCondition]): JsonObject = {
    for {
      (key, getter, folder) <- Seq(
        (NEOFORGE_CONDITION_KEY, (c: PlatformCondition) => c.neoforgeCondition, foldAsJsonArray _),
        (FABRIC_CONDITION_KEY, (c: PlatformCondition) => c.fabricCondition, foldAsJsonArray _),
        (FORGE_CONDITION_KEY, (c: PlatformCondition) => c.forgeCondition, foldAsAndObject _),
      )
    } {
      val objects = for {
        condition <- conditions
        pc <- Chain.fromOption(getter(condition))
      } yield pc
      folder(objects).foreach(arr => obj.add(key, arr))
    }

    obj
  }

  def tagCondition(fabric: Option[TagKey[?]] = None, forge: Option[String] = None): PlatformCondition = {
    val fabricTag = fabric.map(_.location())
    // Use fabric tag for neoforge, convention tags are shared
    TagCondition(fabricTag, fabricTag, forge.map(s => ResourceLocation.parse(s)))
  }

  private def foldAsJsonArray(conditions: Chain[JsonObject]): Option[JsonArray] = {
    val arr = conditions.filterNot(_.isEmpty).foldLeft(new JsonArray()) { (a, o) => a.add(o); a }
    if (arr.isEmpty) None
    else Option(arr)
  }

  private def foldAsAndObject(conditions: Chain[JsonObject]): Option[JsonObject] = {
    for {
      arr <- foldAsJsonArray(conditions)
    } yield {
      val o = new JsonObject
      o.addProperty("type", "forge:and")
      o.add("values", arr)
      o
    }
  }

  private case class TagCondition(fabric: Option[ResourceLocation], neoforge: Option[ResourceLocation], forge: Option[ResourceLocation]) extends PlatformCondition {

    override def fabricCondition: Option[JsonObject] = {
      for {
        tag <- fabric
        condition = ResourceConditions.tagsPopulated(TagKey.create(Registries.ITEM, tag))
        json <- ResourceCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).result().toScala.map(_.getAsJsonObject)
      } yield json
    }

    override def forgeCondition: Option[JsonObject] = {
      for (tag <- forge) yield {
        val tagEmpty = new JsonObject
        tagEmpty.addProperty("type", "forge:tag_empty")
        tagEmpty.addProperty("tag", tag.toString)

        val not = new JsonObject
        not.addProperty("type", "forge:not")
        not.add("value", tagEmpty)

        not
      }
    }

    override def neoforgeCondition: Option[JsonObject] = {
      for (tag <- neoforge) yield {
        val tagEmpty = new JsonObject
        tagEmpty.addProperty("type", "neoforge:tag_empty")
        tagEmpty.addProperty("tag", tag.toString)

        val not = new JsonObject
        not.addProperty("type", "neoforge:not")
        not.add("value", tagEmpty)

        not
      }
    }
  }
}
