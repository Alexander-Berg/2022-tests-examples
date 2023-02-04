package ru.auto.api.model.multiposting

import org.scalatest.Inspectors
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName._
import ru.auto.api.BaseSpec
import ru.auto.api.model.multiposting.MultipostingUtils.ClassifiedNameUtils

class MultipostingUtilsSpec extends BaseSpec with ScalaCheckPropertyChecks with Inspectors {

  "ClassifiedNameUtils" should {

    "return all defined classifieds list" in {
      ClassifiedNameUtils.all should contain theSameElementsAs List(
        AUTORU,
        AVITO,
        DROM
      )
    }

    "return only external classifieds list" in {
      ClassifiedNameUtils.externalClassifieds should contain theSameElementsAs List(
        AVITO,
        DROM
      )
    }

  }

}
