package ru.yandex.vos2.actors.realty.offer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.BasicsModel.Currency.{EUR, RUB}
import ru.yandex.vos2.OfferModel._
import ru.yandex.vertis.vos2.model.realty.AreaUnit.{SOTKA, SQ_M}
import ru.yandex.vertis.vos2.model.realty._
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.TestUtils

import scala.collection.JavaConverters._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class OfferChangeDetectorSpec extends WordSpec with Matchers {

  "Detector" should {
    "detect change of rooms for rent" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder
        .setOfferType(OfferType.RENT)
        .setRoomsTotal(3)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.setRoomsTotal(4)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "ignore change of rooms not for rent" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder
        .setOfferType(OfferType.SELL)
        .setRoomsTotal(3)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.setRoomsTotal(4)
      val o2 = builder.build()
      assert(!OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect change of location" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getAddressBuilder.setRgid(1)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getAddressBuilder.setRgid(2)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect significant change of price" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1000000).setCurrency(RUB)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1020001).setCurrency(RUB)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "ignore insignificant change of price" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1000000).setCurrency(RUB)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1002000).setCurrency(RUB)
      val o2 = builder.build()
      assert(!OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect change of currency" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1000000).setCurrency(RUB)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getPriceBuilder.setPriceValue(1000000).setCurrency(EUR)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect significant change of area" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(100).setAreaUnit(SQ_M)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(106).setAreaUnit(SQ_M)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "ignore insignificant change of area" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(100).setAreaUnit(SQ_M)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(101).setAreaUnit(SQ_M)
      val o2 = builder.build()
      assert(!OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect change of area units" in {
      val builder = TestUtils.createOffer()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(100).setAreaUnit(SQ_M)
      val o1 = builder.build()
      builder.getOfferRealtyBuilder.getAreaFullBuilder.setAreaValue(100).setAreaUnit(SOTKA)
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect significant change of description" in {
      val builder = TestUtils.createOffer()
      builder.setDescription("Some sample description")
      val o1 = builder.build()
      builder.setDescription("Same somple discrebdeon")
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "ignore insignificant change of description" in {
      val builder = TestUtils.createOffer()
      builder.setDescription("Some sample description")
      val o1 = builder.build()
      builder.setDescription("Some sample descriptions")
      val o2 = builder.build()
      assert(!OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "detect change of photos" in {
      val builder = TestUtils.createOffer()
      builder.addAllImageRef(Seq(createImageRef("http://url1.com"), createImageRef("http://url2.com")).asJava)
      val o1 = builder.build()
      builder.addImageRef(createImageRef("http://url3.com"))
      val o2 = builder.build()
      assert(OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  "Detector" should {
    "ignore draft publication" in {
      val builder = TestUtils.createOffer().putFlag(OfferFlag.OF_DRAFT)
      val o1 = builder.build()
      builder.clearFlag(OfferFlag.OF_DRAFT)
      val o2 = builder.build()
      assert(!OfferChangeDetector.hasChangedSignificantly(o1, o2))
    }
  }

  private def createImageRef(url: String): ImageRef = {
    ImageRef.newBuilder().setUrl(url).build()
  }
}
