package ru.auto.salesman.api.v1.service.cashback

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.{Conflict, NotFound, OK}
import cats.data.NonEmptyList
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.cabinet.ApiModel.{
  ClientProperties,
  ClientStatus,
  DetailedClient,
  DetailedClientRequest,
  DetailedClientResponse,
  ExtraBonus
}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.LoyaltyReportDao.{AppliedCashback, NotApprovedReportInfo}
import ru.auto.salesman.dao.{
  CommentAlreadyExisted,
  LoyaltyReportDao,
  LoyaltyReportNotFound,
  Updated
}
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.cashback.ApiModel.{
  CashbackStatusUpdateRequest,
  CashbackStatusUpdateResponse,
  ClientCashbackInfo,
  InvalidContent,
  LoyaltyReport,
  LoyaltyReportInfo,
  LoyaltyReportStatus,
  LoyaltyReportsResponse,
  LoyaltyTableEntity,
  RejectedCashbacks
}
import ru.auto.salesman.model.cashback.LoyaltyLevel.{
  HalfYearLoyaltyLevel,
  YearLoyaltyLevel
}
import ru.auto.salesman.model.cashback.{
  CashbackPeriod,
  LoyaltyReportItem,
  LoyaltyReportItemData
}
import ru.auto.salesman.model.{ClientId, PeriodId, StaffId}
import ru.auto.salesman.service.CashbackPeriodService
import ru.auto.salesman.service.CashbackPeriodService.PeriodNotFoundException
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.service.impl.LoyaltyReportServiceImpl
import ru.auto.salesman.test.dao.gens.cashbackPeriodGen
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString}

import scala.collection.JavaConverters._

class CashbackHandlerSpec extends RoutingSpec {
  private val cashbackPeriodService = mock[CashbackPeriodService]

  private val loyaltyReportDao = mock[LoyaltyReportDao]
  private val cabinetClient = mock[CabinetClient]
  private val promocoderClient = mock[PromocoderClient]

  private val loyaltyReportService =
    new LoyaltyReportServiceImpl(
      loyaltyReportDao,
      cabinetClient,
      cashbackPeriodService,
      promocoderClient
    )

  private val route =
    new CashbackHandler(cashbackPeriodService, loyaltyReportService).route

  def genPeriodJsPair(periodId: Long, periodIsActive: Boolean = false) = {
    val period =
      cashbackPeriodGen(
        periodIdGen = PeriodId(periodId),
        //note: non-active periods are indicated, as omitting the field in result
        isActiveGen = periodIsActive
      ).next
    import period._

    val fields = Seq(
      Some("id" -> JsString(id.toString)),
      Some("start" -> JsString(Timestamps.toString(start.asTimestamp))),
      Some("finish" -> JsString(Timestamps.toString(finish.asTimestamp))),
      Some("isActive" -> JsBoolean(isActive)),
      previousPeriod.map { p =>
        "previousPeriod" -> JsString(p.toString)
      }
    ).flatten

    period -> JsObject(fields: _*)
  }

