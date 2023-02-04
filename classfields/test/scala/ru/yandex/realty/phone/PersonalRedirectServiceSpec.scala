package ru.yandex.realty.phone

import org.joda.time.{DateTime, DateTimeUtils, Duration}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.RedirectPhoneComponents
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.message.ExtDataSchema.SalesDepartmentMessage
import ru.yandex.realty.model.phone.RealtyPhoneTags.{EmptyTagName, PersonalDefaultTagName}
import ru.yandex.realty.model.phone.{PhoneRedirect, RealtyPhoneTags, TeleponyInfo}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.sites.{Company, Site}
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.sites.CompaniesStorage
import ru.yandex.realty.telepony.TeleponyClient
import ru.yandex.realty.telepony.TeleponyClient.Antifraud
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.realty.model.sites.{Company, Site}
import ru.yandex.realty.sites.CompaniesStorage
import ru.yandex.realty.util.time.TimezoneUtils

import java.time.LocalDateTime
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PersonalRedirectServiceSpec
  extends SpecBase
  with RegionGraphTestComponents
  with FeaturesStubComponent
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  override def beforeEach(): Unit = {
    DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T14:00:01").getMillis)
  }

  override def afterAll(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  trait PersonalRedirectServiceFixture {
    val teleponyClient: TeleponyClient = mock[TeleponyClient]
    val redirectPhoneService: RedirectPhoneService = RedirectPhoneComponents.createRedirectPhoneService(teleponyClient)
    val tag: Some[String] = Some(RealtyPhoneTags.PlatformDesktopCampaignTagName + "#yuid=837254#siteId=12")
    val site: Site = new Site(1)
    val companiesProvider: Provider[CompaniesStorage] = () => new CompaniesStorage(Seq[Company]().asJava)
    val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())

    val manager = new PersonalRedirectService(
      redirectPhoneService,
      regionGraphProvider,
      companiesProvider,
      auctionResultProvider,
      features
    )(TestOperationalSupport)

    val salesDepartment: SalesDepartmentMessage = SalesDepartmentMessage
      .newBuilder()
      .addPhoneRedirect(
        PhoneRedirectMessage
          .newBuilder()
          .setTag(EmptyTagName)
          .setGeoId(1)
          .setDomain("billing-realty")
          .setSource("911")
          .setTarget("926")
      )
      .addPhones("926")
      .build()
  }

  "PersonalRedirectService" should {

    " if feature is disabled " should {
      "check for personal redirect returns false" in new PersonalRedirectServiceFixture {
        manager.isApplicableForPersonalRedirect(Regions.MSK_AND_MOS_OBLAST) shouldBe false
      }
      "create personal redirect returns none" in new PersonalRedirectServiceFixture {
        val location = new Location()
        location.setGeocoderId(Regions.MOSCOW)
        site.setLocation(location)

        val phoneRedirectOpt: Option[PhoneRedirect] =
          manager.createPhoneRedirect(tag, salesDepartment, site)(Traced.empty)
        phoneRedirectOpt shouldBe None
      }
    }
    " if feature is enabled " should {
      "check for personal redirect returns false on incorrect region" in new PersonalRedirectServiceFixture {
        features.PersonalPhoneRedirectsEnabled.setNewState(true)
        manager.isApplicableForPersonalRedirect(Regions.YAROSLAVSKAYA_OBLAST) shouldBe false
      }
      "check for personal redirect returns true" in new PersonalRedirectServiceFixture {
        features.PersonalPhoneRedirectsEnabled.setNewState(true)
        manager.isApplicableForPersonalRedirect(Regions.MSK_AND_MOS_OBLAST) shouldBe true
      }
      "create personal redirect returns redirect" in new PersonalRedirectServiceFixture {
        features.PersonalPhoneRedirectsEnabled.setNewState(true)

        val redirect: PhoneRedirect = buildPhoneRedirect(tag, "campaignId", "968", "926")
        (teleponyClient
          .getOrCreate(_: TeleponyInfo, _: Option[Antifraud])(_: Traced))
          .expects(where { (t: TeleponyInfo, _, _) =>
            t.tag == tag
          })
          .returning(Future.successful(redirect))

        val location = new Location()
        location.setGeocoderId(Regions.MOSCOW)
        location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOS_OBLAST)
        site.setLocation(location)

        val phoneRedirectOpt: Option[PhoneRedirect] =
          manager.createPhoneRedirect(tag, salesDepartment, site)(Traced.empty)
        phoneRedirectOpt should not be empty
      }
      "create personal redirect in inappropriate time" in new PersonalRedirectServiceFixture {
        features.PersonalPhoneRedirectsEnabled.setNewState(true)
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T22:00:01").getMillis)

        val redirect: PhoneRedirect = buildPhoneRedirect(Some(PersonalDefaultTagName), "campaignId", "968", "926")
        (teleponyClient
          .getOrCreate(_: TeleponyInfo, _: Option[Antifraud])(_: Traced))
          .expects(where { (t: TeleponyInfo, _, _) =>
            t.tag.get == PersonalDefaultTagName
          })
          .returning(Future.successful(redirect))

        val location = new Location()
        location.setGeocoderId(Regions.MOSCOW)
        location.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOS_OBLAST)
        site.setLocation(location)

        val phoneRedirectOpt: Option[PhoneRedirect] =
          manager.createPhoneRedirect(tag, salesDepartment, site)(
            Traced.empty
          )
        phoneRedirectOpt should not be empty
      }
      "create personal redirect in inappropriate time in other region" in new PersonalRedirectServiceFixture {
        features.PersonalPhoneRedirectsEnabled.setNewState(true)
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T06:59:59").getMillis)

        val redirect: PhoneRedirect = buildPhoneRedirect(tag, "campaignId", "968", "926")
        (teleponyClient
          .getOrCreate(_: TeleponyInfo, _: Option[Antifraud])(_: Traced))
          .expects(where { (t: TeleponyInfo, _, _) =>
            t.tag.get == PersonalDefaultTagName
          })
          .returning(Future.successful(redirect))

        val spbLocation = new Location()
        spbLocation.setGeocoderId(Regions.SPB)
        spbLocation.setSubjectFederation(Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST)
        site.setLocation(spbLocation)

        val phoneRedirectOpt: Option[PhoneRedirect] =
          manager.createPhoneRedirect(tag, salesDepartment, site)(
            Traced.empty
          )
        phoneRedirectOpt should not be empty
      }
    }
    "check all regions have corresponding timezone" in {
      PersonalRedirectService.PersonalRedirectRegions
        .exists(geoId => TimezoneUtils.findBySubjectFederationId(geoId).isEmpty) shouldBe false
    }
  }

  private def buildPhoneRedirect(
    tag: Option[String],
    objectId: String,
    source: String,
    target: String
  ): PhoneRedirect = {
    PhoneRedirect(
      "billing-realty",
      id = System.nanoTime().toString,
      tag = tag,
      objectId = objectId,
      createTime = new DateTime(),
      deadline = Some(new DateTime().plus(Duration.standardDays(1))),
      source = source,
      target = target,
      phoneType = None,
      geoId = None,
      ttl = None
    )
  }

}
