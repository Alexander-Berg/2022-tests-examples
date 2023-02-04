package ru.yandex.vertis.picapica.util

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.picapica.misc.Queue
import ru.yandex.vertis.picapica.util.PriorityBufferStage._

/**
  * @author pnaydenov
  */
class PriorityBufferStageSpec extends TestKit(ActorSystem("Test")) with WordSpecLike with Matchers with ScalaFutures {

  case class Elem(name: String, priority: Int)
  val FIRST = "first"
  val SECOND = "second"
  val THIRD = "third"
  val FOURTH = "fourth"
  val FIFTH = "fifth"

  val decider: Supervision.Decider = { e =>
    fail("Unhandled exception in stream", e)
    Supervision.Stop
  }
  implicit val materalizer = ActorMaterializer(ActorMaterializerSettings(system)
    .withSupervisionStrategy(decider))(system)

  abstract class Fixture {
    implicit val self = testActor
    def createTestStream(bufferSize: Int): (Queue[Elem], TestSubscriber.Probe[Elem]) =
      Source.fromGraph(new PriorityBufferStage[Elem](bufferSize, _.priority))
        .toMat(TestSink.probe)(Keep.both).run()
  }

  "PriorityBufferActor" should {
    "prioritize elements" in new Fixture {
      val (queue, sub) = createTestStream(bufferSize=5)
      queue offer Elem(FIRST, 1)
      queue offer Elem(SECOND, 0)
      queue offer Elem(THIRD, 2)
      queue offer Elem(FOURTH, 0)
      queue offer Elem(FIFTH, 1)

      sub.request(1)
      sub.expectNext() shouldEqual Elem(THIRD, 2)
      sub.expectNoMsg()
      sub.request(2)
      sub.expectNext() shouldEqual Elem(FIRST, 1)
      sub.expectNext() shouldEqual Elem(FIFTH, 1)
      sub.expectNoMsg()
      sub.request(2)
      sub.expectNext() shouldEqual Elem(SECOND, 0)
      sub.expectNext() shouldEqual Elem(FOURTH, 0)
      sub.expectNoMsg()
    }

    "skip less prir elements" in new Fixture {
      val (queue, sub) = createTestStream(bufferSize=3)
      queue offer Elem(FIRST, 1)
      queue offer Elem(SECOND, 0)
      queue offer Elem(THIRD, 1)
      queue offer Elem(FOURTH, 1)
      queue offer Elem(FIFTH, 1)

      sub.request(5)
      sub.expectNext() shouldEqual Elem(FIRST, 1)
      sub.expectNext() shouldEqual Elem(THIRD, 1)
      sub.expectNext() shouldEqual Elem(FOURTH, 1)
      sub.expectNoMsg()
    }

    "skip all elements" in new Fixture {
      val (queue, sub) = createTestStream(bufferSize=3)
      queue offer Elem(FIRST, 1)
      queue offer Elem(SECOND, 1)
      queue offer Elem(THIRD, 1)
      queue offer Elem(FOURTH, 1)
      queue offer Elem(FIFTH, 1)

      sub.request(5)
      sub.expectNext() shouldEqual Elem(FIRST, 1)
      sub.expectNext() shouldEqual Elem(SECOND, 1)
      sub.expectNext() shouldEqual Elem(THIRD, 1)
      sub.expectNoMsg()
    }

    "select available low-priority task for drop" in new Fixture {
      val (queue, sub) = createTestStream(bufferSize=3)
      queue offer Elem(FIRST, 1)
      queue offer Elem(SECOND, 2)
      queue offer Elem(THIRD, 2)
      queue offer Elem(FOURTH, 2)
      queue offer Elem(FIFTH, 5)

      sub.request(3)
      sub.expectNext() shouldEqual Elem(FIFTH, 5)
      sub.expectNext() shouldEqual Elem(SECOND, 2)
      sub.expectNext() shouldEqual Elem(THIRD, 2)
      sub.expectNoMsg()
    }

    "satisfy fast consume rate" in new Fixture {
      val (queue, sub) = createTestStream(bufferSize=3)

      sub.request(2)
      queue offer Elem(FIRST, 1)
      queue offer Elem(SECOND, 2)
      queue offer Elem(THIRD, 3)

      sub.expectNext() shouldEqual Elem(FIRST, 1)
      sub.expectNext() shouldEqual Elem(SECOND, 2)
      sub.expectNoMsg()

      sub.request(2)
      queue offer Elem(FOURTH, 4)
      sub.expectNext() shouldEqual Elem(THIRD, 3)
      sub.expectNext() shouldEqual Elem(FOURTH, 4)
      sub.expectNoMsg()
    }
  }

