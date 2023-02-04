package ru.yandex.vertis.clustering.services.impl

import java.time.temporal.ChronoUnit
import java.util.concurrent.{CountDownLatch, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.curator.test.TestingServer
import org.junit.runner.RunWith
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.ZookeeperConfig
import ru.yandex.vertis.clustering.model.Domains
import ru.yandex.vertis.clustering.services.NodeDecider._
import ru.yandex.vertis.clustering.utils.{DateTimeUtils, ZookeeperUtils}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class NodesZkClientImplSpec extends BaseSpec {

  val testingServer = new TestingServer()
  testingServer.start()

  override def afterAll() {
    testingServer.stop()
  }

  val zookeeperConfig: ZookeeperConfig = new ZookeeperConfig {
    override def namespace: String = "user-clustering-testing"
    override def connectString: String = s"localhost:${testingServer.getPort}"
  }

  val zkUtils: ZookeeperUtils =
    new ZookeeperUtils(zookeeperConfig, "clustering")

  val alice = "alice"
  val bob = "bob"
  val charlie = "charlie"

  val service = new NodesZkClientImpl(zkUtils, Domains.Autoru, acquireTimeout = 5.seconds)
  val agedService = new NodesZkClientImpl(zkUtils, Domains.Autoru, acquireTimeout = 20.seconds)

  val now = Some(DateTimeUtils.now)
  val late = Some(DateTimeUtils.now.minusHours(1))
  val old = Some(DateTimeUtils.now.minusHours(5))

  val aliceReadyNode = Node(alice, now, old)
  val bobLateNode = Node(bob, late, old)
  val charlieLateNode = Node(charlie, late, late)

  private val centThreads = ExecutionContext.fromExecutor(
    Executors.newScheduledThreadPool(100,
                                     new ThreadFactoryBuilder()
                                       .setNameFormat("LockSpec-%d")
                                       .build()))

  "NodesZkClientImpl" should {
    "correctly set states" in {

      service.set(aliceReadyNode)
      service.set(bobLateNode)
      service.set(charlieLateNode)

      Thread.sleep(1000)

      service.nodes().map(_.toSet) shouldBe
        Success(Set(aliceReadyNode, bobLateNode, charlieLateNode))
    }

    "provide interprocess lock" in {
      Future {
        service.withLock("node01") {
          Thread.sleep(3000)
        }
      }(scala.concurrent.ExecutionContext.global)

      val before = DateTimeUtils.now
      service.withLock("node02") {
        ChronoUnit.MILLIS.between(before, DateTimeUtils.now) should be >= 2000L
      }
    }

    "forbid lock acquiring when it already locked" in {
      Future {
        service.withLock("node03") {
          Thread.sleep(10000)
        }
      }(scala.concurrent.ExecutionContext.global)

      Thread.sleep(1000)
      service.withLock("node04") {
        true
      } shouldBe a[Failure[_]]
    }

    "reentrant into withLock in the same thread" in {

      agedService
        .withLock("node01") {
          agedService
            .withLock("node02") {
              true
            }
            .get
        }
        .get shouldBe true

    }

    "pass if use lockWith" in {

      (0 to 2).foreach { c =>
        val latch = new CountDownLatch(100)

        var s = ""

        val fs = (0 to 99).map { i =>
          Future {
            latch.countDown()
            latch.await()
            agedService
              .withLock(s"node${i % 10}") {
                s = s + c
              }
              .get
          }(centThreads)
        }

        Future.sequence(fs).futureValue(Timeout(20.seconds))

        s should be(Iterator.continually(c).take(100).mkString)
      }

      val array = new Array[String](10)

      val latch = new CountDownLatch(100)

      val fs = (0 to 99).map { i =>
        Future {
          val index = i % 10
          latch.countDown()
          latch.await()
          agedService
            .withLock(s"node${i % 10}") {
              array(index) = Option(array(index)).getOrElse("") + "b"
            }
            .get
        }(centThreads)
      }

      Future.sequence(fs).futureValue(Timeout(20.seconds))

      array.foreach(_.length should be(10))

    }
  }

}
