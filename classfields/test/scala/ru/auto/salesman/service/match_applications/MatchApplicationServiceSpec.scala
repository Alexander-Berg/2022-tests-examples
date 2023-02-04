package ru.auto.salesman.service.match_applications

import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util
import java.util.concurrent.TimeUnit.SECONDS

import auto.indexing.CampaignByClientOuterClass.CampaignHeaderList
import cats.data.NonEmptyList
import cats.implicits._
import org.scalacheck.Gen
import org.scalactic.source.Position
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.MatchApplication.{
  ErrorCodes,
  MatchApplicationCreateForm,
  MatchApplicationCreateForms,
  MatchApplicationCreateResponse
}
import ru.auto.salesman.client.{PublicApiClient, SenderClient}
import ru.auto.salesman.dao.MatchApplicationDao.Filter.{
  ForDateTimeInterval,
  WithClients,
  WithMatchApplicationId
}
import ru.auto.salesman.dao.MatchApplicationDao.Update
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.LimitOffset
import ru.auto.salesman.dao.{ClientDao, ClientSubscriptionsDao}
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.mocks.{
  MatchApplicationDaoMock,
  MatchApplicationsNotificationSenderMock
}
import ru.auto.salesman.model.common.{PageModel, PagingModel}
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.MatchApplicationId
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{AutoruUser, ClientId}
import ru.auto.salesman.service.billingcampaign.{BillingCampaignService, BillingTestData}
import ru.auto.salesman.service.match_applications.MatchApplicationService.{
  MatchApplicationsModel,
  SalesmanMatchApplicationNotFoundException
}
import ru.auto.salesman.service.match_applications.MatchApplicationServiceSpec._
import ru.auto.salesman.test.model.gens.MatchApplicationGenerators.{
  RichMatchApplicationCreateRequestGen,
  _
}
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval, Page}
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.util.time.DateTimeUtil
import zio.duration.Duration
import zio.test.environment.{TestClock, TestEnvironment}

import scala.collection.JavaConverters._

class MatchApplicationServiceSpec extends BaseSpec with BeforeAndAfter {

  private val rc = AutomatedContext("test")

  val dao = new MatchApplicationDaoMock

  val billingCampaignService = mock[BillingCampaignService]

  val matchApplicationsNotificationSender =
    new MatchApplicationsNotificationSenderMock(
      mock[PublicApiClient],
      mock[ClientSubscriptionsDao],
      mock[SenderClient],
      mock[ClientDao]
    )

  val service = new MatchApplicationService(
    dao.getMock(),
    billingCampaignService,
    matchApplicationsNotificationSender
  )

  "MatchApplicationService" should {
    "save all match application request forms" in {
      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId2, matchApplicationId2))
        .returningZ(true)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(Some(campaignHeaders(isActive = true)))

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value
      val client2Notification = matchApplicationsNotificationSender
        .mockForClient(clientId2, notificationStatus = true)
        .success
        .value

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe None

