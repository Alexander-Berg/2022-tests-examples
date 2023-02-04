package ru.yandex.auto.vin.decoder.scheduler.workers.queue

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.annotation.nowarn

class InMemoryWorkersQueueTest extends AnyFunSuite {

  test("check duplicates") {
    val vin = VinCode.apply("WF04XXGBB46J13944")
    val testState = WatchingStateHolder(vin, CompoundState.newBuilder().build(), 0)
    val queue =
      new DefaultInMemoryWorkersQueue[VinCode, CompoundState](s"test_${System.nanoTime()}", TestOperationalSupport)

    // empty
    assert((queue.size(): @nowarn) == 0)

    // don't allow duplicates
    assert(queue.enqueue(vin, testState))
    assert(!queue.enqueue(vin, testState))
    assert((queue.size(): @nowarn) == 1)

    // peek don't remove offer from queue
    val peeked = queue.dequeue()
    assert(peeked.get.value.contains(testState))
    assert(!queue.enqueue(vin, testState))
    assert((queue.size(): @nowarn) == 1)

    // finish and enqueue again
    peeked.get.finish()
    assert((queue.size(): @nowarn) == 0)
    assert(queue.enqueue(vin, testState))
    assert((queue.size(): @nowarn) == 1)
  }

  test("limit max size") {
    val vin1 = VinCode.apply("WF04XXGBB46J13941")
    val testState1 = WatchingStateHolder(vin1, CompoundState.newBuilder().build(), 0)
    val vin2 = VinCode.apply("WF04XXGBB46J13942")
    val testState2 = WatchingStateHolder(vin2, CompoundState.newBuilder().build(), 0)
    val vin3 = VinCode.apply("WF04XXGBB46J13943")
    val testState3 = WatchingStateHolder(vin3, CompoundState.newBuilder().build(), 0)

    val queue =
      new DefaultInMemoryWorkersQueue[VinCode, CompoundState](
        s"test_${System.nanoTime()}",
        TestOperationalSupport,
        maxSize = 2
      )

    assert(queue.enqueue(vin1, testState1))
    assert((queue.size(): @nowarn) == 1)
    assert(queue.enqueue(vin2, testState2))
    assert((queue.size(): @nowarn) == 2)
    assert(!queue.enqueue(vin3, testState3))
    assert((queue.size(): @nowarn) == 2)
  }

}
