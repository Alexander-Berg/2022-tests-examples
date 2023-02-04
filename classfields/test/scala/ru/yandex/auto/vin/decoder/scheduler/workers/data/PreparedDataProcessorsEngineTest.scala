package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.utils.tracing.Traced
import cats.syntax.option._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.scheduler.engine.Updater.UpdaterImpl
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.workers.WorkResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class PreparedDataProcessorsEngineTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfter {

  private val mockedProcessor = mock[AsyncPreparedDataProcessor[VinCode]]
  private val vin = VinCode("X4X3D59430PS96744")
  implicit private val ops: MetricsSupport = TestOperationalSupport
  implicit val t: Traced = Traced.empty

  "process" should {
    "do nothing" when {
      "there are not active prepared data states" in {
        val state = {
          val builder = CompoundState.newBuilder()
          builder.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, false))
          builder.build()
        }

        val engine = new PreparedDataProcessorsEngine[VinCode](Map(EventType.ACAT_INFO -> mockedProcessor))
        val res = engine.process(WatchingStateHolder(vin, state, 123L)).await

        res.reschedule shouldBe false
        res.updater shouldBe None
      }
    }
    "finish states without async processors" in {
      val state = {
        val builder = CompoundState.newBuilder()
        builder.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, shouldProcess = true))
        builder.addPreparedDataState(createPreparedDataState(EventType.MAZDA_SERVICE_BOOK, shouldProcess = true))
        builder.build()
      }

      val engine = new PreparedDataProcessorsEngine[VinCode](Map(EventType.AVILON_SERVICE_BOOK -> mockedProcessor))
      val res = engine.process(WatchingStateHolder(vin, state, 123L)).await

      res.reschedule shouldBe false
      res.updater.isEmpty shouldBe false

      val updated = res.updater.map(updater => updater.stateUpdate(state)).get
      updated.getPreparedDataStateCount shouldBe 2
      updated.getPreparedDataState(0).getShouldProcess shouldBe false
      updated.getPreparedDataState(0).getLastCheck > 0 shouldBe true
      updated.getPreparedDataState(1).getShouldProcess shouldBe false
      updated.getPreparedDataState(1).getLastCheck > 0 shouldBe true
    }
    "execute async processor" in {
      val state = {
        val builder = CompoundState.newBuilder()
        builder.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, shouldProcess = true))
        builder.build()
      }

      val workResult = WorkResult[CompoundState](
        UpdaterImpl[CompoundState](
          "test updater",
          updState => {
            val builder = updState.toBuilder
            builder.getPreparedDataStateBuilderList.asScala
              .foreach(_.setLastCheck(111L))
            builder.build()
          },
          WatchingStateUpdate.DefaultSyncDelay
        ).some,
        true
      )
      when(mockedProcessor.process(?, ?)(?)).thenReturn(
        Future.successful(workResult)
      )
      val engine = new PreparedDataProcessorsEngine[VinCode](Map(EventType.ACAT_INFO -> mockedProcessor))
      val res = engine.process(WatchingStateHolder(vin, state, 123L)).await

      res shouldBe workResult
    }
  }

  private def createPreparedDataState(eventType: EventType, shouldProcess: Boolean): PreparedDataState = {
    PreparedDataState.newBuilder().setEventType(eventType).setShouldProcess(shouldProcess).build()
  }
}
