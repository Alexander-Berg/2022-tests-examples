package ru.yandex.auto.vin.decoder.manager

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import auto.carfax.common.clients.hydra.HydraClient
import cats.implicits._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.api.exceptions._
import ru.yandex.auto.vin.decoder.hydra.HydraLimiter
import ru.yandex.auto.vin.decoder.manager.licenseplate.LicensePlateHistoryManager
import ru.yandex.auto.vin.decoder.manager.vin.autocode.VinAutocodeManager
import ru.yandex.auto.vin.decoder.manager.vin.{KnownIdentifiers, VinDataManager}
import ru.yandex.auto.vin.decoder.model._
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.MegaParserRsaUpdateManager
import ru.yandex.auto.vin.decoder.partners.autocode._
import ru.yandex.auto.vin.decoder.partners.autocode.model._
import ru.yandex.auto.vin.decoder.proto.LicensePlateSchema._
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.EmptyRequestInfo
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.commons.http.client.RequestTimeout
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.net.SocketTimeoutException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class RelationshipManagerSpec
  extends TestKit(ActorSystem("RelationshipManagerSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  implicit val metrics: MetricsSupport = TestOperationalSupport
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val r = EmptyRequestInfo
  implicit val vinCode: VinCode = CommonVinCode("SALWA2FK7HA135034")

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val hydraClient: HydraClient = mock[HydraClient]

  val hydraLimiter: HydraLimiter = new HydraLimiter(hydraClient) {
    override val component: String = "test_component"
    override val user: String = "test_user"
  }

  val httpManager: AutocodeHttpManager = mock[AutocodeHttpManager]
  val lpHistoryManager: LicensePlateHistoryManager = mock[LicensePlateHistoryManager]
  val lpUpdateService: LicensePlateUpdateService = mock[LicensePlateUpdateService]
  val vinAutocodeManager: VinAutocodeManager = mock[VinAutocodeManager]
  val vinDataManager: VinDataManager = mock[VinDataManager]
  val rsaUpdateManager: MegaParserRsaUpdateManager = mock[MegaParserRsaUpdateManager]
  val enableRsaUpdates: Feature[Boolean] = mock[Feature[Boolean]]

  when(enableRsaUpdates.value).thenReturn(false)

  val manager = new RelationshipManager(
    httpManager,
    lpHistoryManager,
    lpUpdateService,
    vinAutocodeManager,
    vinDataManager,
    enableRsaUpdates,
    rsaUpdateManager,
    hydraLimiter,
    None
  )

  override protected def beforeEach(): Unit = {
    reset(httpManager)
    reset(vinDataManager)
    reset(lpHistoryManager)
    reset(lpUpdateService)
  }

  val lp: LicensePlate = LicensePlate("K718CE178")
  val vin: VinCode = VinCode("XTA21099043576182")

  private def createState(id: String) = {
    Source(id, SourceState.OK, Some(SourceExtendedState.OK))
  }

  private def createReport(
      reportType: AutocodeReportType[LicensePlate],
      skipTech: Boolean = false,
      responseLp: Option[String] = None,
      responseVin: Option[String] = None) = {
    val model = AutocodeReportResponse(
      "OK",
      Data(
        reportType.id,
        Query(AutocodeQueryType.GRZ, "K718CE178"),
        2,
        0,
        2,
        List(createState(SourceId.Base), createState(SourceId.SubBase)),
        Content(
          AutocodeReportIdentifiers(
            vin = responseVin,
            body = None,
            chassis = None,
            regNum = responseLp,
            sts = None,
            pts = None
          ).some,
          if (!skipTech)
            AutocodeReportTechData("TOYOTA COROLLA", "", 2010, 0, 0, "", "", "", "", "").some
          else None,
          None,
          Nil,
          Nil,
          Nil,
          Nil,
          Nil,
          Nil,
          Nil,
          Nil,
          Nil,
          None,
          None,
          Nil,
          ""
        ).some,
        "",
        ""
      )
    )

    AutocodeReportResponseRaw("", "", model)
  }

  "RelationshipManager" should {
    "request report" when {
      "unknown vin" in {
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(None))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.successful(AutocodeRequest("", CommonVinCode(""), AutocodeReportType.Identifiers)))
        when(httpManager.getResult(?)(?, ?))
          .thenReturn(
            Future.successful(
              createReport(
                reportType = AutocodeReportType.Identifiers,
                responseVin = "XTA21099043576182".some,
                responseLp = "K718CE178".some
              )
            )
          )

        manager.resolveVinAndBuildResponse(lp).await.getVin shouldBe "XTA21099043576182"
      }

      "update is absent" in {
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(None))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(Future.successful {
          AutocodeRequest("", lp, AutocodeReportType.Identifiers)
        })
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)
        when(httpManager.getResult(?)(?, ?))
          .thenReturn(
            Future.successful(
              createReport(
                reportType = AutocodeReportType.Identifiers,
                responseVin = "XTA21099043576182".some,
                responseLp = "K718CE178".some
              )
            )
          )

        manager.resolveVinAndBuildResponse(lp).await.getVin shouldBe "XTA21099043576182"
      }
    }

    "throw UnknownLicensePlateException" when {

      def testUnknownLp(autocodeResponseLp: Option[String], autocodeResponseVin: Option[String]) = {
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(None))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(Future.successful {
          AutocodeRequest("", lp, AutocodeReportType.Identifiers)
        })
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)
        when(httpManager.getResult(?)(?, ?))
          .thenReturn(
            Future.successful(
              createReport(
                reportType = AutocodeReportType.Identifiers,
                responseVin = autocodeResponseVin,
                responseLp = autocodeResponseLp
              )
            )
          )

        intercept[UnknownLicensePlateException] {
          manager.resolveVinAndBuildResponse(lp).await
        }
      }

      "mismatch license plate in autocode response" in {
        testUnknownLp("K516CE99".some, "XTA21099043576182".some)
      }

      "got none license plate in autocode response" in {
        testUnknownLp(None, "XTA21099043576182".some)
      }

      "got none vin in autocode response" in {
        testUnknownLp(lp.toString.some, None)
      }
    }

    "trigger autocode request" when {

      def createRawStorageRelationship(
          vin: String,
          licensePlate: String,
          fresh: Boolean = true) = {
        val rawStorageData = VinInfoHistory.newBuilder()
        rawStorageData.getVehicleIdentifiersBuilder
          .setVin(vin)
          .setLicensePlate(licensePlate)
        val date = if (fresh) System.currentTimeMillis() else 10
        Prepared(date, date, date, rawStorageData.build(), "")
      }

      def createResolutionDataRelationship(licensePlate: LicensePlate, fresh: Boolean) = {
        val date = if (fresh) System.currentTimeMillis() else 10
        KnownIdentifiers.empty.copy(lastLp = IdentifierContainer(licensePlate, None, date).some)
      }

      def testOutdatedRelationship(dataByLp: Prepared, resolutionDataRelationship: KnownIdentifiers) = {
        val resolutionData = ResolutionData.empty(vinCode).copy(identifiers = resolutionDataRelationship)
        val mysqlRelationship = LicensePlateHistory.newBuilder()
        mysqlRelationship.getVinHistoryBuilder.addEntries(
          VinEntry
            .newBuilder()
            .setTrusted(true)
            .setVin(vin.toString)
        )
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?))
          .thenReturn(Success(mysqlRelationship.build().some))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.successful(AutocodeRequest("", lp, AutocodeReportType.Identifiers)))
        when(vinDataManager.getLatestResolutionData(eq(vin), ?)(any())).thenReturn(Future.successful(resolutionData))
        when(vinDataManager.getLastRelationshipByLp(eq(lp))(?))
          .thenReturn(
            Future.successful(
              Some(
                IdentifierContainer(VinCode(dataByLp.data.getVehicleIdentifiers.getVin), None, dataByLp.timestampUpdate)
              )
            )
          )
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)
        when(httpManager.getResult(?)(?, ?))
          .thenReturn(
            Future.successful(
              createReport(
                reportType = AutocodeReportType.Identifiers,
                responseVin = vin.toString.some,
                responseLp = lp.toString.some
              )
            )
          )

        manager.resolveVinAndBuildResponse(lp, 7.days.some).await.getVin shouldBe vin.toString
        verify(httpManager, times(1)).getResult(?)(?, ?)
      }

      "relationship contains mismatching vin in raw storage" in {
        val rawStorageData = createRawStorageRelationship(vin = "XTA21099043576183", licensePlate = lp.toString)
        val resolutionData = createResolutionDataRelationship(licensePlate = lp, fresh = false)
        testOutdatedRelationship(rawStorageData, resolutionData)
      }

      "relationship contains mismatching license plate in resolution data" in {
        val rawStorageData = createRawStorageRelationship(vin = vin.toString, licensePlate = lp.toString, fresh = false)
        val resolutionData = createResolutionDataRelationship(licensePlate = LicensePlate("K516CE99"), fresh = true)
        testOutdatedRelationship(rawStorageData, resolutionData)
      }
    }
  }

  it should {
    "getFreshTrustedVin" when {
      "not trusted and no fresh vin code exists" in {
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(Some {
          val now = System.currentTimeMillis()
          val holder = VinHistoryHolder.newBuilder()
          holder.addEntries(
            VinEntry
              .newBuilder()
              .setFrom(now)
              .setTrusted(false)
              .setVin("XTA21099043576183")
          )
          LicensePlateHistory
            .newBuilder()
            .setEventType(EventType.VIN_CODES)
            .setVinHistory(holder)
            .build()
        }))

        manager.getLastTrustedVin(lp).await shouldBe None
      }

      "trusted fresh vin codes exists" in {
        val now = System.currentTimeMillis()
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(Some {
          val holder = VinHistoryHolder.newBuilder()
          holder
            .addEntries(
              VinEntry
                .newBuilder()
                .setFrom(now - 10.minutes.toMillis)
                .setTrusted(true)
                .setVin("XTA21099043576182")
            )
            .addEntries(
              VinEntry
                .newBuilder()
                .setFrom(now)
                .setTrusted(false)
                .setVin("XTA21099043576183")
            )
          LicensePlateHistory
            .newBuilder()
            .setEventType(EventType.VIN_CODES)
            .setVinHistory(holder)
            .build()
        }))

        manager.getLastTrustedVin(lp).await shouldBe Some(VinCode("XTA21099043576182"))
      }
    }
  }

  it should {
    "raise InvalidLicensePlateException" when {
      "makeCreateLicensePlateRequest raise AutocodeInvalidLicensePlateException" in {
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(None))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.failed(new AutocodeInvalidIdentifier(lp)))
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)

        intercept[InvalidLicensePlateException] {
          manager.resolveVinAndBuildResponse(lp).await
        }
      }
    }

    "raise VinInProgressException" when {
      "make order network problems" in {
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(None))
        when(httpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.failed(RequestTimeout(new SocketTimeoutException)))
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(None)
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)

        intercept[VinInProgressException] {
          manager.resolveVinAndBuildResponse(lp).await
        }
      }

      "regenerate order network problems" in {
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(Some {
          val now = System.currentTimeMillis()
          val holder = VinHistoryHolder.newBuilder()
          holder.addEntries(
            VinEntry
              .newBuilder()
              .setFrom(now)
              .setTrusted(true)
              .setVin("XTA21099043576183")
          )
          LicensePlateHistory
            .newBuilder()
            .setEventType(EventType.VIN_CODES)
            .setVinHistory(holder)
            .build()
        }))
        when(vinDataManager.getLastRelationshipByLp(eq(lp))(?)).thenReturn(Future.successful(None))
        when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(
          Future.successful(
            ResolutionData
              .empty(vinCode)
              .copy(
                identifiers = KnownIdentifiers.empty.copy(
                  lastLp = Some(
                    IdentifierContainer(
                      LicensePlate.apply("K718CE178"),
                      None,
                      System.currentTimeMillis() - 32.days.toMillis
                    )
                  )
                )
              )
          )
        )
        when(httpManager.makeRegenerateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.failed(RequestTimeout(new SocketTimeoutException())))
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(Some {
          WatchingStateHolder(
            lp, {
              val state = CompoundState.newBuilder()
              val report = state.getAutocodeStateBuilder.getReportBuilder(AutocodeReportType.Identifiers)
              state.getAutocodeStateBuilder.addAutocodeReports(report)
              state.build()
            },
            0
          )
        })
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)

        intercept[VinInProgressException] {
          manager.resolveVinAndBuildResponse(lp, Some(1.day)).await
        }
      }

      "get report network problems" in {
        when(lpHistoryManager.getInfoByType(?, eq(EventType.VIN_CODES))(?)).thenReturn(Success(Some {
          val now = System.currentTimeMillis()
          val holder = VinHistoryHolder.newBuilder()
          holder.addEntries(
            VinEntry
              .newBuilder()
              .setFrom(now)
              .setTrusted(true)
              .setVin("XTA21099043576183")
          )
          LicensePlateHistory
            .newBuilder()
            .setEventType(EventType.VIN_CODES)
            .setVinHistory(holder)
            .build()
        }))
        when(vinDataManager.getLastRelationshipByLp(eq(lp))(?)).thenReturn(Future.successful(None))
        when(vinDataManager.getLatestResolutionData(?, ?)(?)).thenReturn(
          Future.successful(
            ResolutionData
              .empty(vinCode)
              .copy(
                identifiers = KnownIdentifiers.empty.copy(
                  lastLp = Some(
                    IdentifierContainer(LicensePlate("K718CE178"), None, System.currentTimeMillis() - 32.days.toMillis)
                  )
                )
              )
          )
        )
        when(lpUpdateService.getState(?, ?)(?)).thenReturn(Some {
          WatchingStateHolder(
            lp, {
              val state = CompoundState.newBuilder()
              val report = state.getAutocodeStateBuilder
                .getReportBuilder(AutocodeReportType.Identifiers)
                .setRequestSent(1)
              state.getAutocodeStateBuilder.addAutocodeReports(report)
              state.build()
            },
            0
          )
        })
        doNothing().when(lpUpdateService).upsertUpdate(?)(?)(?)
        when(httpManager.getResult(?)(?, ?))
          .thenReturn(Future.failed(RequestTimeout(new SocketTimeoutException())))

        intercept[VinInProgressException] {
          manager.resolveVinAndBuildResponse(lp, Some(1.day)).await
        }
      }
    }
  }
}
