package ru.yandex.vertis.telepony.model

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.AntiFraudOptionSetGen

/**
  * @author @logab
  */
class AntiFraudOptionsSpec extends SpecBase with ScalaCheckDrivenPropertyChecks {

  "AntiFraudOption" should {
    "be properly packed" in {
      forAll(AntiFraudOptionSetGen) { options =>
        val withoutUnknown = options.filterNot(_ == AntiFraudOptions.Unknown)
        AntiFraudOptions.unpack(AntiFraudOptions.pack(options)) shouldEqual withoutUnknown
      }
    }
    "avoid packing unknown option" in {
      val unpacked = AntiFraudOptions.unpack(AntiFraudOptions.pack(Set(AntiFraudOptions.Unknown)))
      unpacked shouldEqual Set()
    }
  }

}
