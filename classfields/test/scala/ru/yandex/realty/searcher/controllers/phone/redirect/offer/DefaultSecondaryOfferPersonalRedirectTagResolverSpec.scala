package ru.yandex.realty.searcher.controllers.phone.redirect.offer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.adsource.UncheckedAdSource
import ru.yandex.realty.phone.PhoneTagRequestParams
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.platform.PlatformType
import ru.yandex.realty.time.LocalDateTimeUtils

@RunWith(classOf[JUnitRunner])
class DefaultSecondaryOfferPersonalRedirectTagResolverSpec extends SpecBase with PropertyChecks {

  private val tagResolver = new DefaultSecondaryOfferPersonalRedirectTagResolver()
  private val adSourceTimestamp = 1643804930917L
  private val adSourceType = "googleAd"
  private val utmDate = LocalDateTimeUtils.millisDefaultFormat(adSourceTimestamp)
  private val emptyTagParams = PhoneTagRequestParams(None, None, Set.empty)
  private val tagsTestData =
    Table(
      ("defaultTag", "offerId", "tagParams", "platformType", "uuid", "yuid", "expected"),
      (None, "123", emptyTagParams, None, None, None, Some("offerid=123")),
      (Some("someTag1"), "", emptyTagParams, None, None, None, Some("someTag1")),
      (Some("someTag2"), "123", emptyTagParams, None, None, None, Some("someTag2#offerid=123")),
      (Some("someTag3#offerid=334"), "123", emptyTagParams, None, None, None, Some("someTag3#offerid=334")),
      (
        Some("someTag4"),
        "",
        PhoneTagRequestParams(None, Some(UncheckedAdSource(adSourceType, adSourceTimestamp, None)), Set.empty),
        None,
        None,
        None,
        Some(s"someTag4#utmSource=$adSourceType#utmDate=$utmDate")
      ),
      (Some("someTag5"), "", emptyTagParams, Some(PlatformType.IOS), None, None, Some("someTag5#PlatformIos")),
      (Some("someTag6"), "", emptyTagParams, None, Some("1"), None, Some("someTag6#uuid=1")),
      (Some("someTag7"), "", emptyTagParams, None, None, Some("2"), Some("someTag7#yuid=2")),
      (Some("someTag8"), "", emptyTagParams, None, Some("1"), Some("2"), Some("someTag8#uuid=1")),
      (
        Some("someTag9"),
        "",
        PhoneTagRequestParams(None, None, Set("adid=asd3")),
        None,
        None,
        None,
        Some("someTag9#adid=asd3")
      ),
      (
        Some("someTag10"),
        "432",
        PhoneTagRequestParams(None, Some(UncheckedAdSource(adSourceType, adSourceTimestamp, None)), Set("ddd=923rtg")),
        Some(PlatformType.IOS),
        Some("1"),
        Some("2"),
        Some(s"someTag10#offerid=432#PlatformIos#utmSource=$adSourceType#utmDate=$utmDate#uuid=1#ddd=923rtg")
      )
    )

  "DefaultSecondaryOfferPersonalRedirectTagResolver" should {
    forAll(tagsTestData) {
      (
        defaultTag: Tag,
        offerId: String,
        tagParams: PhoneTagRequestParams,
        platformType: Option[PlatformType.Value],
        uuid: Option[String],
        yuid: Option[String],
        expected: Tag
      ) =>
        "resolveTag for " + defaultTag in {
          tagResolver.resolveTag(defaultTag, offerId, tagParams, platformType, uuid, yuid) shouldBe expected
        }
    }
  }
}
