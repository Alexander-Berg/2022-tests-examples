package ru.yandex.realty.transformers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.vos.RequestModel.{UnifiedLocation, VosOffer}
import ru.yandex.realty.model.offer.{CategoryType, OfferType}

@RunWith(classOf[JUnitRunner])
class DefaultAddressConverterSpec extends SpecBase {
  private def getOffer(address: String, location: Option[UnifiedLocation]) = {
    VosOffer(
      "24601",
      OfferType.SELL,
      CategoryType.APARTMENT,
      address,
      location
    )
  }

  "CallController" should {
    "process vos offers, when address is set" in {
      val offer = getOffer(
        "Улица такая-то, 42д",
        Some(
          UnifiedLocation(
            Some("A"),
            Some("B"),
            Some("C"),
            Some("D"),
            Some("E"),
            Some("42")
          )
        )
      )
      val result = DefaultAddressConverter.getAddress(offer)
      require(result == "Улица такая-то, 42д")
    }

    "return empty string, when no address and no unified location is set" in {
      val offer = getOffer(
        "",
        None
      )
      val result = DefaultAddressConverter.getAddress(offer)
      require(result == "")
    }

    "return empty string, when only house number is present" in {
      val offer = getOffer(
        "",
        Some(
          UnifiedLocation(
            None,
            None,
            None,
            None,
            None,
            Some("42")
          )
        )
      )
      val result = DefaultAddressConverter.getAddress(offer)
      require(result == "")
    }

    "find the most accurate match" in {
      val offer = getOffer(
        "",
        Some(
          UnifiedLocation(
            Some("Region"),
            Some("District"),
            None,
            Some("SubLocality"),
            None,
            None
          )
        )
      )
      val result = DefaultAddressConverter.getAddress(offer)
      require(result == "SubLocality")
    }

    "return string with house number, if present" in {
      val offer = getOffer(
        "",
        Some(
          UnifiedLocation(
            None,
            Some("District"),
            Some("Locality"),
            None,
            None,
            Some("42д")
          )
        )
      )
      val result = DefaultAddressConverter.getAddress(offer)
      require(result == "Locality, 42д")
    }
  }
}
