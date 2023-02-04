package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.reflect.runtime.universe._

/**
  * Specs on [[TypeTag]]
  *
  * @author alesavin
  */
class TypeTagSpec extends AnyWordSpec with Matchers {

  "TypeTag" should {
    "match" in {
      info(IntType(1).toString)
      info(StringType("aaaa").toString)
      info(BooleanType(true).toString)
    }
  }

  case class IntType(i: Int)
  case class StringType(s: String)
  case class BooleanType(b: Boolean)

  val intType = typeOf[IntType]

  def print[T: TypeTag](x: T) =
    typeOf[T] match {
      case `intType` =>
        println(s"$intType: ${x.asInstanceOf[IntType].i}")
      case t @ TypeRef(a, b, c) =>
        println(s"$a, $b, $c")
    }

}
