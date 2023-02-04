package ru.yandex.realty.searcher.controllers.phone.redirect.offer

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType}
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.telepony.TeleponyClient.Domain
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.tracing.Traced

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DefaultSecondaryOfferPersonalRedirectServiceSpec
  extends SpecBase
  with PropertyChecks
  with TeleponyClientMockComponents
  with FeaturesStubComponent {

  private val redirectPhoneService =
    RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)(ExecutionContext.global)
  private val service = new DefaultSecondaryOfferPersonalRedirectService(
    redirectPhoneService,
    features
  )
  private val offer = prepareOffer()
  private val offerTestData =
    Table(
      ("offer", "expected"),
      (offer, preparePhoneRedirect("921", Some("someTag"))),
      (offer, preparePhoneRedirect("926", Some("anotherTag")))
    )

  "DefaultSecondaryOfferPersonalRedirectService" should {
    forAll(offerTestData) { (offer: Offer, expected: PhoneRedirect) =>
      "createRedirect for " + expected in {
        expectTeleponyCall(expected)

        val phoneRedirectOpt = service.createRedirect(
          offer,
          expected.target,
          expected.tag
        )(Traced.empty)
        phoneRedirectOpt shouldBe Some(expected)
      }

      "createRedirect fail for " + expected in {
        expectTeleponyCallFailed(expected)

        val phoneRedirectOpt = service.createRedirect(
          offer,
          expected.target,
          expected.tag
        )(Traced.empty)
        phoneRedirectOpt shouldBe None
      }
    }
  }

  private def preparePhoneRedirect(phone: String, tag: Tag) = {
    PhoneRedirect(
      Domain.`realty-offers`,
      "",
      "partner_987",
      tag,
      new DateTime(),
      None,
      "1111",
      phone,
      Some(PhoneType.Mobile),
      None,
      Some(2.days)
    )
  }

  private def prepareOffer() = {
    val offer = new Offer()
    offer.setId(123L)
    offer.setPartnerId(987L)
    offer
  }
}
