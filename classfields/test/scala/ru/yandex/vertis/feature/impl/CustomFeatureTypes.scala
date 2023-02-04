package ru.yandex.vertis.feature.impl

import ru.yandex.vertis.feature.model.{FeatureType, FeatureTypes, SerDe}

import scala.util.matching.Regex

/**
  * Extension of [[FeatureTypes]] for tests
  *
  * @author frenki
  */
object CustomFeatureTypes extends FeatureTypes {

  case class ComplexType(a: Int, b: Int) {

    require(a >= 0, "a must be non negative")
    require(b >= 0, "b must be non negative")

    override def toString: String = s"$a + ${b}i"
  }
  object ComplexType {

    val Pattern: Regex = "([0-9]+) \\+ ([0-9]+)i".r

    def unapply(value: String): Option[ComplexType] =
      value match {
        case Pattern(a, b) =>
          Some(ComplexType(a.toInt, b.toInt))
        case _ => None
      }
  }

  implicit case object ComplexTypeFeatureType
    extends FeatureType[ComplexType] {

    override protected def id: String = "ComplexType"

    override def serDe: SerDe[ComplexType] =
      SerDe[ComplexType](_.toString, d => ComplexType.unapply(d).getOrElse(
        throw new IllegalArgumentException("Can't parse ComplexType")
      ))
  }

  override val featureTypes: Iterable[FeatureType[_]] =
    Iterable(ComplexTypeFeatureType)
}
