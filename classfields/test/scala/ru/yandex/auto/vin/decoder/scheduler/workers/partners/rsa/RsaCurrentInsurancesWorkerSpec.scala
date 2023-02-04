package ru.yandex.auto.vin.decoder.scheduler.workers.partners.rsa

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.enablers.Emptiness.emptinessOfJavaCollection
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.manager.IdentifiersManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.converter.RsaRawToPreparedConverter
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.model.RsaResponseRawModel
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.{ScrapingHubRsaClient, ScrapinghubRsaReportType}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RsaCurrentInsurancesWorkerSpec
  extends RsaWorkerSpec
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockitoSupport {

  implicit val t: Traced = Traced.empty
  implicit val tracer = NoopTracerFactory.create()
  val rateLimiter: RateLimiter = mock[RateLimiter]
  val rawStorageManager: RawStorageManager[VinCode] = mock[RawStorageManager[VinCode]]
  val shRsaClient: ScrapingHubRsaClient[VinCode] = mock[ScrapingHubRsaClient[VinCode]]
  val identifiersManager: IdentifiersManager = mock[IdentifiersManager]
  val vinUpdateDao: VinWatchingDao = mock[VinWatchingDao]
  val queue: WorkersQueue[VinCode, CompoundState] = mock[WorkersQueue[VinCode, CompoundState]]
  val converter = new RsaRawToPreparedConverter
  implicit val metrics: TestOperationalSupport.type = TestOperationalSupport

  val rsaCurrentInsurancesWorker = new RsaCurrentInsurancesWorker(
    shRsaClient,
    converter,
    rateLimiter,
    vinUpdateDao,
    rawStorageManager,
    identifiersManager,
    queue,
    Feature("SH RSA", _ => true)
  )

  when(identifiersManager.connect(?, ?, ?, ?)(?)).thenReturn(Future.unit)
  when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

  val currentInsurancesReportType: ScrapinghubRsaReportType = ScrapinghubRsaReportType.CurrentInsurances
  val insuranceDetailsReportType: ScrapinghubRsaReportType = ScrapinghubRsaReportType.InsuranceDetails
  val vin: VinCode = VinCode("XW8ZZZ3CZ9G001083")

  "RsaCurrentInsurancesWorker" should {

    "process correct response from RSA" in {

      val responseRaw = getStringFromResouce("/scrapinghub/rsa/current_insurances.json")
      val successResponse = RsaResponseRawModel(200, responseRaw, vin, "", currentInsurancesReportType)

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsurances(?, ?)(?, ?)).thenReturn(Future.successful(successResponse))

      val ts = System.currentTimeMillis()

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(currentInsurancesReportType).setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaCurrentInsurancesWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, currentInsurancesReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe currentInsurancesReportType.toString
      reportState.getShouldProcess shouldBe false
      reportState.getStateUpdateHistoryList shouldBe empty
      reportState.getLastCheck should be >= ts
    }

    "process failure correctly when response from RSA is empty" in {

      val emptyRaw = """{"items":{"request_result":[], "sh_last_visited": "2021-01-28T16:16:21+0300"}}"""
      val emptyResponse = RsaResponseRawModel(200, emptyRaw, vin, "", currentInsurancesReportType)

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsurances(?, ?)(?, ?)).thenReturn(Future.successful(emptyResponse))

      val ts = System.currentTimeMillis()

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(currentInsurancesReportType).setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaCurrentInsurancesWorker.action(stateHolder)

      val currentInsurancesState = getReportState(res, stateHolder, currentInsurancesReportType)
      val insuranceDetailsState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false

      currentInsurancesState.getReportType shouldBe currentInsurancesReportType.toString
      currentInsurancesState.getShouldProcess shouldBe false
      currentInsurancesState.getLastCheck should be >= ts

      insuranceDetailsState.getReportType shouldBe insuranceDetailsReportType.toString
      insuranceDetailsState.getShouldProcess shouldBe false
      insuranceDetailsState.getLastCheck should be >= ts
    }

    "process failure correctly when getting response from rsa" in {
      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsurances(?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException))

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(currentInsurancesReportType).setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaCurrentInsurancesWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, currentInsurancesReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe currentInsurancesReportType.toString
      reportState.getShouldProcess shouldBe true
      reportState.getLastCheck shouldBe 0
    }

    "process failure correctly when rate limiter is exceed" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(false)

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(currentInsurancesReportType).setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaCurrentInsurancesWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, currentInsurancesReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe currentInsurancesReportType.toString
      reportState.getShouldProcess shouldBe true
      reportState.getLastCheck shouldBe 0
    }

    "process failure correctly when there is no sh rsa state" in {

      val stateBuilder = CompoundState.newBuilder()
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)

      an[IllegalArgumentException] should be thrownBy rsaCurrentInsurancesWorker.action(stateHolder)
    }
  }
}
