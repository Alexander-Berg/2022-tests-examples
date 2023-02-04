package ru.yandex.auto.vin.decoder.manager

import auto.carfax.common.utils.tracing.Traced
import cats.implicits._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AsyncFunSpec
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.api.exceptions.{UnknownVinException, VinInProgressException}
import ru.yandex.auto.vin.decoder.manager.vin._
import ru.yandex.auto.vin.decoder.manager.vin.autocode.VinAutocodeManager
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.model.vin.Event
import ru.yandex.auto.vin.decoder.model.{CommonVinCode, ResolutionData, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.model._
import ru.yandex.auto.vin.decoder.partners.autocode.{AutocodeHttpManager, AutocodeReportType, AutocodeRequest}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import ru.yandex.auto.vin.decoder.service.vin.VinUpdateService
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.misc.StringUtils._
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class RegistrationManagerSpec extends AsyncFunSpec with MockitoSupport with BeforeAndAfterEach {
  implicit private val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t: Traced = Traced.empty

  private val autocodeHttpManager = mock[AutocodeHttpManager]
  private val vinUpdateManager = mock[VinUpdateManager]
  private val vinUpdateService = mock[VinUpdateService]
  private val vinAutocodeManager = mock[VinAutocodeManager]
  private val unificator = mock[Unificator]
  private val vinResolutionManager = mock[VinDataManager]

  private val manager = new RegistrationManager(
    autocodeHttpManager,
    vinUpdateService,
    vinAutocodeManager,
    vinResolutionManager
  )

  private val vinCode: VinCode = CommonVinCode("SALWA2FK7HA135034")
  private val resolutionData = ResolutionData.empty(vinCode)
  private val autocodeRequest = Future.successful(AutocodeRequest("request_id", vinCode, AutocodeReportType.Tech))

  override protected def beforeEach(): Unit = {
    reset(autocodeHttpManager)
    reset(vinResolutionManager)
    reset(vinUpdateManager)
    reset(vinAutocodeManager)
    reset(unificator)
    doNothing().when(vinUpdateService).upsertUpdate(?)(?)(?)
  }

  private def makeAutocodeResponse(stWait: Int = 0, techMarkModel: String = "") =
    Future.successful {
      val identifiers = AutocodeReportIdentifiers(vinCode.toString.some, None, None, None, None, None)
      val techData = techMarkModel.toOption.map(AutocodeReportTechData(_, "", 0, 0, 0, "", "", "", "", ""))
      val content =
        Content(
          identifiers.some,
          techData,
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
        )
      val model = AutocodeReportResponse(
        "OK",
        Data(
          AutocodeReportType.Tech.id,
          Query(AutocodeQueryType.VIN, vinCode.toString),
          2,
          stWait,
          2,
          List.empty,
          Some(content),
          "2012-10-01T09:45:00.000+02:00",
          ""
        )
      )

      AutocodeReportResponseRaw("", "", model)
    }

  private def makeVinInfoHistory(eventType: EventType, mark: String, timestamp: Option[Long] = None) = {
    val builder = VinInfoHistory
      .newBuilder()
      .setEventType(eventType)

    eventType match {
      case EventType.AUTOCODE_REGISTRATION | EventType.AUTOCODE_REGISTRATION | EventType.AUTOCODE_MOS_REGISTRATION =>
        val registrationBuilder = builder.getRegistrationBuilder
          .setMark(mark)
        timestamp.foreach(registrationBuilder.setTimestamp)
      case EventType.AUTOCODE_TTX =>
        val ttxBuilder = builder.getAutocodeTtxBuilder
          .setMark(mark)
        timestamp.foreach(ttxBuilder.setTimestamp)
      case _ => throw new IllegalArgumentException("not allowed")

    }

    builder.build()
  }

  describe("VehicleInfoManager") {
    it("should return registration") {
      val now = System.currentTimeMillis()
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(
          Some(
            WatchingStateHolder(
              vinCode, {
                val builder = CompoundState.newBuilder()
                builder.getAutocodeStateBuilder
                  .getReportBuilder(AutocodeReportType.Tech)
                  .setReportId("report_id")
                  .setRequestSent(now - 2)
                  .setReportArrived(now - 1)
                builder.build()
              },
              now
            )
          )
        )
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          resolutionData.copy(
            registration = Some(
              Prepared.simulate(makeVinInfoHistory(EventType.AUTOCODE_REGISTRATION, "TOYOTA"))
            )
          )
        )
      )
      manager.getInfo(vinCode).map { info =>
        assert(info.isLeft)
      }
    }

    it("should raise UnknownVinException for fresh ttx") {
      val now = System.currentTimeMillis()
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(
          Some(
            WatchingStateHolder(
              vinCode, {
                val builder = CompoundState.newBuilder()
                builder.getAutocodeStateBuilder
                  .getReportBuilder(AutocodeReportType.Tech)
                  .setReportId("report_id")
                  .setRequestSent(now - 2)
                  .setReportArrived(now - 1)
                builder.build()
              },
              now
            )
          )
        )
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          resolutionData
            .copy(autocodeTtx = Some(makeVinInfoHistory(EventType.AUTOCODE_TTX, Event.NoMark, Some(now))))
        )
      )
      recoverToSucceededIf[UnknownVinException](manager.getInfo(vinCode))
    }

    it("should go to partner and save to database cuz no ttx") {
      val now = System.currentTimeMillis()
      when(vinUpdateService.getState(?, ?)(?)).thenReturn(
        Some(
          WatchingStateHolder(
            vinCode,
            CompoundState.newBuilder().build(),
            System.currentTimeMillis()
          )
        )
      )
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          resolutionData
            .copy(autocodeTtx = Some(makeVinInfoHistory(EventType.AUTOCODE_TTX, Event.NoMark, Some(now))))
        )
      )
      when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse(techMarkModel = "Toyota Camry"))
      when(unificator.unify(?, ?)(?))
        .thenReturn(Future.successful(List(MarkModelResult("TOYOTA", "CAMRY", "", unclear = false))))
      when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
      when(vinAutocodeManager.createTtx(?, ?)(?))
        .thenReturn(Future.successful(VinInfoHistory.newBuilder().build()))

      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }

    it("should return ttx") {
      val now = System.currentTimeMillis()
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(
          Some(
            WatchingStateHolder(
              vinCode, {
                val builder = CompoundState.newBuilder()
                builder.getAutocodeStateBuilder
                  .getReportBuilder(AutocodeReportType.Tech)
                  .setReportId("report_id")
                  .setRequestSent(now - 2)
                  .setReportArrived(now - 1)
                builder.build()
              },
              now
            )
          )
        )
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          resolutionData
            .copy(autocodeTtx = Some(makeVinInfoHistory(EventType.AUTOCODE_TTX, "TOYOTA")))
        )
      )
      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }

    it("should return ttx if no registration") {
      val now = System.currentTimeMillis()
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(
          Some(
            WatchingStateHolder(
              vinCode, {
                val builder = CompoundState.newBuilder()
                builder.getAutocodeStateBuilder
                  .getReportBuilder(AutocodeReportType.Tech)
                  .setReportId("report_id")
                  .setRequestSent(now - 2)
                  .setReportArrived(now - 1)
                builder.build()
              },
              now
            )
          )
        )
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(
        Future.successful(
          resolutionData
            .copy(
              autocodeTtx = Some(makeVinInfoHistory(EventType.AUTOCODE_TTX, "TOYOTA")),
              registration = Some(
                Prepared.simulate(makeVinInfoHistory(EventType.AUTOCODE_REGISTRATION, Event.NoMark))
              )
            )
        )
      )
      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }

    it("should go to partner and get empty response") {
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(Some(WatchingStateHolder(vinCode, CompoundState.newBuilder().build(), System.currentTimeMillis())))
      when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse())
      recoverToSucceededIf[UnknownVinException](manager.getInfo(vinCode))
    }

    it("should go to partner and report in progress") {
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))
      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(Some(WatchingStateHolder(vinCode, CompoundState.newBuilder().build(), System.currentTimeMillis())))
      when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse(2))
      recoverToSucceededIf[VinInProgressException](manager.getInfo(vinCode))
    }

    it("should go to partner and save to database") {
      when(vinAutocodeManager.createTtx(?, ?)(?))
        .thenReturn(Future.successful(VinInfoHistory.newBuilder().build()))
      when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))

      when(vinUpdateService.getState(?, ?)(?))
        .thenReturn(Some(WatchingStateHolder(vinCode, CompoundState.newBuilder().build(), System.currentTimeMillis())))
      when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse(techMarkModel = "Toyota Camry"))
      when(unificator.unify(?, ?)(?))
        .thenReturn(Future.successful(List(MarkModelResult("TOYOTA", "CAMRY", "", unclear = false))))
      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }

    it("should go to partner and save to database with no info") {
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))
      when(vinUpdateService.getState(?, ?)(?)).thenReturn {
        Some(
          WatchingStateHolder(
            vinCode, {
              val builder = CompoundState.newBuilder()
              builder.getAutocodeStateBuilder
                .getReportBuilder(AutocodeReportType.Tech)
                .setReportId("report_id")
                .setNoInfo(true)
              builder.build()
            },
            System.currentTimeMillis()
          )
        )
      }
      when(autocodeHttpManager.makeRegenerateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse(techMarkModel = "Toyota Camry"))
      when(unificator.unify(?, ?)(?))
        .thenReturn(Future.successful(List(MarkModelResult("TOYOTA", "CAMRY", "", unclear = false))))
      when(vinAutocodeManager.createTtx(?, ?)(?))
        .thenReturn(Future.successful(VinInfoHistory.newBuilder().build()))
      when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }

    it("should raise UnknownVinException for fresh registration for fresh state") {
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))
      when(vinUpdateService.getState(?, ?)(?)).thenReturn {
        Some(
          WatchingStateHolder(
            vinCode, {
              val builder = CompoundState.newBuilder()
              builder.getAutocodeStateBuilder
                .getReportBuilder(AutocodeReportType.Tech)
                .setReportArrived(System.currentTimeMillis())
                .setNoInfo(true)
              builder.build()
            },
            System.currentTimeMillis()
          )
        )
      }
      recoverToSucceededIf[UnknownVinException](manager.getInfo(vinCode))
    }

    it("should go to partner without state") {
      when(vinAutocodeManager.createTtx(?, ?)(?))
        .thenReturn(Future.successful(VinInfoHistory.newBuilder().build()))
      when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
      when(vinResolutionManager.getLatestResolutionData(?, ?)(?)).thenReturn(Future.successful(resolutionData))

      when(vinUpdateService.getState(?, ?)(?)).thenReturn(None)
      when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?)).thenReturn(autocodeRequest)
      when(autocodeHttpManager.getResult(?)(?, ?)).thenReturn(makeAutocodeResponse(techMarkModel = "Toyota Camry"))
      when(unificator.unify(?, ?)(?))
        .thenReturn(Future.successful(List(MarkModelResult("TOYOTA", "CAMRY", "", unclear = false))))
      manager.getInfo(vinCode).map { info =>
        assert(info.isRight)
      }
    }
  }
}
