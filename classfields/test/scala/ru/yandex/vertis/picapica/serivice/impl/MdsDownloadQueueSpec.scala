package ru.yandex.vertis.picapica.serivice.impl

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Matchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import ru.yandex.vertis.picapica.actor.ThrottlingQueue.Entry
import ru.yandex.vertis.picapica.actor.{AsyncAvatarsImageProcessor, ThrottlingQueue}
import ru.yandex.vertis.picapica.dao.AvatarsDao
import ru.yandex.vertis.picapica.generators.Generators
import ru.yandex.vertis.picapica.generators.Producer._
import ru.yandex.vertis.picapica.misc.impl.PerHostQueueImpl
import ru.yandex.vertis.picapica.ops.TestOps
import ru.yandex.vertis.picapica.service.StorageService

import scala.concurrent.Future
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class MdsDownloadQueueSpec extends TestKit(ActorSystem())
    with WordSpecLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with TestOps {

  val mds = mock[AvatarsDao]

  val storage = mock[StorageService]

  override protected def afterEach(): Unit = {
    super.afterEach()
    Mockito.reset(mds, storage)
  }

  trait Ctx {
    val Concurrency = 3
    val Rate = 30

    val queue = new PerHostQueueImpl[Entry](100, 100, Map.empty, 50.millis, Rate)

    val throttler = system.actorOf(Props(new ThrottlingQueue(queue)))

    val processors = Iterable.fill(Concurrency) {
      system.actorOf(Props(new AsyncAvatarsImageProcessor(mds, throttler)))
    }
  }

  "Queue" should {
    "throttle correctly" in new Ctx {
      val Count = 100
      Mockito.when(storage.get(any(), any())).thenReturn(Future.successful(Seq.empty))
      Mockito.when(mds.store(any())(any())).thenReturn(Future.successful(Generators.ActualIdGen.next))

      for (_ <- 1 to 1000) {
        throttler.tell(Generators.ImageTasksRequestGen.next, testActor)
      }

      val expectedDuration = (Count.toDouble / Rate).seconds

      val startTime = System.currentTimeMillis()

      receiveN(100, 2 * expectedDuration)

      (System.currentTimeMillis() - startTime).millis should be > (0.1 * expectedDuration)
    }
    // TODO: write test in another place for it
//    "don't download image if it's already downloaded" in new Ctx {
//      val Count = 3
//      val task = Generators.TaskGen.next
//      val actualId = Generators.ActualIdGen.next
//
//      Mockito.when(storage.get(any(), any())).thenReturn(Future.successful(
//        Seq((task.id, StoredAvatarsData(actualId, Map.empty, None)))))
//
//      for (_ <- 1 to 3) {
//        throttler.tell(task, testActor)
//      }
//      val expectedDuration = (Count.toDouble / Rate).seconds
//      receiveN(Count, 2 * expectedDuration)
//
//      Mockito.verify(mds, Mockito.never()).store(any())(any())
//    }
  }
}
