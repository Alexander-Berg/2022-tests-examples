package ru.yandex.vertis.punisher.convert

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.vertis.punisher.BaseSpec

/**
  * @author akhazhoyan 07/2019
  */
@RunWith(classOf[JUnitRunner])
class ProtobufConversionSpec extends BaseSpec {

  import ru.yandex.vertis.punisher.Generators._
  import Protobuf._

  "LastUserState" should {
    "correctly convert" in {
      check(forAll(LastUserStateGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "PunisherRequest" should {
    "correctly convert" in {
      check(forAll(PunisherRequestGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }
}
