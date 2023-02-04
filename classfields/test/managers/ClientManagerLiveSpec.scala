package auto.dealers.amoyak.logic.managers

import auto.common.clients.cabinet.testkit.{CabinetEmptyTest, CabinetTest}
import auto.common.clients.salesman.testkit.{SalesmanClientEmpty, SalesmanClientMock}
import auto.common.manager.region.testkit.{RegionManagerEmpty, RegionManagerMock}
import auto.common.manager.statistics.testkit.StatisticsManagerMock
import auto.dealers.amoyak.model.blocks.{Finance, Loyalty, ModerationBlock}
import cats.syntax.option._
import common.geobase.Region
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import auto.dealers.amoyak.model._
import auto.dealers.amoyak.model.model.defaultAmoDateTimeFormatter
import ru.auto.amoyak.internal_service_model.Office7Data.Clients
import ru.auto.amoyak.internal_service_model.{AmoyakDto, Office7Data}
import ru.auto.api.response_model.DealerAccountResponse
import ru.auto.cabinet.api_model.{
  AmoUpdateClientRequest,
  Company => ProtoCompany,
  DealerStocks,
  InvoiceRequestsResponse,
  Moderation
}
import ru.auto.salesman.model.cashback.api_model.{LoyaltyReport, LoyaltyReportInfo}
import ru.yandex.vertis.feature.model.Feature
import zio.ZLayer
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._
import zio.test.mock.MockClock

import java.time.{OffsetDateTime, ZoneOffset}

object ClientManagerLiveSpec extends DefaultRunnableSpec {

  val clientId = 1L
  val agencyId = 2L

  val timePoint =
    OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

  val clockMock = MockClock.CurrentDateTime {
    value(timePoint)
  }.optional

  val regionManagerEmpty = RegionManagerMock.GetRegions(anything, value(Seq.empty[Region])).optional
  val passportManagerEmpty = PassportManagerMock.GetLastSession(anything, value(LastSession().some)).optional
  val statisticsManagerEmpty = StatisticsManagerMock.GetActivationsDaily(anything, value(Seq())).atMost(0)
  val tariffManagerEmpty = TariffManagerMock.GetTariffs(anything, value(TariffsFull())).optional
  val falseFeature = ZLayer.succeed(Feature("test_feature", _ => false))
  val trueFeature = ZLayer.succeed(Feature("test_feature", _ => true))