  "GET /cashback/period/{periodId}" should {
    "get cashback period by id" in {
      val (period, expectedJson) = genPeriodJsPair(1L)

      (cashbackPeriodService.getById _)
        .expects(period.id)
        .returningZ(Some(period))

      Get(s"/period/${period.id}")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsObject] shouldBe expectedJson

      }
    }

    "get active period by id" in {
      val (period, expectedJson) =
        genPeriodJsPair(1L, periodIsActive = true)

      (cashbackPeriodService.getById _)
        .expects(period.id)
        .returningZ(Some(period))

      Get(s"/period/${period.id}")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val resp = responseAs[JsObject]
        resp shouldBe expectedJson
      }
    }
  }

  "GET /cashback/periods" should {
    "return all existing periods" in {
      val (periods, responses) =
        (1L to 3L).map(i => genPeriodJsPair(i)).toList.unzip

      (cashbackPeriodService.getPeriods _)
        .expects()
        .returningZ(periods)

      Get("/periods").withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
        val response = responseAs[JsObject]
        val expectedResponse = JsObject(
          "periods" -> JsArray(responses.toVector)
        )
        response shouldBe expectedResponse
      }
    }
  }

  "GET /cashback/amount" should {
    "get cashback amount for period and client" in {
      (loyaltyReportDao.findApplied _)
        .expects(PeriodId(1L), 1L)
        .returningZ(
          Some(
            AppliedCashback(cashbackAmount = 1000, cashbackPercent = 10)
          )
        )

      Get("/amount?clientId=1&periodId=1")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsObject] shouldBe
          JsObject(
            "amount" -> JsNumber(1000),
            "percent" -> JsNumber(10)
          )
      }
    }

    "return zero on non existent client and period application" in {
      (loyaltyReportDao.findApplied _)
        .expects(PeriodId(1L), 1L)
        .returningZ(None)

      Get("/amount?clientId=1&periodId=1")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsObject] shouldBe
          JsObject(
            "amount" -> JsNumber(0),
            "percent" -> JsNumber(0)
          )
      }
    }
  }

  "PUT /cashback/period/{periodId}/close" should {
    "close cashback period by id" in {
      val periodId = PeriodId(1)

      (cashbackPeriodService.closeById _)
        .expects(periodId)
        .returningZ(())

      Put(s"/period/$periodId/close")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsObject]
      }
    }

    "return 404 for nonexistent cashback period" in {
      val periodId = PeriodId(2)

      (cashbackPeriodService.closeById _)
        .expects(periodId)
        .throwing(PeriodNotFoundException(periodId))

      Put(s"/period/$periodId/close")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe NotFound
        responseAs[JsObject]
      }
    }

    "PUT /cashback/period/{periodId}/approve" should {
      "set approve by id" in {
        val periodId = PeriodId(1)
        val update = ClientCashbackInfo
          .newBuilder()
          .setClientId(20101)
          .setCashbackAmount(100)
          .build()
        val invalidUpdate = ClientCashbackInfo
          .newBuilder()
          .setClientId(42)
          .setCashbackAmount(200)
          .build()
        val req = CashbackStatusUpdateRequest
          .newBuilder()
          .setUserId("test_user")
          .addClientsCashbacks(update)
          .addClientsCashbacks(invalidUpdate)
          .build()
        val response = CashbackStatusUpdateResponse
          .newBuilder()
          .addRejected(
            RejectedCashbacks
              .newBuilder()
              .setClientId(1)
              .setReason(
                InvalidContent
                  .newBuilder()
                  .setActualStatus(LoyaltyReportStatus.APPLIED)
                  .setActualCashbackAmount(42)
              )
          )
          .build()
        (loyaltyReportDao.setApprove _)
          .expects(
            StaffId("test_user"),
            periodId,
            NonEmptyList.of(update, invalidUpdate)
          )
          .returningZ(
            List(NotApprovedReportInfo(1, LoyaltyReportStatus.APPLIED, 42))
          )

        Put(s"/period/$periodId/approve", req)
          .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
          status shouldBe OK
          responseAs[CashbackStatusUpdateResponse] shouldBe response
        }
      }
    }
  }

  "PUT /cashback/period/{periodId}/pre_approve" should {
    "set pre_approve by id" in {
      val periodId = PeriodId(1)
      val update = ClientCashbackInfo
        .newBuilder()
        .setClientId(20101)
        .setCashbackAmount(100)
        .build()
      val invalidUpdate = ClientCashbackInfo
        .newBuilder()
        .setClientId(42)
        .setCashbackAmount(200)
        .build()
      val req = CashbackStatusUpdateRequest
        .newBuilder()
        .setUserId("test_user")
        .addClientsCashbacks(update)
        .addClientsCashbacks(invalidUpdate)
        .build()
      val response = CashbackStatusUpdateResponse
        .newBuilder()
        .addRejected(
          RejectedCashbacks
            .newBuilder()
            .setClientId(1)
            .setReason(
              InvalidContent
                .newBuilder()
                .setActualStatus(LoyaltyReportStatus.APPLIED)
                .setActualCashbackAmount(42)
            )
        )
        .build()
      (loyaltyReportDao.setPreApprove _)
        .expects(
          StaffId("test_user"),
          periodId,
          NonEmptyList.of(update, invalidUpdate)
        )
        .returningZ(
          List(NotApprovedReportInfo(1, LoyaltyReportStatus.APPLIED, 42))
        )

      Put(s"/period/$periodId/pre_approve", req)
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[CashbackStatusUpdateResponse] shouldBe response
      }
    }
  }

  "DELETE /cashback/period/{periodId}/pre_approve" should {
    "remove pre_approve by id" in {
      val periodId = PeriodId(1)
      val update = ClientCashbackInfo
        .newBuilder()
        .setClientId(20101)
        .setCashbackAmount(100)
        .build()
      val invalidUpdate = ClientCashbackInfo
        .newBuilder()
        .setClientId(42)
        .setCashbackAmount(200)
        .build()
      val req = CashbackStatusUpdateRequest
        .newBuilder()
        .setUserId("test_user")
        .addClientsCashbacks(update)
        .addClientsCashbacks(invalidUpdate)
        .build()
      val response = CashbackStatusUpdateResponse
        .newBuilder()
        .addRejected(
          RejectedCashbacks
            .newBuilder()
            .setClientId(1)
            .setReason(
              InvalidContent
                .newBuilder()
                .setActualStatus(LoyaltyReportStatus.APPLIED)
                .setActualCashbackAmount(42)
            )
        )
        .build()
      (loyaltyReportDao.removePreApprove _)
        .expects(periodId, NonEmptyList.of(update, invalidUpdate))
        .returningZ(
          List(NotApprovedReportInfo(1, LoyaltyReportStatus.APPLIED, 42))
        )

      Delete(s"/period/$periodId/pre_approve", req)
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[CashbackStatusUpdateResponse] shouldBe response
      }
    }
  }

  "GET cashback/period/{periodId}/reports" should {
    "create table for cashback period" in {
      val periodId = PeriodId(1)

      val dateTime = new DateTime("2020-01-12T12:00:00.000+0000")

      val report1ForZeroPeriod = LoyaltyReport.newBuilder
        .setPeriodId(0)
        .setClientId(1)
        .setLoyaltyLevel(YearLoyaltyLevel.raw)
        .setCashbackAmount(2)
        .setCashbackPercent(1)
        .setHasFullStock(false)
        .build()

      val report1ForFirstPeriod = LoyaltyReport.newBuilder
        .setPeriodId(1)
        .setClientId(1)
        .setLoyaltyLevel(YearLoyaltyLevel.raw)
        .setCashbackAmount(1)
        .setCashbackPercent(1)
        .setHasFullStock(true)
        .build()

      val report2ForFirstPeriod = LoyaltyReport.newBuilder
        .setPeriodId(1)
        .setClientId(2)
        .setLoyaltyLevel(HalfYearLoyaltyLevel.raw)
        .setCashbackAmount(3)
        .setCashbackPercent(6)
        .setHasFullStock(false)
        .setExtraBonus(ExtraBonus.OVER_2000_CARS)
        .build()

      val properties = ClientProperties
        .newBuilder()
        .setRegionId(213)
        .setOriginId("2")
        .setStatus(ClientStatus.ACTIVE)
        .setEpoch(Timestamps.fromMillis(dateTime.getMillis))
        .setAddress("Москва")
        .setEmail("test@yandex.ru")
        .setPriorityPlacement(true)
        .setAutoProlong(false)
        .setCompanyId(12)
        .build()

      def detailedClient(id: ClientId) =
        DetailedClient
          .newBuilder()
          .setId(id)
          .setName("Dealer")
          .setProperties(properties)
          .setAgencyId(2)
          .setAgencyName("Test agency")
          .setCompanyId(3)
          .setCompanyName("Test company")
          .build()

      (cashbackPeriodService.getById _)
        .expects(PeriodId(1))
        .returningZ(
          Some(
            CashbackPeriod(
              id = PeriodId(1),
              start = DateTime.now().minusDays(1),
              finish = DateTime.now(),
              isActive = true,
              previousPeriod = Some(PeriodId(0))
            )
          )
        )

      (loyaltyReportDao.findByPeriodID _)
        .expects(PeriodId(1))
        .returningZ(List(report1ForFirstPeriod, report2ForFirstPeriod))
      (loyaltyReportDao.findByPeriodID _)
        .expects(PeriodId(0))
        .returningZ(List(report1ForZeroPeriod))

      (cabinetClient.getDetailedClients _)
        .expects(
          DetailedClientRequest
            .newBuilder()
            .addClientIds(1L)
            .addClientIds(2L)
            .build()
        )
        .returningZ(
          DetailedClientResponse
            .newBuilder()
            .addAllClients(List(detailedClient(1), detailedClient(2)).asJava)
            .build()
        )

      Get(s"/period/$periodId/reports")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val reports =
          responseAs[LoyaltyReportsResponse].getReportsList.asScala.toList
        reports.length shouldBe 2
        reports should contain allOf (LoyaltyTableEntity
          .newBuilder()
          .setReport(report1ForFirstPeriod)
          .setClient(detailedClient(1))
          .setPrevPeriodReport(report1ForZeroPeriod)
          .build(),
        LoyaltyTableEntity
          .newBuilder()
          .setReport(report2ForFirstPeriod)
          .setClient(detailedClient(2))
          .build())
      }
    }

    "create table for cashback period without previous period" in {
      val periodId = PeriodId(1)

      val dateTime = new DateTime("2020-01-12T12:00:00.000+0000")

      val report1ForFirstPeriod = LoyaltyReport.newBuilder
        .setPeriodId(1)
        .setClientId(1)
        .setLoyaltyLevel(YearLoyaltyLevel.raw)
        .setCashbackAmount(1)
        .setCashbackPercent(1)
        .setHasFullStock(true)
        .build()

      val report2ForFirstPeriod = LoyaltyReport.newBuilder
        .setPeriodId(1)
        .setClientId(2)
        .setLoyaltyLevel(HalfYearLoyaltyLevel.raw)
        .setCashbackAmount(3)
        .setCashbackPercent(6)
        .setHasFullStock(false)
        .setExtraBonus(ExtraBonus.OVER_2000_CARS)
        .build()

      val properties = ClientProperties
        .newBuilder()
        .setRegionId(213)
        .setOriginId("2")
        .setStatus(ClientStatus.ACTIVE)
        .setEpoch(Timestamps.fromMillis(dateTime.getMillis))
        .setAddress("Москва")
        .setEmail("test@yandex.ru")
        .setPriorityPlacement(true)
        .setAutoProlong(false)
        .setCompanyId(12)
        .build()

      def detailedClient(id: ClientId) =
        DetailedClient
          .newBuilder()
          .setId(id)
          .setName("Dealer")
          .setProperties(properties)
          .setAgencyId(2)
          .setAgencyName("Test agency")
          .setCompanyId(3)
          .setCompanyName("Test company")
          .build()

      (cashbackPeriodService.getById _)
        .expects(PeriodId(1))
        .returningZ(
          Some(
            CashbackPeriod(
              id = PeriodId(1),
              start = DateTime.now().minusDays(1),
              finish = DateTime.now(),
              isActive = true,
              previousPeriod = None
            )
          )
        )

      (loyaltyReportDao.findByPeriodID _)
        .expects(PeriodId(1))
        .returningZ(List(report1ForFirstPeriod, report2ForFirstPeriod))

      (cabinetClient.getDetailedClients _)
        .expects(
          DetailedClientRequest
            .newBuilder()
            .addClientIds(1L)
            .addClientIds(2L)
            .build()
        )
        .returningZ(
          DetailedClientResponse
            .newBuilder()
            .addAllClients(List(detailedClient(1), detailedClient(2)).asJava)
            .build()
        )

      Get(s"/period/$periodId/reports")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val reports =
          responseAs[LoyaltyReportsResponse].getReportsList.asScala.toList
        reports.length shouldBe 2
        reports should contain allOf (LoyaltyTableEntity
          .newBuilder()
          .setReport(report1ForFirstPeriod)
          .setClient(detailedClient(1))
          .build(),
        LoyaltyTableEntity
          .newBuilder()
          .setReport(report2ForFirstPeriod)
          .setClient(detailedClient(2))
          .build())
      }
    }

    "return 404 for nonexistent cashback period" in {
      val periodId = PeriodId(1)
      (cashbackPeriodService.getById _)
        .expects(periodId)
        .returningZ(None)

      Get(s"/period/$periodId/reports")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe NotFound
      }
    }
  }

  "PUT /cashback/period/{periodId}/comment" should {

    val validPeriodId = PeriodId(1)
    val invalidPeriodId = PeriodId(2)
    val clientId = 1L
    val data = JsObject(
      "text" -> JsString("test"),
      "author" -> JsString("testUser"),
      "client_id" -> JsNumber(clientId)
    ).toString()

    "add new comment" in {
      (loyaltyReportDao.addComment _)
        .expects(validPeriodId, 1L, "test", StaffId("testUser"))
        .returningZ(Updated)

      Put(s"/period/$validPeriodId/comment")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
    }

    "return error if periodId is not present" in {
      (loyaltyReportDao.addComment _)
        .expects(invalidPeriodId, 1L, "test", StaffId("testUser"))
        .returningZ(LoyaltyReportNotFound)

      Put(s"/period/$invalidPeriodId/comment")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe NotFound
      }
    }

    "return conflict if comment for periodId already exists" in {
      (loyaltyReportDao.addComment _)
        .expects(validPeriodId, 1L, "test", StaffId("testUser"))
        .returningZ(CommentAlreadyExisted)

      Put(s"/period/$validPeriodId/comment")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe Conflict
      }
    }
  }

  "POST /cashback/report/{reportId}/placement_discount" should {
    val validReportId = 1L
    val invalidReportId = 2L

    val data = JsObject(
      "discountPercent" -> JsNumber(20),
      "editor" -> JsString("testUser"),
      "comment" -> JsString("комментарий")
    ).toString()

    "works" in {
      (loyaltyReportDao.editPlacementDiscount _)
        .expects(validReportId, 20, StaffId("testUser"), "комментарий")
        .returningZ(true)

      Post(s"/report/$validReportId/placement_discount")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
    }

    "returns 404 if report id doesn't exist" in {
      (loyaltyReportDao.editPlacementDiscount _)
        .expects(invalidReportId, 20, StaffId("testUser"), "комментарий")
        .returningZ(false)

      Post(s"/report/$invalidReportId/placement_discount")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe NotFound
      }
    }
  }

  "DELETE /cashback/period/{periodId}/comment" should {
    val validPeriodId = PeriodId(1)
    val invalidPeriodId = PeriodId(2)
    val data = JsObject(
      "author" -> JsString("testUser"),
      "client_id" -> JsNumber(1)
    ).toString()

    "delete existing comment" in {
      (loyaltyReportDao.deleteComment _)
        .expects(validPeriodId, 1L, StaffId("testUser"))
        .returningZ(true)

      Delete(s"/period/$validPeriodId/comment")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
    }

    "return error if periodId is not present" in {
      (loyaltyReportDao.deleteComment _)
        .expects(invalidPeriodId, 1L, StaffId("testUser"))
        .returningZ(false)

      Delete(s"/period/$invalidPeriodId/comment")
        .withEntity(ContentTypes.`application/json`, data) ~> seal(
        route
      ) ~> check {
        status shouldBe NotFound
      }
    }
  }

  "GET /cashback/client/{clientId}/report/current" should {
    "return current report with period and items" in {
      val clientId = 20101L
      val periodId = PeriodId(1)

      val period = CashbackPeriod(
        id = periodId,
        start = DateTime.now().minusDays(1),
        finish = DateTime.now(),
        isActive = true,
        previousPeriod = None
      )

      val report = LoyaltyReport
        .newBuilder()
        .setPeriodId(periodId)
        .setClientId(clientId)
        .setLoyaltyLevel(YearLoyaltyLevel.raw)
        .setCashbackAmount(1)
        .setCashbackPercent(1)
        .setHasFullStock(true)
        .setPlacementDiscountPercent(7)
        .build()

      val item = LoyaltyReportItem(
        reportId = 1,
        data = LoyaltyReportItemData(
          criterion = "site-check",
          value = 1,
          resolution = true,
          comment = Some("ok"),
          epoch = DateTime.now()
        )
      )

      (loyaltyReportDao.findCurrentWithItems _)
        .expects(clientId)
        .returningZ(Some(report, List(item)))

      (cashbackPeriodService.getById _)
        .expects(periodId)
        .returningZ(Some(period))

      Get(s"/client/$clientId/report/current")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val response = responseAs[LoyaltyReportInfo]
        response shouldBe (LoyaltyReportInfo
          .newBuilder()
          .setPeriod(period.asProto)
          .setReport(report)
          .addItems(item.asProto)
          .build())
      }
    }

    "return 404 on client without report" in {
      val clientId = 20101L
      (loyaltyReportDao.findCurrentWithItems _)
        .expects(clientId)
        .returningZ(None)

      Get(s"/client/$clientId/report/current")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 on report without period" in {
      val clientId = 20101L
      val periodId = PeriodId(1)

      val report = LoyaltyReport
        .newBuilder()
        .setPeriodId(periodId)
        .setClientId(clientId)
        .setLoyaltyLevel(YearLoyaltyLevel.raw)
        .setCashbackAmount(1)
        .setCashbackPercent(1)
        .setHasFullStock(true)
        .build()

      val item = LoyaltyReportItem(
        reportId = 1,
        data = LoyaltyReportItemData(
          criterion = "site-check",
          value = 1,
          resolution = true,
          comment = Some("ok"),
          epoch = DateTime.now()
        )
      )

      (loyaltyReportDao.findCurrentWithItems _)
        .expects(clientId)
        .returningZ(Some(report, List(item)))

      (cashbackPeriodService.getById _).expects(periodId).returningZ(None)

      Get(s"/client/$clientId/report/current")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe NotFound
      }
    }
  }
}
