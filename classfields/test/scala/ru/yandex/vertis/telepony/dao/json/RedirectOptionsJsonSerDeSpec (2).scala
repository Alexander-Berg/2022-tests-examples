package ru.yandex.vertis.telepony.dao.json

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.RedirectOptionsGen

import scala.annotation.nowarn

/**
  * @author neron
  */
class RedirectOptionsJsonSerDeSpec extends SpecBase with ScalaCheckPropertyChecks {

  implicit val generatorConfig = PropertyCheckConfiguration(1000)

  "Json transformer" should {
    "transform redirect options" in {
      forAll(RedirectOptionsGen)(test(_, RedirectOptionsJsonSerDe))
    }
  }

  @nowarn
  private def test[M](model: M, c: JsonConversion[M]): Unit = {
    val actualJson = c.to(model)
    val actualModel = c.from(actualJson)
    actualModel shouldEqual model
    val actualJsonAgain = c.to(actualModel)
    actualJsonAgain shouldEqual actualJson
  }

}
