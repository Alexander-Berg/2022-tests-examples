package ru.yandex.realty.rent.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class ContractTermsTest extends SpecBase {

  "ContractTerms" should {
    "Urls check" in {
      ContractTerms.versions.count(_.url == "https://yandex.ru/legal/realty_lease_tenant/") shouldBe 1
    }
  }
}
