package ru.yandex.realty.model.raw

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.serialization.RawOfferProtoConverter
import ru.yandex.realty.picapica.MdsUrlBuilder

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class RawOfferSpec extends SpecBase {
  "rawOffer" should {
    "default to a null value for builtYear" in {
      val rawOffer = new RawOfferImpl
      rawOffer.getBuiltYear should be(null)
    }

    "allow non-null value for builtYear" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setBuiltYear(17)
      rawOffer.getBuiltYear should be(17)
    }

    "allow null value for builtYear" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setBuiltYear(42)
      rawOffer.getBuiltYear should be(42)
      rawOffer.setBuiltYear(null)
      rawOffer.getBuiltYear should be(null)
    }

    "preserve null value for buildYear during ser/deser" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setId(Random.nextLong().toString)

      rawOffer.setBuiltYear(152351)
      rawOffer.setBuiltYear(null)
      rawOffer.getBuiltYear should be(null)

      val msg = RawOfferProtoConverter.toMessage(rawOffer)
      val deserialized = RawOfferProtoConverter.fromMessage(msg, new MdsUrlBuilder("//a.z"))

      deserialized.getBuiltYear should be(null)
      RawOfferProtoConverter.toMessage(deserialized) should be(RawOfferProtoConverter.toMessage(rawOffer))
    }

    "default to a null value for readyQuarter" in {
      val rawOffer = new RawOfferImpl
      rawOffer.getReadyQuarter should be(null)
    }

    "allow non-null value for readyQuarter" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setReadyQuarter(22)
      rawOffer.getReadyQuarter should be(22)
    }

    "allow null value for readyQuarter" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setReadyQuarter(21351)
      rawOffer.getReadyQuarter should be(21351)
      rawOffer.setReadyQuarter(null)
      rawOffer.getReadyQuarter should be(null)
    }

    "preserve null value for readyQuarter during ser/deser" in {
      val rawOffer = new RawOfferImpl
      rawOffer.setId(Random.nextLong().toString)

      rawOffer.setReadyQuarter(5613451)
      rawOffer.setReadyQuarter(null)
      rawOffer.getReadyQuarter should be(null)

      val msg = RawOfferProtoConverter.toMessage(rawOffer)
      val deserialized = RawOfferProtoConverter.fromMessage(msg, new MdsUrlBuilder("//a.z"))

      deserialized.getReadyQuarter should be(null)
      RawOfferProtoConverter.toMessage(deserialized) should be(RawOfferProtoConverter.toMessage(rawOffer))
    }
  }
}
