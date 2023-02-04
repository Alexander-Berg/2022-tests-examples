package ru.yandex.auto.vin.decoder.scheduler.workers.partners

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.clients.avilon.AvilonClient
import ru.yandex.auto.vin.decoder.clients.avilon.model.AvilonServiceBookItemModel
import ru.yandex.auto.vin.decoder.hydra.AvilonRequestsClicker
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.avilon.{AvilonServiceBookRawModel, AvilonServiceBookRawToPreparedConverter}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AvilonWorkerSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with MockitoSupport {

  private val vinCode = VinCode("Z8T4C5FS9BM005269")

  implicit val t = Traced.empty
  implicit val tracer = NoopTracerFactory.create()
  val avilonRequestsClicker = mock[AvilonRequestsClicker]

  def buildAvilonWorker(aviloClient: AvilonClient) =
    new AvilonWorker(
      aviloClient,
      avilonRequestsClicker,
      rawStorageManager,
      new AvilonServiceBookRawToPreparedConverter,
      rateLimiter,
      vinUpdateDao,
      queue,
      Feature("Avilon", _ => true)
    )(metrics, tracer)

  def avilonClient(response: Future[String]) = new AvilonClient {

    override def getServiceBooks(
        vin: VinCode
      )(implicit t: Traced,
        trigger: PartnerRequestTrigger): Future[AvilonServiceBookRawModel] =
      response.map { raw =>
        val sbModels = Json.parse(raw).as[List[AvilonServiceBookItemModel]]
        AvilonServiceBookRawModel(raw, "200", vinCode, sbModels)
      }
  }
  val rawStorageManager = mock[RawStorageManager[VinCode]]
  val rateLimiter = mock[RateLimiter]
  val vinUpdateDao = mock[VinWatchingDao]
  val queue = mock[WorkersQueue[VinCode, CompoundState]]
  val metrics = TestOperationalSupport

  override def beforeAll(): Unit = {
    reset(rateLimiter)
  }

  "AvilonWorker" must {

    when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

    "process correct response from avilon" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?))
        .thenReturn(Future.successful(Seq(Prepared(0, 0, 0, VinInfoHistory.newBuilder().build(), ""))))
      when(avilonRequestsClicker.getAvaliableClicks(?)).thenReturn(Future.successful(1000))

      val ts = System.currentTimeMillis()
      val succeedResponse = Future.successful(
        s"""[{"vin":"$vinCode","id":"123","event_date":"2016-04-12 20:34:36","event_region":"Москва","event_city":"Москва",
         |"type":"ordinary_dealer","brand":"Фольксваген","model":"Jetta 6","mileage":58525,
         |"works":["Аккумуляторная батарея зарядить","Рулевой механизм EML снять и установить"],
         |"products":["Болт (kombi)","Болт с шестигр. гол. (комби) м10х35","Болт м10х75","Болт м10х76","Болт м8х35х22"],
         |"client_type":1,"year":2015,"recommendations":[]}]""".stripMargin
      )

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(succeedResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe false
      avilonState.getNotFound shouldBe false
      avilonState.getLastCheck should be >= ts
    }

    "process correct response from avilon then items is empty" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?))
        .thenReturn(Future.successful(Seq(Prepared(0, 0, 0, VinInfoHistory.newBuilder().build(), ""))))
      when(avilonRequestsClicker.getAvaliableClicks(?)).thenReturn(Future.successful(1000))

      val ts = System.currentTimeMillis()
      val emptyResponse = Future.successful("[]")

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(emptyResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe false
      avilonState.getNotFound shouldBe true
      avilonState.getLastCheck should be >= ts
    }

    "process failure correctly when vin is not in AVILON_VINS" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?))
        .thenReturn(Future.successful(Seq.empty[Prepared]))
      when(avilonRequestsClicker.getAvaliableClicks(?)).thenReturn(Future.successful(1000))

      val ts = System.currentTimeMillis()
      val emptyResponse = Future.successful("[]")

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(emptyResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe false
      avilonState.getNotFound shouldBe false
      avilonState.getLastCheck should be >= ts
    }

    "process failure correctly when avilon quota is excised" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?))
        .thenReturn(Future.successful(Seq(Prepared(0, 0, 0, VinInfoHistory.newBuilder().build(), ""))))
      when(avilonRequestsClicker.getAvaliableClicks(?)).thenReturn(Future.successful(0))

      val emptyResponse = Future.successful("[]")

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(emptyResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe true
    }

    "process failure correctly when getting response from avilon" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rawStorageManager.getAllPrepared(?, ?)(?))
        .thenReturn(Future.successful(Seq(Prepared(0, 0, 0, VinInfoHistory.newBuilder().build(), ""))))
      when(avilonRequestsClicker.getAvaliableClicks(?)).thenReturn(Future.successful(1000))

      val failedResponse = Future.failed(new RuntimeException)

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(failedResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe true
    }

    "process failure correctly when rate limiter is exceed" in {

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(false)

      val noMatterResponse = Future.successful("blah blah")

      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvilonStateBuilder.setShouldProcess(true)
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)
      val res = buildAvilonWorker(avilonClient(noMatterResponse)).action(stateHolder)

      val avilonState = res.updater.get(stateHolder.toUpdate).state.getAvilonState

      res.reschedule shouldBe false
      avilonState.getShouldProcess shouldBe true
    }

    "process failure correctly when there is no avilon state" in {

      val noMatterResponse = Future.successful("blah blah")

      val stateBuilder = CompoundState.newBuilder()
      val stateHolder = WatchingStateHolder(vinCode, stateBuilder.build(), 1)

      an[IllegalArgumentException] should be thrownBy
        buildAvilonWorker(avilonClient(noMatterResponse)).action(stateHolder)
    }

  }

}