      client1Notification.await.timeout(Duration(1, SECONDS)).success
      client2Notification.await.timeout(Duration(1, SECONDS)).success
    }

    "response with success if notifications sending fails" in {
      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId2, matchApplicationId2))
        .returningZ(true)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(Some(campaignHeaders(isActive = true)))

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value

      val client2Notification = matchApplicationsNotificationSender
        .mockForClient(clientId2, notificationStatus = false)
        .success
        .value

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe None

      client1Notification.await.timeout(Duration(1, SECONDS)).success
      client2Notification.await
        .timeout(Duration(1, SECONDS))
        .failure
        .exception shouldBe a[TestException]
    }

    "save only match applications with clients with active billing campaign" in {
      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(Some(campaignHeaders(isActive = false)))

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value

      val expectedResponse = MatchApplicationCreateResponse
        .newBuilder()
        .setErrorCode(ErrorCodes.NO_ACTIVE_BILLING_CAMPAIGN)
        .addAllClientsIds(util.Arrays.asList(clientId2))
        .build()

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe Some(expectedResponse)

      client1Notification.await.timeout(Duration(1, SECONDS)).success
    }

    "don`t send email notifications on duplicate match applications" in {
      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId2, matchApplicationId2))
        .returningZ(false)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(Some(campaignHeaders(isActive = false)))

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value

      val expectedResponse = MatchApplicationCreateResponse
        .newBuilder()
        .setErrorCode(ErrorCodes.NO_ACTIVE_BILLING_CAMPAIGN)
        .addAllClientsIds(util.Arrays.asList(clientId2))
        .build()

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe Some(expectedResponse)

      client1Notification.await.timeout(Duration(1, SECONDS)).success
    }

    "save only match applications with clients with active billing campaign with same category" in {

      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(Some(campaignHeaders(isActive = true, Category.MOTO)))

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value

      val expectedResponse = MatchApplicationCreateResponse
        .newBuilder()
        .setErrorCode(ErrorCodes.NO_ACTIVE_BILLING_CAMPAIGN)
        .addAllClientsIds(util.Arrays.asList(clientId2))
        .build()

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe Some(expectedResponse)

      client1Notification.await.timeout(Duration(1, SECONDS)).success
    }

    "save only match applications with clients with active billing campaign with same section" in {
      dao.createIfNotExistsMethod
        .expects(matchApplicationCreateRequest(clientId1, matchApplicationId1))
        .returningZ(true)

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId1)
        .returningZ(Some(campaignHeaders(isActive = true)))

      (billingCampaignService.getMatchApplicationsCampaigns _)
        .expects(clientId2)
        .returningZ(
          Some(campaignHeaders(isActive = true, section = Section.USED))
        )

      val client1Notification = matchApplicationsNotificationSender
        .mockForClient(clientId1, notificationStatus = true)
        .success
        .value

      val expectedResponse = MatchApplicationCreateResponse
        .newBuilder()
        .setErrorCode(ErrorCodes.NO_ACTIVE_BILLING_CAMPAIGN)
        .addAllClientsIds(util.Arrays.asList(clientId2))
        .build()

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        service.create(forms).provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value shouldBe Some(expectedResponse)

      client1Notification.await.timeout(Duration(1, SECONDS)).success
    }

    "find requests with page and client id" in {
      val clientId = 20101
      val page = Page(number = 1, size = 2)

      val now = DateTimeUtil.now()
      val interval = DateTimeInterval(now.minusDays(10), now.plusDays(30))

      val records = List(
        MatchApplicationCreateRequestGen
          .withClientId(clientId)
          .next
      )

      val expectedFilters =
        NonEmptyList.of(WithClients(20101), ForDateTimeInterval(interval))

      dao.findMethod
        .expects(expectedFilters, LimitOffset(page).some)
        .returningZ(records)

      dao.totalCostMethod
        .expects(expectedFilters)
        .returningZ(10000)

      dao.countMethod
        .expects(expectedFilters)
        .returningZ(9)

      val expected =
        MatchApplicationsModel(
          records,
          totalCost = 10000,
          PagingModel(
            pageCount = 5,
            total = 9,
            page = PageModel(pageNum = 1, pageSize = 2)
          )
        )

      service
        .find(clientId, interval, page)
        .success
        .value shouldBe expected
    }

    "mark read with client id and match application id exists" in {
      val clientId = 20101
      val matchApplicationId = MatchApplicationId(Gen.uuid.next)

      val expectedFilters =
        NonEmptyList.of(
          WithClients(20101),
          WithMatchApplicationId(matchApplicationId)
        )
      val expectedLimitOffset = Some(LimitOffset(Some(1), Some(0)))
      val records = List(
        MatchApplicationCreateRequestGen
          .withClientId(clientId)
          .withMatchApplicationId(matchApplicationId)
          .withIsRead(false)
          .next
      )

      dao.findMethod
        .expects(expectedFilters, expectedLimitOffset)
        .returningZ(records)

      dao.updateIfPreconditionMethod
        .expects(expectedFilters, NonEmptyList.of(Update.ReadStatus(true)), *)
        .returningZ(Unit)

      service
        .updateIsReadFlag(clientId, matchApplicationId, isRead = true)
        .success
    }

    "fail if mark read non-existing match application" in {
      val clientId = 20101
      val matchApplicationId = MatchApplicationId(Gen.uuid.next)

      val expectedFilters =
        NonEmptyList.of(
          WithClients(20101),
          WithMatchApplicationId(matchApplicationId)
        )

      dao.updateIfPreconditionMethod
        .expects(expectedFilters, NonEmptyList.of(Update.ReadStatus(true)), *)
        .throwingZ(
          SalesmanMatchApplicationNotFoundException(
            matchApplicationId,
            clientId
          )
        )

      service
        .updateIsReadFlag(clientId, matchApplicationId, isRead = true)
        .failure
        .exception shouldBe SalesmanMatchApplicationNotFoundException(
        matchApplicationId,
        clientId
      )
    }
  }

  override protected def before(fun: => Any)(implicit pos: Position): Unit =
    matchApplicationsNotificationSender.clear()
}

