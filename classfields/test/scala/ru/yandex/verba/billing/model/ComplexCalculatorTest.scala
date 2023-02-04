package ru.yandex.verba.billing.model

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.billing.model.Calculator.Price
import ru.yandex.verba.core.attributes._
import ru.yandex.verba.core.attributes.diff.{AttributeDiff, Diff}
import ru.yandex.verba.core.util.VerbaUtils
import spray.json.JsValue

import scala.annotation.nowarn

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 23.10.14
  */
@nowarn("cat=w-flag-value-discard")
class ComplexCalculatorTest extends AnyFreeSpec with VerbaUtils with Matchers {
  def calc = ComplexCalculator(_: String, Set(SimpleCalculator("x", Price("x", 1))))
  "complex diff" - {
    val diff = Diff(
      Map(
        "a" -> AttributeDiff(Empty, ComplexEntity(Attributes("x" -> Str("x")))),
        "b" -> AttributeDiff(ComplexEntity(Attributes("x" -> Str("y"))), ComplexEntity(Attributes("x" -> Str("x")))),
        "c" -> AttributeDiff(ComplexEntity(Attributes("x" -> Str("y"))), Empty),
        "d" -> AttributeDiff(
          ComplexEntities(
            Seq(ComplexEntity("1", Attributes("x" -> Str("y"))))
          ),
          ComplexEntities(Seq(ComplexEntity("2", Attributes("x" -> Str("y")))))
        )
      )
    )
    "diff find" - {
      val res = diff.changes.values.flatMap(ComplexCalculator.toDiff)
      res.size shouldEqual 5
    }
    "calc" - {
      val events = diff.changes.keys.toSeq.flatMap(calc(_).apply(diff, null))
      events.size shouldEqual 5
    }
  }

  "json parse and write" - {
    import spray.json._
    import Calculator._
    val calculator = calc("x")
    val j = calculator.asInstanceOf[Calculator].toJson
    val parsedCalculator: Calculator = j.convertTo[Calculator]
    println(parsedCalculator.equals(calculator))
  }
}
