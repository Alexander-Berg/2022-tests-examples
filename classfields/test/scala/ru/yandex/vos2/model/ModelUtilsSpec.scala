package ru.yandex.vos2.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.OfferModel.Multiposting.{Classified => VosClassified}
import ru.auto.api.ApiOfferModel.Multiposting.{Classified => ApiClassified}

class ModelUtilsSpec extends AnyWordSpec with Matchers {

  "Multiposting: convert classified from ApiModel to VosModel" should {
    "convert statuses" in {
      val statuses: Map[OfferStatus, CompositeStatus] = Map(
        OfferStatus.ACTIVE -> CompositeStatus.CS_ACTIVE,
        OfferStatus.INACTIVE -> CompositeStatus.CS_INACTIVE,
        OfferStatus.BANNED -> CompositeStatus.CS_BANNED,
        OfferStatus.REMOVED -> CompositeStatus.CS_REMOVED,
        OfferStatus.DRAFT -> CompositeStatus.CS_DRAFT
      )

      statuses.foreach {
        case (apiStatus, vosStatus) =>
          val apiClassified = ApiClassified
            .newBuilder()
            .setId("offerId")
            .setStatus(apiStatus)
            .build()

          val expected = VosClassified
            .newBuilder()
            .setId("offerId")
            .setStatus(vosStatus)
            .build()

          apiClassified.toVosModel shouldBe expected
      }
    }

    "convert not filled statuses" in {
      val apiClassified = ApiClassified
        .newBuilder()
        .setId("offerId")
        .setUrl("offerUrl")
        .build()

      val expected = VosClassified
        .newBuilder()
        .setId("offerId")
        .setUrl("offerUrl")
        .build()

      val result = apiClassified.toVosModel

      result shouldBe expected

      apiClassified.getStatus shouldBe OfferStatus.STATUS_UNKNOWN
      result.getStatus shouldBe CompositeStatus.CS_UNKNOWN
    }

    "convert with services" in {
      val apiClassified = ApiClassified
        .newBuilder()
        .setCreateDate(111L)
        .setStartDate(222L)
        .setExpireDate(333L)
        .setEnabled(true)
        .setName(ApiClassified.ClassifiedName.AUTORU)
        .setStatus(OfferStatus.NEED_ACTIVATION)
        .setId("offerId")
        .setUrl("offerUrl")
        .addServices {
          ApiClassified.Service
            .newBuilder()
            .setService("someService")
            .setCreateDate(777L)
            .setStartDate(888L)
            .setExpireDate(999L)
            .setIsActive(true)
            .setBadge("someBadge")
        }
        .build()

      val expected = VosClassified
        .newBuilder()
        .setCreateDate(111L)
        .setStartDate(222L)
        .setExpireDate(333L)
        .setEnabled(true)
        .setName(VosClassified.ClassifiedName.AUTORU)
        .setStatus(CompositeStatus.CS_NEED_ACTIVATION)
        .setId("offerId")
        .setUrl("offerUrl")
        .addServices {
          VosClassified.Service
            .newBuilder()
            .setService("someService")
            .setCreateDate(777L)
            .setStartDate(888L)
            .setExpireDate(999L)
            .setIsActive(true)
            .setBadge("someBadge")
        }
        .build()

      apiClassified.toVosModel shouldBe expected
    }
  }

  "Multiposting: convert classified from VosModel to ApiModel" should {
    "convert statuses" in {
      val statuses: Map[CompositeStatus, OfferStatus] = Map(
        CompositeStatus.CS_ACTIVE -> OfferStatus.ACTIVE,
        CompositeStatus.CS_INACTIVE -> OfferStatus.INACTIVE,
        CompositeStatus.CS_BANNED -> OfferStatus.BANNED,
        CompositeStatus.CS_REMOVED -> OfferStatus.REMOVED,
        CompositeStatus.CS_DRAFT -> OfferStatus.DRAFT
      )

      statuses.foreach {
        case (vosStatus, apiStatus) =>
          val vosClassified = VosClassified
            .newBuilder()
            .setId("offerId")
            .setStatus(vosStatus)
            .build()

          val expected = ApiClassified
            .newBuilder()
            .setId("offerId")
            .setStatus(apiStatus)
            .build()

          vosClassified.toApiModel(photos = None) shouldBe expected
      }
    }

    "convert not filled statuses" in {
      val vosClassified = VosClassified
        .newBuilder()
        .setId("offerId")
        .setUrl("offerUrl")
        .build()

      val expected = ApiClassified
        .newBuilder()
        .setId("offerId")
        .setUrl("offerUrl")
        .build()

      val result = vosClassified.toApiModel(photos = None)

      result shouldBe expected

      vosClassified.getStatus shouldBe CompositeStatus.CS_UNKNOWN
      result.getStatus shouldBe OfferStatus.STATUS_UNKNOWN
    }

    "convert with services" in {
      val vosClassified = VosClassified
        .newBuilder()
        .setCreateDate(111L)
        .setStartDate(222L)
        .setExpireDate(333L)
        .setEnabled(true)
        .setName(VosClassified.ClassifiedName.AUTORU)
        .setStatus(CompositeStatus.CS_NEED_ACTIVATION)
        .setId("offerId")
        .setUrl("offerUrl")
        .addServices {
          VosClassified.Service
            .newBuilder()
            .setService("someService")
            .setCreateDate(777L)
            .setStartDate(888L)
            .setExpireDate(999L)
            .setIsActive(true)
            .setBadge("someBadge")
        }
        .build()

      val expected = ApiClassified
        .newBuilder()
        .setCreateDate(111L)
        .setStartDate(222L)
        .setExpireDate(333L)
        .setEnabled(true)
        .setName(ApiClassified.ClassifiedName.AUTORU)
        .setStatus(OfferStatus.NEED_ACTIVATION)
        .setId("offerId")
        .setUrl("offerUrl")
        .addServices {
          ApiClassified.Service
            .newBuilder()
            .setService("someService")
            .setCreateDate(777L)
            .setStartDate(888L)
            .setExpireDate(999L)
            .setIsActive(true)
            .setBadge("someBadge")
        }
        .build()

      vosClassified.toApiModel(photos = None) shouldBe expected
    }
  }

}