  val salesmanTest =
    SalesmanClientMock.GetLoyaltyReport(anything, value(Some(LoyaltyReportInfo.defaultInstance))).optional

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ClientManagerLive")(
      testM("amoUpdateClient should fail if updates are disabled") {
        val updateClientRequest = UpdateClientRequest(
          clientId = AmoClientId(clientId, isAgency = false),
          agency = Some(AmoClientId(agencyId, isAgency = true)),
          headCompany = None,
          responsibleManagerEmail = None,
          moderationComment = None,
          modifiedManagerEmail = None
        )

        val clientManager =
          CabinetEmptyTest.empty ++
            SalesmanClientEmpty.empty ++
            RegionManagerEmpty.empty ++
            clockMock ++
            TariffManagerMock.empty ++
            StatisticsManagerMock.empty ++
            PassportManagerMock.empty ++
            trueFeature >>> ClientManager.live

        assertM(ClientManager.updateClient(updateClientRequest).run)(
          fails(isSubtype[AutoRuAmoCrmException.UpdateException](anything))
        ).provideCustomLayer(clientManager)
      },
      testM("amoUpdateClient should send update request to cabinet") {
        val updateClientRequest = UpdateClientRequest(
          clientId = AmoClientId(clientId, isAgency = false),
          agency = Some(AmoClientId(agencyId, isAgency = true)),
          headCompany = None,
          responsibleManagerEmail = None,
          moderationComment = None,
          modifiedManagerEmail = None
        )
        val amoUpdateClientRequest = AmoUpdateClientRequest(clientId, Some(agencyId))

        val cabinetTest = CabinetTest.AmoUpdateClient(equalTo(amoUpdateClientRequest), value(())).optional

        val clientManager =
          (cabinetTest ++
            salesmanTest ++
            regionManagerEmpty ++
            clockMock ++
            tariffManagerEmpty ++
            statisticsManagerEmpty ++
            passportManagerEmpty).toLayer ++
            falseFeature >>> ClientManager.live

        assertM(ClientManager.updateClient(updateClientRequest))(isUnit)
          .provideCustomLayer(clientManager)
      },
      testM("getClient should compose all endpoints into one message for client") {
        val clientIdStr = "client-123"
        val clientId = AmoClientId.from(clientIdStr).fold(throw _, identity)
        val defualtStatisticsPeriodInDays = 30
        val email = "email@email.em"

        val cabinetGetClientMock = CabinetTest.AmoGetClient(
          equalTo(clientId.autoruId),
          value(
            AmoyakDto.defaultInstance.withOffice7Data(
              Office7Data.defaultInstance.withClients(
                Clients.defaultInstance
                  .withClientId(clientId.autoruId)
                  .withEmail(email)
              )
            )
          )
        )

        val cabinetGetStockMock = CabinetTest.GetStock(
          equalTo(clientId.autoruId),
          value(DealerStocks.defaultInstance)
        )

        val firstModeration = Some(true)
        val moderationComment = Some("Comment")
        val onModeration = Some(false)
        val sitecheck = None

        val cabinetGetModerationMock = CabinetTest.GetModeration(
          equalTo(clientId.autoruId),
          value(
            Moderation(
              firstModeration = firstModeration,
              banReasons = None,
              onModeration = onModeration,
              sitecheck = sitecheck,
              sitecheckDate = None,
              moderationComment = moderationComment
            )
          )
        )

        val cabinetGetInvoiceRequestsMock =
          CabinetTest.GetInvoiceRequests(equalTo(clientId.autoruId), value(InvoiceRequestsResponse.defaultInstance))

        val cabinetGetClientAccount =
          CabinetTest.GetClientAccount(equalTo(clientId.autoruId), value(DealerAccountResponse.defaultInstance))

        val defaultLoyaltyReport =
          Some(
            LoyaltyReportInfo.defaultInstance
              .copy(report = Some(LoyaltyReport.defaultInstance))
          )

        val salesmanClientMock =
          SalesmanClientMock.GetLoyaltyReport(
            equalTo(clientId.autoruId),
            value(defaultLoyaltyReport)
          )

        val statisticsManagerMock = StatisticsManagerMock.GetActivationsDaily(
          equalTo((clientId.autoruId, MoscowClock.asMoscowTime(timePoint), defualtStatisticsPeriodInDays)),
          value(Seq())
        )

        val defaultLoyalty =
          Some(
            Loyalty(
              isLoyal = Some(false),
              loyaltyLevel = Some(0),
              loyaltyCashbackAmount = Some(0),
              loyaltyCashbackPercent = Some(0)
            )
          )

        val expected = AmoCrmFullClientMessage(
          clientId = clientId,
          origin = None,
          clientType = ClientType.Client.lowercaseName.some,
          name = None,
          agency = None,
          headCompany = None,
          responsibleManagerEmail = None,
          status = "new".some,
          createdTime = None,
          city = None,
          region = None,
          address = None,
          phoneNumber = None,
          website = None,
          email = Some(email),
          dealership = None,
          tariffs = Some(TariffsFull(None, None)),
          loyalty = defaultLoyalty,
          moderation = Some(
            ModerationBlock(
              firstModeration = firstModeration,
              banReasons = None,
              onModeration = onModeration,
              sitecheck = None,
              sitecheckDate = None,
              moderationComment = moderationComment
            )
          ),
          finance = Some(
            Finance(
              0,
              0,
              "2020-01-01",
              0,
              "https://admin.balance.yandex-team.ru/passports.xml?tcl_id=0",
              "https://admin.balance.yandex.ru/passports.xml?tcl_id=0",
              List(),
              List()
            )
          ),
          stock = Some(Map()),
          lastSession = LastSession().some,
          updateTimestamp = Some(MoscowClock.asMoscowTime(timePoint))
        )

        val clientManager =
          (clockMock ++
            cabinetGetClientMock ++
            cabinetGetModerationMock ++
            salesmanClientMock ++
            tariffManagerEmpty ++
            cabinetGetStockMock ++
            cabinetGetInvoiceRequestsMock ++
            cabinetGetClientAccount ++
            statisticsManagerMock ++
            passportManagerEmpty ++
            regionManagerEmpty).toLayer ++
            falseFeature >>> ClientManager.live

        assertM(ClientManager.getClient(clientId))(equalTo(expected)).provideCustomLayer(clientManager)
      },
      testM("getClient should works correctly for company") {
        val clientIdStr = "company-123"
        val clientId = AmoClientId.from(clientIdStr).fold(throw _, identity)

        val cabinetGetCompanyMock = CabinetTest.AmoGetCompany(
          equalTo(clientId.autoruId),
          value(
            ProtoCompany.defaultInstance
              .withCreateDate(ScalaProtobuf.toTimestamp(timePoint.minusDays(1)))
              .withTitle("dummy title")
          )
        )

        val expected = AmoCrmFullClientMessage(
          clientId = clientId,
          origin = None,
          clientType = None,
          name = "dummy title".some,
          agency = None,
          headCompany = None,
          responsibleManagerEmail = None,
          status = None,
          createdTime = timePoint.minusDays(1).some.map(_.format(defaultAmoDateTimeFormatter)),
          city = None,
          region = None,
          address = None,
          phoneNumber = None,
          website = None,
          email = None,
          dealership = None,
          tariffs = None,
          loyalty = None,
          moderation = None,
          finance = None,
          stock = None,
          lastSession = None,
          updateTimestamp = Some(MoscowClock.asMoscowTime(timePoint))
        )

        val clientManager =
          (clockMock ++
            cabinetGetCompanyMock ++
            salesmanTest ++
            regionManagerEmpty ++
            tariffManagerEmpty ++
            statisticsManagerEmpty ++
            passportManagerEmpty).toLayer ++
            falseFeature >>> ClientManager.live

        assertM(ClientManager.getClient(clientId))(equalTo(expected)).provideCustomLayer(clientManager)
      }
      // FIXME
      //      testM("amoUpdateClient should fail for invalid ids") {
      //        val updateClientRequest = UpdateClientRequest(
      //          clientId = AmoClientId(clientId, isAgency = true),
      //          agency = None,
      //          headCompany = None,
      //          responsibleManagerEmail = None,
      //          moderationComment = None
      //        )
      //        val salesmanTest =
      //          SalesmanClientMock.GetLoyaltyReport(anything, value(LoyaltyReportInfo.defaultInstance)).optional
      //
      //        val cabinetTest = CabinetTest.AmoUpdateClient(anything).optional
      //
      //        assertM(ClientManager.updateClient(updateClientRequest).run)(fails(isSubtype[ClientManagerError](anything)))
      //          .provideCustomLayer((cabinetTest ++ salesmanTest ++ regionManagerEmpty ++ clockMock) >>> ClientManager.live)
      //      }
    )
}
