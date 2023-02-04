package ru.yandex.realty.phone

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.adsource.{AdSourceType, UncheckedAdSource}
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.phone.RealtyPhoneTags._
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.platform.PlatformType
import ru.yandex.realty.sites.CompaniesStorage
import ru.yandex.realty.telepony.TeleponyClientMockComponents
import ru.yandex.realty.time.LocalDateTimeUtils
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.util.Date
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class CampaignOfferTagResolverSpec
  extends SpecBase
  with PropertyChecks
  with RegionGraphTestComponents
  with FeaturesStubComponent
  with TeleponyClientMockComponents {

  val now = new Date().getTime
  val nowFormatDate = LocalDateTimeUtils.millisDefaultFormat(now)
  val rtbHouseAdSource = Some(UncheckedAdSource(AdSourceType.RTB_HOUSE.toString, now))
  val criteoAdvAdSource = Some(UncheckedAdSource(AdSourceType.CRITEO_ADV.toString, now))
  val googleAdwordsAdSource = Some(UncheckedAdSource(AdSourceType.GOOGLE_ADWORDS.toString, now))
  val yandexDirectAdSource = Some(UncheckedAdSource(AdSourceType.YANDEX_DIRECT.toString, now))
  val mytargetAdSource = Some(UncheckedAdSource(AdSourceType.MYTARGET.toString, now))

  val yandexDirectAdSourceWithCampaing = Some(
    UncheckedAdSource(AdSourceType.YANDEX_DIRECT.toString, now, Some("campaign1"))
  )
  val fakeAdSource = Some(UncheckedAdSource("fake", now, None))

  val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)

  val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())

  trait CampaignOfferTagResolverFixture {

    val redirectPhoneService =
      RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)(ExecutionContext.global)

    val personalRedirectService = new PersonalRedirectService(
      redirectPhoneService,
      regionGraphProvider,
      companiesProvider,
      auctionResultProvider,
      features
    )(TestOperationalSupport)

    val manager = new CampaignOfferTagResolver(regionGraphProvider, features, personalRedirectService)
  }

  trait CampaignOfferTagResolverForPersonalTagsFixture {

    val redirectPhoneService =
      RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)(ExecutionContext.global)

    features.PersonalPhoneRedirectsEnabled.setNewState(true)

    val personalRedirectService = new PersonalRedirectService(
      redirectPhoneService,
      regionGraphProvider,
      companiesProvider,
      auctionResultProvider,
      features
    )(TestOperationalSupport)
    val manager = new CampaignOfferTagResolver(regionGraphProvider, features, personalRedirectService)
  }

  val campaignTestData =
    Table(
      ("region", "platform", "adSource", "expectedTag"),
      (Regions.MOSCOW, PlatformType.IOS, None, Some(PlatformIosCampaignTagName)),
      (Regions.MOSCOW, PlatformType.Android, None, Some(PlatformAndroidCampaignTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Desktop, None, Some(PlatformDesktopCampaignTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, None, Some(PlatformTouchCampaignTagName)),
      (Regions.KAZAN, PlatformType.IOS, None, Some(CampaignTagName)),
      (Regions.KAZAN, PlatformType.Desktop, None, Some(CampaignTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Desktop, rtbHouseAdSource, Some(RtbHouseDesktopCampaignTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Desktop, criteoAdvAdSource, Some(CriteoAdvDesktopCampaignTagName)),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        googleAdwordsAdSource,
        Some(GoogleAdwordsDesktopCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        yandexDirectAdSource,
        Some(YandexDirectDesktopCampaignTagName)
      ),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, rtbHouseAdSource, Some(RtbHouseTouchCampaignTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, criteoAdvAdSource, Some(CriteoAdvTouchCampaignTagName)),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Touch,
        googleAdwordsAdSource,
        Some(GoogleAdwordsTouchCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Touch,
        yandexDirectAdSource,
        Some(YandexDirectTouchCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        googleAdwordsAdSource,
        Some(GoogleAdwordsAndroidCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        yandexDirectAdSource,
        Some(YandexDirectAndroidCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        mytargetAdSource,
        Some(MytargetAndroidCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        googleAdwordsAdSource,
        Some(GoogleAdwordsIosCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        yandexDirectAdSource,
        Some(YandexDirectIosCampaignTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        mytargetAdSource,
        Some(MytargetIosCampaignTagName)
      )
    )

  val offerTestData =
    Table(
      ("geocoderId", "platform", "adSource", "expectedTag"),
      (Regions.MOSCOW, PlatformType.IOS, None, Some(PlatformIosOfferTagName)),
      (Regions.MOSCOW, PlatformType.Android, None, Some(PlatformAndroidOfferTagName)),
      (Regions.MOSCOW, PlatformType.Desktop, None, Some(PlatformDesktopOfferTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, None, Some(PlatformTouchOfferTagName)),
      (Regions.KAZAN, PlatformType.IOS, None, Some(NewbuildingOfferTagName)),
      (Regions.KAZAN, PlatformType.Desktop, None, Some(NewbuildingOfferTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Desktop, rtbHouseAdSource, Some(RtbHouseDesktopOfferTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Desktop, criteoAdvAdSource, Some(CriteoAdvDesktopOfferTagName)),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        googleAdwordsAdSource,
        Some(GoogleAdwordsDesktopOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        yandexDirectAdSource,
        Some(YandexDirectDesktopOfferTagName)
      ),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, rtbHouseAdSource, Some(RtbHouseTouchOfferTagName)),
      (Regions.MSK_AND_MOS_OBLAST, PlatformType.Touch, criteoAdvAdSource, Some(CriteoAdvTouchOfferTagName)),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Touch,
        googleAdwordsAdSource,
        Some(GoogleAdwordsTouchOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Touch,
        yandexDirectAdSource,
        Some(YandexDirectTouchOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        googleAdwordsAdSource,
        Some(GoogleAdwordsAndroidOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        yandexDirectAdSource,
        Some(YandexDirectAndroidOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Android,
        mytargetAdSource,
        Some(MytargetAndroidOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        googleAdwordsAdSource,
        Some(GoogleAdwordsIosOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        yandexDirectAdSource,
        Some(YandexDirectIosOfferTagName)
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.IOS,
        mytargetAdSource,
        Some(MytargetIosOfferTagName)
      )
    )

  val personalOfferTestData =
    Table(
      ("geocoderId", "platform", "adSource", "siteId", "offerId", "uuid", "yuid", "expectedTag"),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        None,
        Some(1L),
        Some("offerId1"),
        Some("uidValue"),
        None,
        Some(s"$PlatformIosOfferTagName#uuid=uidValue#siteId=1#offerId=offerId1")
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        None,
        Some(2L),
        Some("offerId2"),
        None,
        Some("yuidValue"),
        Some(s"$PlatformDesktopOfferTagName#yuid=yuidValue#siteId=2#offerId=offerId2")
      ),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        yandexDirectAdSource,
        Some(3L),
        Some("offerId3"),
        Some("uidValue"),
        None,
        Some(s"$YandexDirectIosOfferTagName#uuid=uidValue#siteId=3#offerId=offerId3#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        yandexDirectAdSourceWithCampaing,
        Some(4L),
        Some("offerId4"),
        Some("uidValue"),
        None,
        Some(
          s"$YandexDirectIosOfferTagName#uuid=uidValue#siteId=4#offerId=offerId4#utmCampaign=campaign1#utmDate=$nowFormatDate"
        )
      )
    )

  val personalCampaignTestData =
    Table(
      ("geocoderId", "platform", "adSource", "siteId", "uuid", "yuid", "expectedTag"),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"$PlatformIosCampaignTagName#uuid=uidValue2#siteId=21")
      ),
      (
        Regions.MSK_AND_MOS_OBLAST,
        PlatformType.Desktop,
        None,
        Some(22L),
        None,
        Some("yuidValue2"),
        Some(s"$PlatformDesktopCampaignTagName#yuid=yuidValue2#siteId=22")
      ),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        yandexDirectAdSource,
        Some(23L),
        Some("uidValue2"),
        None,
        Some(s"$YandexDirectIosCampaignTagName#uuid=uidValue2#siteId=23#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        PlatformType.IOS,
        yandexDirectAdSourceWithCampaing,
        Some(4L),
        Some("uidValue"),
        None,
        Some(
          s"$YandexDirectIosCampaignTagName#uuid=uidValue#siteId=4#utmCampaign=campaign1#utmDate=$nowFormatDate"
        )
      )
    )

  val uncheckedPersonalCampaignTestData =
    Table(
      ("geocoderId", "platform", "adSource", "siteId", "uuid", "yuid", "expectedTag"),
      (
        Regions.MOSCOW,
        Some(PlatformType.IOS),
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"$PlatformIosCampaignTagName#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        None,
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.IOS),
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"$PlatformIosCampaignTagName#uuid=uidValue2#siteId=21")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.Desktop),
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"$PlatformDesktopCampaignTagName#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.Desktop),
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Some(s"$PlatformDesktopCampaignTagName#uuid=uidValue2#siteId=21")
      )
    )

  val uncheckedPersonalOfferTestData =
    Table(
      ("geocoderId", "platform", "adSource", "siteId", "uuid", "yuid", "tagInfo", "expectedTag"),
      (
        Regions.MOSCOW,
        Some(PlatformType.IOS),
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Set.empty[String],
        Some(s"$PlatformIosOfferTagName#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        None,
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Set.empty[String],
        Some(s"#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.IOS),
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Set.empty[String],
        Some(s"$PlatformIosOfferTagName#uuid=uidValue2#siteId=21")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.Desktop),
        fakeAdSource,
        Some(21L),
        Some("uidValue2"),
        None,
        Set.empty[String],
        Some(s"$PlatformDesktopOfferTagName#utmSource=fake#uuid=uidValue2#siteId=21#utmDate=$nowFormatDate")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.Desktop),
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Set.empty[String],
        Some(s"$PlatformDesktopOfferTagName#uuid=uidValue2#siteId=21")
      ),
      (
        Regions.MOSCOW,
        Some(PlatformType.Desktop),
        None,
        Some(21L),
        Some("uidValue2"),
        None,
        Set("adid=rreas"),
        Some(s"$PlatformDesktopOfferTagName#uuid=uidValue2#siteId=21#adid=rreas")
      )
    )

  "CampaignOfferTagResolverSpec" should {
    features.AddPlatformTagsForPhoneRedirects.setNewState(true)
    features.AdSourceTagsForPhoneRedirects.setNewState(true)

    forAll(campaignTestData) {
      (geocoderId: Int, platform: PlatformType.Value, adSource: Option[UncheckedAdSource], expected: Tag) =>
        s"getCampaignTag region $geocoderId, platform $platform, adSource $adSource" in new CampaignOfferTagResolverFixture {
          manager.getCampaignTag(geocoderId, Some(platform), adSource, PersonalTagParams()) shouldBe expected
        }
    }

    forAll(offerTestData) {
      (geocoderId: Int, platform: PlatformType.Value, adSource: Option[UncheckedAdSource], expected: Tag) =>
        s"getOfferTag region $geocoderId, platform $platform, adSource $adSource" in new CampaignOfferTagResolverFixture {
          manager.getOfferTag(geocoderId, Some(platform), adSource, PersonalTagParams()) shouldBe expected
        }
    }

    "get None if features are disabled" in new CampaignOfferTagResolverFixture {
      features.AddPlatformTagsForPhoneRedirects.setNewState(false)
      features.AdSourceTagsForPhoneRedirects.setNewState(false)
      features.PersonalPhoneRedirectsEnabled.setNewState(false)
      manager.getOfferTag(Regions.MOSCOW, Some(PlatformType.Desktop), None, PersonalTagParams()) shouldBe None
      manager.getCampaignTag(Regions.KAZAN, Some(PlatformType.IOS), None, PersonalTagParams()) shouldBe None
      manager.getOfferTag(Regions.MOSCOW, Some(PlatformType.Desktop), rtbHouseAdSource, PersonalTagParams()) shouldBe None
      manager.getCampaignTag(Regions.KAZAN, Some(PlatformType.IOS), rtbHouseAdSource, PersonalTagParams()) shouldBe None
    }

    forAll(personalOfferTestData) {
      (
        geocoderId: Int,
        platform: PlatformType.Value,
        adSource: Option[UncheckedAdSource],
        siteId: Option[Long],
        offerId: Option[String],
        uuid: Option[String],
        yuid: Option[String],
        expected: Tag
      ) =>
        s"get personal offer tag $expected" in new CampaignOfferTagResolverForPersonalTagsFixture {
          manager.getOfferTag(
            geocoderId,
            Some(platform),
            adSource,
            PersonalTagParams(siteId, offerId, uuid, yuid, adSource.flatMap(_.adSourceCampaign))
          ) shouldBe expected
        }
    }

    forAll(personalCampaignTestData) {
      (
        geocoderId: Int,
        platform: PlatformType.Value,
        adSource: Option[UncheckedAdSource],
        siteId: Option[Long],
        uuid: Option[String],
        yuid: Option[String],
        expected: Tag
      ) =>
        s"get personal campaign tag $expected" in new CampaignOfferTagResolverForPersonalTagsFixture {
          manager.getCampaignTag(
            geocoderId,
            Some(platform),
            adSource,
            PersonalTagParams(siteId, None, uuid, yuid, adSource.flatMap(_.adSourceCampaign))
          ) shouldBe expected
        }
    }

    forAll(uncheckedPersonalCampaignTestData) {
      (
        geocoderId: Int,
        platform: Option[PlatformType.Value],
        adSource: Option[UncheckedAdSource],
        siteId: Option[Long],
        uuid: Option[String],
        yuid: Option[String],
        expected: Tag
      ) =>
        s"get personal campaign tag for fake ad source $expected" in new CampaignOfferTagResolverForPersonalTagsFixture {
          manager.getCampaignTag(
            geocoderId,
            platform,
            adSource,
            PersonalTagParams(siteId, None, uuid, yuid, adSource.flatMap(_.adSourceCampaign))
          ) shouldBe expected
        }
    }

    forAll(uncheckedPersonalOfferTestData) {
      (
        geocoderId: Int,
        platform: Option[PlatformType.Value],
        adSource: Option[UncheckedAdSource],
        siteId: Option[Long],
        uuid: Option[String],
        yuid: Option[String],
        tagInfo: Set[String],
        expected: Tag
      ) =>
        s"get personal offer tag for fake ad source $expected" in new CampaignOfferTagResolverForPersonalTagsFixture {
          manager.getOfferTag(
            geocoderId,
            platform,
            adSource,
            PersonalTagParams(siteId, None, uuid, yuid, adSource.flatMap(_.adSourceCampaign), tagInfo)
          ) shouldBe expected
        }
    }

  }
}
