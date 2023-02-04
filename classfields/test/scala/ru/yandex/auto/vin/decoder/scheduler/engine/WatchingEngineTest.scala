package ru.yandex.auto.vin.decoder.scheduler.engine

import io.prometheus.client.Histogram
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate.{AsyncMaxDelay, AsyncMinDelay}
import ru.yandex.auto.vin.decoder.scheduler.models.{Delay, ExactDelay, State, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.{Instrumentation, ProcessingStage, StageSupport}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.{MockitoSupport, NotMockedException}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Future
import scala.concurrent.duration._

class WatchingEngineTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfterAll with Matchers {

  private val vin = VinCode("X4X3D59430PS96744")

  implicit val m: MetricsSupport = TestOperationalSupport

  def mockStageCustom[T, S: State]: MockProcessingStage[T, S] = new MockProcessingStage[T, S]

  def mockStage[T, S: State]: ProcessingStage[T, S] = {
    val h = MockProcessingStage[T, S]()
    val res = mock[ProcessingStage[T, S]]
    when(res.instrumentation).thenReturn(h.instrumentation)
    when(res.prometheusProcessTimer).thenReturn(h.prometheusProcessTimer)
    when(res.name).thenReturn("stub")
    res
  }

  def createEngine[T, S: State](
      list: List[ProcessingStage[T, S]] = List.empty[ProcessingStage[T, S]]): WatchingEngine[T, S] =
    new WatchingEngine[T, S] {
      override def stages: List[ProcessingStage[T, S]] = list
    }

  def createProcessingState[S: State](
      compoundState: S,
      delay: Delay = WatchingStateUpdate.DefaultSyncDelay): ProcessingState[S] = {
    ProcessingState(WatchingStateUpdate(compoundState, delay))
  }

  protected case class MockProcessingStage[T, S: State](
      sp: Option[ProcessingState[S] => Boolean] = None,
      f: Option[(T, ProcessingState[S]) => ProcessingState[S]] = None,
      var counter: Int = 0
    )(implicit val m: MetricsSupport)
    extends ProcessingStage[T, S] {

    override def shouldProcess(state: ProcessingState[S]): Boolean =
      sp.getOrElse(throw new NotMockedException("should process"))(state)

    override def process(id: T, state: ProcessingState[S]): ProcessingState[S] = {
      counter = counter + 1
      f.getOrElse(throw new NotMockedException("process"))(id, state)
    }

    def whenShouldProcess(f: ProcessingState[S] => Boolean): MockProcessingStage[T, S] =
      this.copy(sp = Some(f))

    def whenProcess(f: (T, ProcessingState[S]) => ProcessingState[S]): MockProcessingStage[T, S] =
      this.copy(f = Some(f))
  }

  implicit val i: Instrumentation = Instrumentation(
    Histogram.build("test", "test hist").labelNames("one", "two", "three").create(),
    "test"
  )

  "update engine" should {

    "fix default delay without async ops in post process " in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = StageSupport.createDefaultProcessingState(CompoundState.newBuilder().build())

      val res = engine.postProcess(vin, state)
      res.compoundStateUpdate.delay shouldBe ExactDelay(365.days)
      res.compoundStateUpdate.state.getLastVisited != 0 shouldBe true
    }

    "fix default delay with async ops in post process " in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = StageSupport.createDefaultProcessingState(CompoundState.newBuilder().build())
      val withAsync = state.withAsyncOperation("test")(f => f)

      val res = engine.postProcess(vin, withAsync)
      assert(res.compoundStateUpdate.delay.toDuration >= AsyncMinDelay)
      assert(res.compoundStateUpdate.delay.toDuration <= AsyncMaxDelay)
      res.compoundStateUpdate.state.getLastVisited != 0 shouldBe true
    }

    "fix big exact delay with async ops in post process " in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = createProcessingState(CompoundState.newBuilder().build(), ExactDelay(5.hour))
      val withAsync = state.withAsyncOperation("test")(f => f)

      val res = engine.postProcess(vin, withAsync)
      res.compoundStateUpdate.delay shouldBe ExactDelay(10.minutes)
      res.compoundStateUpdate.state.getLastVisited != 0 shouldBe true
    }

    "not fix exact delay with async ops in post process" in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = createProcessingState(CompoundState.newBuilder().build(), ExactDelay(1.hour))
      val withAsync = state.withAsyncOperation("test")(f => f)

      val res = engine.postProcess(vin, withAsync)
      res.compoundStateUpdate.delay shouldBe ExactDelay(1.hour)
      res.compoundStateUpdate.state.getLastVisited != 0 shouldBe true
    }

    "not fix exact delay without async ops in post process" in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = createProcessingState(CompoundState.newBuilder().build(), ExactDelay(10.hour))

      val res = engine.postProcess(vin, state)
      res.compoundStateUpdate.delay shouldBe ExactDelay(10.hour)
      res.compoundStateUpdate.state.getLastVisited != 0 shouldBe true
    }

    "fix default delay without async ops in post process async" in {
      val engine = createEngine[VinCode, CompoundState]()

      val state = WatchingStateUpdate.defaultSync(CompoundState.newBuilder().build())

      val res = engine.postProcessAsync(vin, state)
      res.delay shouldBe ExactDelay(365.days)
      res.state.getLastVisited != 0 shouldBe true
    }

    "do not call stages with should process in false" in {
      val stage = mock[ProcessingStage[VinCode, CompoundState]]
      when(stage.shouldProcess(?)).thenReturn(false)

      val engine = createEngine[VinCode, CompoundState](List(stage))
      engine.process(vin, CompoundState.newBuilder().build())

      verify(stage, never()).run(?, ?)
    }

    "do not process stages after errors" in {
      val state = CompoundState.newBuilder().build()
      val pState = StageSupport.createDefaultProcessingState(state)
      val stage1 = mockStage[VinCode, CompoundState]
      val stage2 = mockStageCustom[VinCode, CompoundState]
        .whenShouldProcess(_ => true)
        .whenProcess((_, _) => throw new RuntimeException("xo"))
      val stage3 = mockStageCustom[VinCode, CompoundState]
        .whenShouldProcess(_ => true)

      when(stage1.shouldProcess(?)).thenReturn(true)
      when(stage1.run(?, ?)).thenReturn(Some(pState))

      val engine = createEngine[VinCode, CompoundState](List(stage1, stage2, stage3))
      engine.process(vin, CompoundState.newBuilder().build())

      verify(stage1, times(1)).run(?, ?)
      stage2.counter shouldBe 1
      stage3.counter shouldBe 0
    }

    "not reschedule far away if there are failed async ops" in {
      val engine = createEngine[VinCode, CompoundState]()
      val pState = StageSupport
        .createDefaultProcessingState(CompoundState.newBuilder.build)
        .withAsyncOperationF("failure")(_ => Future.failed(new RuntimeException))
      val res = engine.processAsync(vin, pState).await
      res.nonEmpty shouldBe true
      res.get._2.delay().toDuration.lt(2.hours) shouldBe true
    }
  }
}
