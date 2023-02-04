package ru.yandex.realty.searcher.controllers.phone.redirect.offer

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.adsource.UncheckedAdSource
import ru.yandex.realty.auth.{Application, AuthInfo}
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{Offer, OfferType, PhoneNumber}
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.phone.PhoneTagRequestParams
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.platform.{PlatformInfo, PlatformType}
import ru.yandex.realty.request.RequestImpl
import ru.yandex.realty.searcher.api.SearcherApi.OfferPhonesResponse
import ru.yandex.realty.searcher.controllers.phone.PhoneSearchersHelper.redirectPhonesResponse
import ru.yandex.realty.services.telepony.{TelephonyTag, TelephonyTagBuilder}
import ru.yandex.realty.telepony.TeleponyClient.Domain
import ru.yandex.realty.tracing.Traced

import java.util
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DefaultSecondaryOfferPersonalRedirectManagerSpec extends SpecBase with PropertyChecks {

  private val availabilityService = mock[SecondaryOfferPersonalRedirectAvailabilityService]
  private val tagResolver = mock[SecondaryOfferPersonalRedirectTagResolver]
  private val redirectService = mock[SecondaryOfferPersonalRedirectService]
  private val teleponyTagBuilder = mock[TelephonyTagBuilder]
  private val manager = new DefaultSecondaryOfferPersonalRedirectManager(
    availabilityService,
    tagResolver,
    redirectService,
    teleponyTagBuilder
  )

  private val adSourceTimestamp = 1643804930917L
  private val adSourceType = "googleAd"
  private val adSource = UncheckedAdSource(adSourceType, adSourceTimestamp, None)
  private val geocoderId = 1123
  private val subjectFederationId = Regions.MSK_AND_MOS_OBLAST
  private val redirect = preparePhoneRedirect()
  private val defaulTag: Some[String] = Some("offerid=123")
  private val redirectsTestData =
    Table(
      ("description", "redirectsAvailable", "numberPoolIsReady", "tag", "redirect", "expected"),
      (
        "redirects disabled or unavailable in the given region",
        false,
        true,
        Some("nicePersonalTag"),
        Some(redirect),
        None
      ),
      (
        "redirects enabled but computed tag is equal to default",
        true,
        true,
        defaulTag,
        Some(redirect),
        None
      ),
      (
        "redirects enabled and tag is fine, but redirect creation failed",
        true,
        true,
        Some("nicePersonalTag"),
        None,
        None
      ),
      (
        "redirects enabled and all ok",
        true,
        true,
        Some("nicePersonalTag"),
        Some(redirect),
        Some(redirectPhonesResponse(Seq(redirect)))
      ),
      (
        "redirects enabled but numberPool is empty",
        true,
        false,
        Some("nicePersonalTag"),
        Some(redirect),
        None
      )
    )

  "DefaultSecondaryOfferPersonalRedirectManager" should {
    forAll(redirectsTestData) {
      (
        description: String,
        redirectsAvailable: Boolean,
        numberPoolIsReady: Boolean,
        tag: Tag,
        redirect: Option[PhoneRedirect],
        expected: Option[OfferPhonesResponse]
      ) =>
        "getPersonalRedirects " + description in {
          val offer = prepareOffer()

          val request = new RequestImpl
          request.setApplication(Application.UnitTests)
          request.setAuthInfo(AuthInfo())
          request.setPlatformInfo(Option(PlatformInfo("ios", "")))

          (availabilityService
            .isApplicableForPersonalRedirect(_: Int, _: Option[Int], _: OfferType))
            .expects(geocoderId, Some(subjectFederationId), OfferType.SELL)
            .once()
            .returning(redirectsAvailable)

          (redirectService
            .isNumberPoolReady(_: Int)(_: Traced))
            .expects(subjectFederationId, *)
            .once()
            .repeat(if (redirectsAvailable) 1 else 0)
            .returning(numberPoolIsReady)

          (teleponyTagBuilder
            .build(_: Offer))
            .expects(offer)
            .repeat(if (redirectsAvailable) 1 else 0)
            .returning(prepareTeleponyTag)

          val params = PhoneTagRequestParams(None, Some(adSource), Set.empty)
          (tagResolver
            .resolveTag(
              _: Tag,
              _: String,
              _: PhoneTagRequestParams,
              _: Option[PlatformType.Value],
              _: Option[String],
              _: Option[String]
            ))
            .expects(
              *,
              "123",
              params,
              Some(PlatformType.IOS),
              None,
              None
            )
            .repeat(if (redirectsAvailable) 1 else 0)
            .returning(tag)

          (redirectService
            .createRedirect(_: Offer, _: String, _: Tag)(_: Traced))
            .expects(offer, *, tag, *)
            .repeat(if (redirectsAvailable && tag != defaulTag && numberPoolIsReady) 1 else 0)
            .returning(redirect)

          manager.shouldUsePersonalRedirect(offer) shouldBe redirectsAvailable

          if (redirectsAvailable) {
            val response = manager.getPersonalRedirects(
              offer,
              params
            )(request)

            response shouldBe expected
          }
        }
    }
  }

  private def prepareTeleponyTag = {
    TelephonyTag(
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("123"),
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

  private def prepareOffer() = {
    val offer = new Offer()
    offer.setId(123L)
    offer.setOfferType(OfferType.SELL)
    offer.setPartnerId(987L)
    val location = new Location()
    location.setGeocoderId(geocoderId)
    location.setSubjectFederation(subjectFederationId, NodeRgid.MOS_OBLAST)
    offer.setLocation(location)
    val agent = offer.createAndGetSaleAgent()
    val phoneNumber = new PhoneNumber("+79998888888")
    val phones = new util.ArrayList[PhoneNumber]()
    phones.add(phoneNumber)
    agent.setUnifiedPhones(phones)
    offer
  }

  private def preparePhoneRedirect() = {
    PhoneRedirect(
      Domain.`realty-offers`,
      "",
      "partner_987",
      Some("tag"),
      new DateTime(),
      None,
      "1111",
      "2222",
      Some(PhoneType.Mobile),
      None,
      Some(2.days)
    )
  }
}
