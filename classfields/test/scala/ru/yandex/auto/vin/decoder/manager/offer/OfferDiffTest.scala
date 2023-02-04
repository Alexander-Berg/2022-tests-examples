package ru.yandex.auto.vin.decoder.manager.offer

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.manager.offer.OfferDiff.{OfferUpdate, UpdateExtractor}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfo

import scala.jdk.CollectionConverters.ListHasAsScala

class OfferDiffTest extends AnyWordSpecLike with MockitoSugar with Matchers {

  def beatenDescriptor: Descriptors.FieldDescriptor =
    VinInfo.getDefaultInstance.getDescriptorForType.getFields.asScala.find(_.getName == "beaten").get

  def markDescriptor: Descriptors.FieldDescriptor =
    VinInfo.getDefaultInstance.getDescriptorForType.getFields.asScala.find(_.getName == "mark").get

  "OfferUpdate" should {
    "combine empty correсtly" in {
      val empty = OfferUpdate.Empty
      val nonEmpty = OfferUpdate(c => c, "name")
      empty.combine(nonEmpty).getName shouldBe "name"
      empty.combine(empty).getName shouldBe "empty"
    }

    "update when created directly" in {
      val update = OfferUpdate(_.setBeaten(true), "")
      val offer = VinInfo.newBuilder().setBeaten(false)
      update(offer).getBeaten shouldBe true
    }

    "update when created by field" in {
      val b = true.asInstanceOf[AnyRef]
      val update = OfferUpdate.apply(beatenDescriptor: FieldDescriptor, b)
      val offer = VinInfo.newBuilder().setBeaten(false)
      update(offer).getBeaten shouldBe true
    }
  }

  "UpdateExtractor" should {
    "extract field update" in {
      val current = VinInfo.newBuilder().setMark("HONDA")
      val prev = VinInfo.newBuilder().setMark("BMW")
      val extractor = UpdateExtractor(markDescriptor)
      val res = extractor.getUpdate(current.build(), prev.build())
      res.isDefined shouldBe true
      res.get.getName shouldBe "mark"
      res.get.apply(prev).getMark shouldBe "HONDA"
    }

    "skip field update if not changed" in {
      val current = VinInfo.newBuilder().setMark("HONDA")
      val prev = VinInfo.newBuilder().setMark("HONDA")
      val extractor = UpdateExtractor(markDescriptor)
      val res = extractor.getUpdate(current.build(), prev.build())
      res.isDefined shouldBe false
    }
  }

  "igonre update extractor" should {
    "remove ignore if it is possible" in {
      val current = VinInfo.newBuilder().setIgnored(false)
      val prev = VinInfo.newBuilder().setIgnored(true).setIgnoreReason(VinOffersManager.IGNORE_BY_CHANGED_VIN)
      val res = OfferDiff.ignoreUpdateExtractor.getUpdate(current.build(), prev.build())
      res.isDefined shouldBe true
      res.get.apply(prev).getIgnored shouldBe false
    }

    "do not remove ignore if it is  not possible" in {
      val current = VinInfo.newBuilder().setIgnored(false)
      val prev = VinInfo.newBuilder().setIgnored(true).setIgnoreReason(VinOffersManager.IGNORE_BY_SUPPORT)
      val res = OfferDiff.ignoreUpdateExtractor.getUpdate(current.build(), prev.build())
      res.isDefined shouldBe false
    }

    "do add ignore" in {
      val current = VinInfo.newBuilder().setIgnored(true).setIgnoreReason(VinOffersManager.IGNORE_BY_SUPPORT)
      val prev = VinInfo.newBuilder().setIgnored(false)
      val res = OfferDiff.ignoreUpdateExtractor.getUpdate(current.build(), prev.build())
      res.isDefined shouldBe true
      res.get.apply(prev).getIgnored shouldBe true
      res.get.apply(prev).getIgnoreReason shouldBe VinOffersManager.IGNORE_BY_SUPPORT
    }
  }

}