  "PriorityQueuePreserveOrder" should {
    def asList[T](pq: PriorityQueuePreserveOrder[T]): List[T] = {
      val buffer = collection.mutable.Buffer.empty[T]
      while (pq.nonEmpty()) buffer += pq.pollHighPriority()
      buffer.toList
    }

    "remove new priority group" in {
      val pq = new PriorityQueuePreserveOrder[Elem](3, _.priority)
      pq.add(Elem(FIRST, 0))
      pq.add(Elem(SECOND, 0))
      pq.add(Elem(THIRD, 1))

      asList(pq) shouldEqual List(Elem(THIRD, 1), Elem(FIRST, 0), Elem(SECOND, 0))
      pq.nonEmpty() shouldBe false

      pq.add(Elem(FOURTH, 3))
      pq.nonEmpty() shouldBe true
      pq.pollHighPriority() shouldEqual Elem(FOURTH, 3)
      pq.nonEmpty() shouldBe false
    }

    "preserve order" in {
      val pq = new PriorityQueuePreserveOrder[Elem](5, _.priority)
      pq.add(Elem(FIRST, 1))
      pq.add(Elem(SECOND, 0))
      pq.add(Elem(THIRD, 1))
      pq.add(Elem(FOURTH, 0))
      pq.add(Elem(FIFTH, 1))

      asList(pq) shouldEqual
        List(Elem(FIRST, 1), Elem(THIRD, 1), Elem(FIFTH, 1), Elem(SECOND, 0), Elem(FOURTH, 0))
    }

    "poll highest priority element" in {
      val pq = new PriorityQueuePreserveOrder[Elem](5, _.priority)
      pq.add(Elem(FIRST, 1))
      pq.add(Elem(SECOND, 0))
      pq.add(Elem(THIRD, 3))
      pq.add(Elem(FOURTH, 2))
      pq.add(Elem(FIFTH, 1))

      asList(pq) shouldEqual
        List(Elem(THIRD, 3), Elem(FOURTH, 2), Elem(FIRST, 1), Elem(FIFTH, 1), Elem(SECOND, 0))
    }

    "displace low with high priority element" in {
      val pq = new PriorityQueuePreserveOrder[Elem](3, _.priority)
      pq.add(Elem(FIRST, 0))  shouldBe true
      pq.add(Elem(SECOND, 1)) shouldBe true
      pq.add(Elem(THIRD, 0))  shouldBe true
      pq.add(Elem(FOURTH, 2)) shouldBe true
      pq.add(Elem(FIFTH, 2))  shouldBe true

      asList(pq) shouldEqual
        List(Elem(FOURTH, 2), Elem(FIFTH, 2), Elem(SECOND, 1))
    }

    "dispace newer element" in {
      val pq = new PriorityQueuePreserveOrder[Elem](3, _.priority)
      pq.add(Elem(FIRST, 1))  shouldBe true
      pq.add(Elem(SECOND, 1)) shouldBe true
      pq.add(Elem(THIRD, 2))  shouldBe true
      pq.add(Elem(FOURTH, 2)) shouldBe true

      asList(pq) shouldEqual
        List(Elem(THIRD, 2), Elem(FOURTH, 2), Elem(FIRST, 1))
    }

    "skip elements" in {
      val pq = new PriorityQueuePreserveOrder[Elem](3, _.priority)
      pq.add(Elem(FIRST, 1))  shouldBe true
      pq.add(Elem(SECOND, 2)) shouldBe true
      pq.add(Elem(THIRD, 2))  shouldBe true
      pq.add(Elem(FOURTH, 2)) shouldBe true
      pq.add(Elem(FIFTH, 2))  shouldBe false

      asList(pq) shouldEqual
        List(Elem(SECOND, 2), Elem(THIRD, 2), Elem(FOURTH, 2))
    }

    // long test ~2 min
    "run in linear time" ignore {
      def bench(block: ⇒ Any): (Int, Double) = {
        val start = System.nanoTime
        val ref = block
        val time = ((System.nanoTime - start) / 1000000.0D).toInt
        System.gc()
        val mem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024).toDouble
        time → mem
      }

      val Ns = 100000 :: 1000000 :: 5000000 :: 10000000 :: Nil
      val results = Ns.map { n ⇒
        val pq = new PriorityQueuePreserveOrder[Elem](n, _.priority)
        val dryRun = bench {
          (0 until n).map { i ⇒
            Elem(i.toString, 1)
          }.toArray
        }
        val insert = bench {
          (0 until n).foreach { i ⇒
            pq.add(Elem(i.toString, 0))
          }
        }
        val priorityInsert = bench {
          (0 until n).foreach { i ⇒
            pq.add(Elem(i.toString, 1))
          }
        }
        val pollPush = bench {
          (0 until n).foreach { i ⇒
            pq.pollHighPriority()
            pq.add(Elem(i.toString, 0))
            pq.add(Elem(i.toString, 1))
          }
        }
        val poolAll = bench {
          (0 until n).foreach { i ⇒
            pq.pollHighPriority()
          }
        }
        pq.nonEmpty() shouldBe false
        val manyPriorities = bench {
          (0 until n).foreach { i ⇒
            val priority = i % 100
            pq.add(Elem(i.toString, priority))
          }
        }
        val poolAllDifferentPriorities = bench {
          (0 until n).foreach { i ⇒
            pq.pollHighPriority()
          }
        }
        pq.nonEmpty() shouldBe false
        Map(
          "1) dryRun" → dryRun,
          "2) insert" → insert,
          "3) priorityInsert" → priorityInsert,
          "4) pollPush" → pollPush,
          "5) poolAll" → poolAll,
          "6) manyPriorities" → manyPriorities,
          "7) poolAllDifferentPriorities" → poolAllDifferentPriorities)
      }
      results.flatMap(_.toSeq).groupBy(_._1).toSeq.sortBy(_._1).foreach {
        case (name, values) ⇒
          info(name)
          var prevTime: Option[Int] = None
          var prevMem: Option[Double] = None
          info(values.zip(Ns).map { case ((_, (time, mem)), n) ⇒
            val s =
              if (prevTime.isDefined) {
                val (Some(pTime), Some(pMem)) = (prevTime, prevMem)
                val timeRatio = time / pTime.toDouble
                val memRatio = mem / pMem
                f"$n%10d:\t$time%5d ms (x$timeRatio%.2f)\t$mem%.2f Mb (x$memRatio%.2f)"
              } else f"$n%10d:\t$time%5d ms\t\t\t$mem%.2f Mb"
            prevMem = Some(mem)
            prevTime = Some(time)
            s
          }.mkString("\n"))
      }
    }
  }
}
