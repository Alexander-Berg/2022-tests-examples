package ru.yandex.vertis.moderation.httpclient.vin

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.auto.api.vin.VinApiModel
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.instance.{ExternalId, User}

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class RichVinModerationOfferItemSpec extends SpecBase {

  private case class ExternalIdTestCase(description: String, item: VinApiModel.OfferItem, expected: Option[ExternalId])

  private val externalIdTestCases: Seq[ExternalIdTestCase] =
    Seq(
      ExternalIdTestCase(
        description = "Private users offer",
        item = VinApiModel.OfferItem.newBuilder.setOfferId("OFFER-ID-1").setUser("user:USER-ID-1").build(),
        expected = Some(ExternalId(User.Autoru("USER-ID-1"), "OFFER-ID-1"))
      ),
      ExternalIdTestCase(
        description = "Dealer users offer",
        item = VinApiModel.OfferItem.newBuilder.setOfferId("OFFER-ID-2").setUser("dealer:DEALER-ID-2").build(),
        expected = Some(ExternalId(User.Dealer("DEALER-ID-2"), "OFFER-ID-2"))
      )
    )

  "RichVinModerationOfferItem" should {
    externalIdTestCases.foreach { case ExternalIdTestCase(description, item, expected) =>
      description in {
        item.externalId shouldBe expected
      }
    }

    "Failed to parse user" in {
      intercept[IllegalArgumentException] {
        VinApiModel.OfferItem.newBuilder().setUser("incorrect-id").build().user
      }
    }
  }
}
