package ru.yandex.vertis.telepony.util

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{Operators, Phone}

class OriginOperatorHelperSpec extends SpecBase {
  private val helper: OriginOperatorHelper = OriginOperatorHelper()

  private val anyPhone: Phone = Phone("+79991112233")
  private val megafonPhone: Phone = Phone("+79210906509")
  private val voxPhone: Phone = Phone("+79847771539")

  "OriginOperatorHelper" should {
    "use operator as origin if not vox" in {
      helper.resolveOriginOperator(anyPhone, Operators.Mts) shouldBe Operators.Mts
      helper.resolveOriginOperator(anyPhone, Operators.Mtt) shouldBe Operators.Mtt
      helper.resolveOriginOperator(anyPhone, Operators.Beeline) shouldBe Operators.Beeline
    }

    "resolve megafon vox origin" in {
      helper.resolveOriginOperator(megafonPhone, Operators.Vox) shouldBe Operators.Megafon
    }

    "resolve vox vox origin" in {
      helper.resolveOriginOperator(voxPhone, Operators.Vox) shouldBe Operators.Vox
    }

    "resolve default vox origin" in {
      helper.resolveOriginOperator(anyPhone, Operators.Vox) shouldBe Operators.Mtt
    }

  }
}
