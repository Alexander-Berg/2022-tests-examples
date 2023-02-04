package ru.yandex.auto.vin.decoder.scheduler.workers.partners.rsa

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.enablers.Emptiness.emptinessOfJavaCollection
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
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
import scala.concurrent.duration._

class RsaInsuranceDetailsWorkerSpec
  extends RsaWorkerSpec
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with MockitoSupport {

  val rateLimiter: RateLimiter = mock[RateLimiter]
  val rawStorageManager: RawStorageManager[VinCode] = mock[RawStorageManager[VinCode]]
  val shRsaClient: ScrapingHubRsaClient[VinCode] = mock[ScrapingHubRsaClient[VinCode]]
  val vinUpdateDao: VinWatchingDao = mock[VinWatchingDao]
  val queue: WorkersQueue[VinCode, CompoundState] = mock[WorkersQueue[VinCode, CompoundState]]
  implicit val metrics: TestOperationalSupport.type = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()
  implicit val t = Traced.empty

  val rsaInsuranceDetailsWorker = new RsaInsuranceDetailsWorker(
    shRsaClient,
    new RsaRawToPreparedConverter,
    rateLimiter,
    vinUpdateDao,
    rawStorageManager,
    queue,
    Feature("SH RSA", _ => true)
  )

  when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

  val currentInsurancesReportType: ScrapinghubRsaReportType = ScrapinghubRsaReportType.CurrentInsurances
  val insuranceDetailsReportType: ScrapinghubRsaReportType = ScrapinghubRsaReportType.InsuranceDetails
  val vin: VinCode = VinCode("XW8ZZZ3CZ9G001083")

  "RsaInsuranceDetailsWorker" should {

    "process correct response from RSA" in {

      val responseRaw = getStringFromResouce("/scrapinghub/rsa/insurance_details.json")
      val successResponse = RsaResponseRawModel(200, responseRaw, vin, "", insuranceDetailsReportType)

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsuranceDetails(?, ?, ?, ?)(?, ?)).thenReturn(Future.successful(successResponse))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future(List(buildPrepared)))

      val ts = System.currentTimeMillis()

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis())
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false

      reportState.getReportType shouldBe insuranceDetailsReportType.toString
      reportState.getShouldProcess shouldBe false
      reportState.getStateUpdateHistoryList shouldBe empty
      reportState.getLastCheck should be >= ts
    }

    "process correctly when current insurances is empty" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future(Nil))

      val ts = System.currentTimeMillis()

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis())
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false

      reportState.getReportType shouldBe insuranceDetailsReportType.toString
      reportState.getShouldProcess shouldBe false
      reportState.getStateUpdateHistoryList shouldBe empty
      reportState.getLastCheck should be >= ts
    }

    "process correctly when current insurances report is not fresh and it is pending" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis() - 90.days.toMillis)
        .setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val currentInsurancesReportState = getReportState(res, stateHolder, currentInsurancesReportType)
      val insuranceDetailsReportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false

      insuranceDetailsReportState.getReportType shouldBe insuranceDetailsReportType.toString
      insuranceDetailsReportState.getShouldProcess shouldBe true
      insuranceDetailsReportState.getLastCheck shouldBe 0

      currentInsurancesReportState.getReportType shouldBe currentInsurancesReportType.toString
      currentInsurancesReportState.getShouldProcess shouldBe true
    }

    "process correctly when current insurances report is not fresh and it is not pending" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

      val ts = System.currentTimeMillis()

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis() - 90.days.toMillis)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val currentInsurancesReportState = getReportState(res, stateHolder, currentInsurancesReportType)
      val insuranceDetailsReportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false

      insuranceDetailsReportState.getReportType shouldBe insuranceDetailsReportType.toString
      insuranceDetailsReportState.getShouldProcess shouldBe true
      insuranceDetailsReportState.getLastCheck shouldBe 0

      currentInsurancesReportState.getReportType shouldBe currentInsurancesReportType.toString
      currentInsurancesReportState.getShouldProcess shouldBe true
      currentInsurancesReportState.getProcessRequested should be >= ts
    }

    "process failure correctly when response from RSA is empty" in {

      val emptyRaw = """{"items":{"request_result":[], "sh_last_visited": "2021-01-28T16:16:21+0300"}}"""
      val emptyResponse = RsaResponseRawModel(200, emptyRaw, vin, "", insuranceDetailsReportType)

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsuranceDetails(?, ?, ?, ?)(?, ?)).thenReturn(Future.successful(emptyResponse))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future(List(buildPrepared)))

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis())
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe insuranceDetailsReportType.toString
      reportState.getShouldProcess shouldBe true
      reportState.getLastCheck shouldBe 0
    }

    "process failure correctly when getting response from rsa" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(shRsaClient.getInsuranceDetails(?, ?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future(List(buildPrepared)))

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      stateBuilder.getScrapinghubRsaStateBuilder
        .getReportBuilder(currentInsurancesReportType)
        .setLastCheck(System.currentTimeMillis())
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe insuranceDetailsReportType.toString
      reportState.getShouldProcess shouldBe true
      reportState.getLastCheck shouldBe 0
    }

    "process failure correctly when rate limiter is exceed" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(false)

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getScrapinghubRsaStateBuilder.getReportBuilder(insuranceDetailsReportType).setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)
      val res = rsaInsuranceDetailsWorker.action(stateHolder)

      val reportState = getReportState(res, stateHolder, insuranceDetailsReportType)

      res.reschedule shouldBe false
      reportState.getReportType shouldBe insuranceDetailsReportType.toString
      reportState.getShouldProcess shouldBe true
      reportState.getLastCheck shouldBe 0
    }

    "process failure correctly when there is no sh rsa state" in {

      val stateBuilder = CompoundState.newBuilder()
      val stateHolder = WatchingStateHolder(vin, stateBuilder.build(), 1)

      an[IllegalArgumentException] should be thrownBy rsaInsuranceDetailsWorker.action(stateHolder)
    }
  }
}
