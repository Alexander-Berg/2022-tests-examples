package ru.yandex.realty.telepony

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.CacheHolder
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.message.ExtDataSchema.{
  DatePatternMessage,
  SalesDepartmentMessage,
  TimeIntervalMessage,
  WeekTimetableMessage
}
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.sites.CompaniesStorage
import ru.yandex.realty.storage.verba.VerbaStorage

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RedirectPhoneUtilsSpec extends SpecBase with RedirectPhoneUtils {

  val siteId = 0L

  val emptyCallbackInfoSiteId = 1L

  val emptyTargetNameSiteId = 2L

  val companyId = 0L

  val auctionResultStorage: AuctionResultStorage = mock[AuctionResultStorage]

  val April18Daytime: DateTime = DateTime.parse("2022-04-18T12:00:00") // Monday

  val companiesStorage: CompaniesStorage = new CompaniesStorage(
    List({
      val company = new Company(companyId)
      company.setName("Company")
      company
    }).asJava
  )

  protected val cacheHolder: CacheHolder = new CacheHolder

  protected val regionGraphProvider: Provider[RegionGraph] = RegionGraphTestComponents.regionGraphProvider

  protected val verbaProvider: Provider[VerbaStorage] = mock[Provider[VerbaStorage]]

  protected val phoneUnifierClient: PhoneUnifierClient = mock[PhoneUnifierClient]

  protected val auctionResultProvider: Provider[AuctionResultStorage] = () => auctionResultStorage

  protected val companiesProvider: Provider[CompaniesStorage] = () => companiesStorage

  "RedirectPhoneUtilsSpec" should {
    "Return redirect callback info if we have targetName and callPeriods" in new RedirectCallbackInfoFixtures {
      DateTimeUtils.setCurrentMillisFixed(April18Daytime.getMillis)
      val callbackInfo = getRedirectCallbackInfo(siteId)
      DateTimeUtils.setCurrentMillisSystem()
      callbackInfo shouldNot be(None)

    }

    "Return none if we have only targetName" in new RedirectCallbackInfoFixtures {
      DateTimeUtils.setCurrentMillisFixed(April18Daytime.getMillis)
      val callbackInfo = getRedirectCallbackInfo(emptyCallbackInfoSiteId)
      DateTimeUtils.setCurrentMillisSystem()
      callbackInfo should be(None)
    }

    "Return none info if we have only callback periods" in new RedirectCallbackInfoFixtures {
      DateTimeUtils.setCurrentMillisFixed(April18Daytime.getMillis)
      val callbackInfo = getRedirectCallbackInfo(emptyTargetNameSiteId)
      DateTimeUtils.setCurrentMillisSystem()
      callbackInfo should be(None)
    }
  }

  trait RedirectCallbackInfoFixtures {
    (auctionResultStorage
      .getDepartmentsForSiteId(_: Long))
      .expects(siteId)
      .returning(IndexedSeq(getNoneEmptySalesDepartmentMessage))
      .noMoreThanTwice()

    (auctionResultStorage
      .getDepartmentsForSiteId(_: Long))
      .expects(emptyCallbackInfoSiteId)
      .returning(IndexedSeq(getEmptySalesDepartmentMessage))
      .noMoreThanTwice()

    (auctionResultStorage
      .getDepartmentsForSiteId(_: Long))
      .expects(emptyTargetNameSiteId)
      .returning(IndexedSeq())
      .noMoreThanTwice()
  }

  def getNoneEmptySalesDepartmentMessage: SalesDepartmentMessage =
    SalesDepartmentMessage
      .newBuilder()
      .setWeekTimetable(
        WeekTimetableMessage
          .newBuilder()
          .setTimeZone("Europe/Moscow")
          .addDatePatterns(
            DatePatternMessage
              .newBuilder()
              .setFrom(1)
              .setTo(7)
              .addTimeIntervals(TimeIntervalMessage.newBuilder().setFrom("09:00").setTo("21:00").build())
              .build()
          )
          .build()
      )
      .setCampaignId(companyId.toString)
      .build()

  def getEmptySalesDepartmentMessage: SalesDepartmentMessage =
    SalesDepartmentMessage
      .newBuilder()
      .setWeekTimetable(
        WeekTimetableMessage
          .newBuilder()
          .setTimeZone("Europe/Moscow")
          .addDatePatterns(
            DatePatternMessage
              .newBuilder()
              .build()
          )
          .build()
      )
      .setCampaignId(companyId.toString)
      .build()
}
