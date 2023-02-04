package ru.yandex.realty.converters.vos

import java.util
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.context.ExtDataLoaders
import ru.yandex.realty.generator.RawOfferApartmentSellGenerator
import ru.yandex.realty.model.raw.{RawOfferImpl, RawTemporaryPriceImpl}
import ru.yandex.realty.model.serialization.MockRawOfferBuilder
import ru.yandex.realty.proto.offer.vos.Offer
import ru.yandex.realty.storage.verba.VerbaStorage
import ru.yandex.realty.util.MockUtils

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 18.12.17
  */
@RunWith(classOf[JUnitRunner])
class VosOfferSourceProtoConverterTest extends FlatSpec with Matchers with PropertyChecks {
  implicit val verbaStorage: VerbaStorage =
    ExtDataLoaders.createVerbaStorage(getClass.getClassLoader.getResourceAsStream("verba2-3-4457.data"))

  "VosOfferSource converter" should "correct work in simple sell case" in {
    val f = MockRawOfferBuilder.createMockRawOfferOld
    f.setId("1")
    f.setPhotoPreviews(new util.ArrayList[Offer.PhotoPreview]())
    val msg = VosOfferSourceProtoConverter.toMessage(f)
    val clone = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
    MockUtils.assertEquals(f, clone)
  }

  it should "correct work for selling apartment" in {
    forAll(RawOfferApartmentSellGenerator.RawOfferApartmentSellGenerator) { rawOffer =>
      val msg = VosOfferSourceProtoConverter.toMessage(rawOffer)
      val clone = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
      MockUtils.assertEquals(rawOffer, clone)
    }
  }

  it should "correct work in simple rent case" in {
    val f = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(false)
    f.setId("1")
    f.setPhotoPreviews(new util.ArrayList[Offer.PhotoPreview]())
    val msg = VosOfferSourceProtoConverter.toMessage(f)
    val clone = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
    MockUtils.assertEquals(f, clone)
  }

  it should "correct work for experimental fields" in {
    val first = MockRawOfferBuilder.createRawOfferWithExperimentalFields
    first.setId("1")
    first.setPhotoPreviews(new util.ArrayList[Offer.PhotoPreview]())
    val msg = VosOfferSourceProtoConverter.toMessage(first)
    val clone = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
    MockUtils.assertEquals(first, clone)
  }

  it should "correct work for rent fields" in {
    val first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(false)
    first.setId("1")
    first.setPhotoPreviews(new util.ArrayList[Offer.PhotoPreview]())
    first.setRentDeposit(1L)
    first.setUtilitiesFee("included")
    val msg = VosOfferSourceProtoConverter.toMessage(first)
    val clone = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
    MockUtils.assertEquals(first, clone)
  }

  it should "correct work for temporary price" in {
    val first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(false)
    first.setId("1")
    first.setPhotoPreviews(new util.ArrayList[Offer.PhotoPreview]())
    val p = new RawTemporaryPriceImpl
    p.setValue(110000.0f)
    p.setCurrency("RUR")
    p.setDuration(3)
    first.setTemporaryPrice(p)
    val msg = VosOfferSourceProtoConverter.toMessage(first)
    val second = VosOfferSourceProtoConverter.fromMessage(msg).asInstanceOf[RawOfferImpl]
    MockUtils.assertEquals(first, second)
  }
}