object MatchApplicationServiceSpec {

  val now = DateTimeUtil.now()

  val matchApplicationId1 = MatchApplicationIdGen.next
  val matchApplicationId2 = MatchApplicationIdGen.next

  val clientId1 = 1L
  val clientId2 = 2L

  val forms: MatchApplicationCreateForms =
    MatchApplicationCreateForms
      .newBuilder()
      .addAllForms(
        List(
          MatchApplicationCreateForm
            .newBuilder()
            .setClientOfferId("12345-123")
            .setClientId(1)
            .setUserId(2)
            .setClientOfferCategory(Category.CARS)
            .setClientOfferSection(Section.NEW)
            .setMatchApplicationId(matchApplicationId1.toString)
            .setCreateDate(now.asTimestamp)
            .build(),
          MatchApplicationCreateForm
            .newBuilder()
            .setClientOfferId("12345-123")
            .setClientId(2)
            .setUserId(2)
            .setClientOfferCategory(Category.CARS)
            .setClientOfferSection(Section.NEW)
            .setMatchApplicationId(matchApplicationId2.toString)
            .setCreateDate(now.asTimestamp)
            .build()
        ).asJava
      )
      .build

  def matchApplicationCreateRequest(
      clientId: ClientId,
      matchApplicationId: MatchApplicationId
  ) =
    MatchApplicationCreateRequest(
      clientId = clientId,
      userId = AutoruUser(2),
      matchApplicationId,
      AutoruOfferId("12345-123"),
      Category.CARS,
      Section.NEW,
      MatchApplicationCreateRequest.Statuses.New,
      createDate = now,
      isRead = false
    )

  def campaignHeaders(
      isActive: Boolean,
      category: Category = Category.CARS,
      section: Section = Section.NEW
  ): CampaignHeaderList = {
    val builder = CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setId("1")
      .setOrder(BillingTestData.Order)
      .setSettings(BillingTestData.Settings)
      .setOwner(BillingTestData.Owner)
      .setProduct(
        Product
          .newBuilder()
          .setVersion(1)
          .addGoods(
            Good
              .newBuilder()
              .setVersion(1)
              .setCustom(
                Custom
                  .newBuilder()
                  .setId(
                    s"match-application:${category.toString.toLowerCase}:${section.toString.toLowerCase}"
                  )
                  .setCost(
                    Cost
                      .newBuilder()
                      .setVersion(1)
                  )
              )
          )
      )
    if (!isActive) builder.setInactiveReason(InactiveReason.NO_ENOUGH_FUNDS)

    CampaignHeaderList
      .newBuilder()
      .addCampaignHeaders(builder)
      .build()
  }
}
