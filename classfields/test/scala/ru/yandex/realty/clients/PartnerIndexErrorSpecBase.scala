package ru.yandex.realty.clients

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.capa.PartnerIndexError
import ru.yandex.realty.model.offer.IndexingError._

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class PartnerIndexErrorSpecBase extends SpecBase {

  "PartnerIndexError" should {
    "enumType return index error for type " in {
      val partnerIndexError = PartnerIndexError("LOCATION_NOT_FOUND", 0, Array.empty)

      partnerIndexError.enumType should be(Some(LOCATION_NOT_FOUND))
    }

    "enumType return None for wrong type " in {
      val partnerIndexError = PartnerIndexError("abrakadabra", 0, Array.empty)

      partnerIndexError.enumType should be(None)
    }

    "enumType return None for empty type " in {
      val partnerIndexError = PartnerIndexError("", 0, Array.empty)

      partnerIndexError.enumType should be(None)
    }
  }

}
