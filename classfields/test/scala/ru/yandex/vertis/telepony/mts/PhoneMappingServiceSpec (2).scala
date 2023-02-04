package ru.yandex.vertis.telepony.mts

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.model.Phone

/**
  * @author evans
  */
trait PhoneMappingServiceSpec extends SpecBase with ScalaCheckPropertyChecks {

  implicit val generatorConfig = PropertyCheckConfiguration(1000)

  def phoneMappingService: PhoneMappingService

  "Phone mapping service" should {
    "transform +79147901234 to +74232701234" in {
      val phone = Phone("+79147901234")
      val expected = Phone("+74232701234")
      phoneMappingService.def2abc(phone) shouldEqual expected
    }

    "transform +74957239600 to +79857239600" in {
      val phone = Phone("+74957239600")
      val expected = Phone("+79857239600")
      phoneMappingService.abc2def(phone) shouldEqual expected
    }
    "nothing do for +79312320032" in {
      val phone = Phone("+79312320032")
      phoneMappingService.def2abc(phone) shouldEqual phone
    }
    "transform +79882394483 to +78622394483" in {
      val phone = Phone("+79882394483")
      val expected = Phone("+78622394483")
      phoneMappingService.def2abc(phone) shouldEqual expected
    }
    val brokenDefNumbers = Seq(
      Phone("+79114008509"),
      Phone("+79114008036"),
      Phone("+74162566853")
    )
    brokenDefNumbers.foreach { phone =>
      s"transform ${phone.value} to and back" ignore {
        phoneMappingService.abc2def(phoneMappingService.def2abc(phone)) shouldEqual phone
      }
    }

// TODO remove after fix
//    "transform to and back" in {
//      forAll(DefPhoneGen.suchThat(p => !brokenDefNumbers.contains(p))) { phone =>
//        phoneMappingService.abc2def(phoneMappingService.def2abc(phone)) shouldEqual phone
//      }
//    }
//    "transform back and tp" in {
//      forAll(AbcPhoneGen) { phone =>
//        phoneMappingService.def2abc(phoneMappingService.abc2def(phone)) shouldEqual phone
//      }
//    }
  }
}
