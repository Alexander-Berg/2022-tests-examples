package ru.yandex.auto.vin.decoder.scheduler.models

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState

import scala.concurrent.duration.{Duration, DurationInt}

class WatchingStateHolderTest extends AnyWordSpec {
  val vin = VinCode("WBAUE11040E238571")
  val state = CompoundState.newBuilder().build()

  implicit val cs: State[CompoundState] = new State[CompoundState] {

    override def withLastVisited(state: CompoundState, lastVisited: Long): CompoundState = {
      state.toBuilder.setLastVisited(lastVisited).build()
    }

    override def getLastVisited(state: CompoundState): Long = {
      state.getLastVisited
    }
  }

  "WatchingStateHolder".can {
    "toUpdate" should {
      "assign zero delay as the timestamp is zero" in {
        val timestamp = 0L
        val updatingHolder = WatchingStateHolder(vin, state, timestamp).toUpdate
        assert(updatingHolder.delay == DefaultDelay(Duration.Zero))
      }
      "assign zero delay as the timestamp is in the past" in {
        val timestamp = 123L
        val updatingHolder = WatchingStateHolder(vin, state, timestamp).toUpdate
        assert(updatingHolder.delay == DefaultDelay(Duration.Zero))
      }

      "calculate delay correctly as the timestamp is in the future" in {
        val timestamp = System.currentTimeMillis() + 2.minutes.toMillis
        val updatingHolder = WatchingStateHolder(vin, state, timestamp).toUpdate
        assert(updatingHolder.delay.toDuration <= 2.minutes)
        assert(updatingHolder.delay.toDuration >= 100.seconds)
      }
    }
  }
}
