package ru.yandex.vertis.telepony.service.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.telepony.model.Operators

/**
  *
  * @author zvez
  */
class OperatorNamesSpec extends AnyWordSpecLike with Matchers {

  "OperatorNames" should {
    "guess operator" in {
      OperatorNames.guessOperator("ПАО \"Мобильные ТелеСистемы\"") shouldBe Some(Operators.Mts)
      OperatorNames.guessOperator("ПАО \"Вымпел-Коммуникации\"") shouldBe Some(Operators.Beeline)
    }

    "not fail on unknown name" in {
      OperatorNames.guessOperator("Венезулиан") shouldBe None
    }
  }
}
