package ru.yandex.vertis.picapica.utils

import java.util.concurrent.{Future, Executors, CompletableFuture}

import akka.actor.{ActorSystem, Scheduler}
import akka.testkit.TestKit
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
/**
 * @author evans
 */
@RunWith(classOf[JUnitRunner])
class WithinFutureAdapterSpec(system: ActorSystem)
  extends TestKit(system)
  with WordSpecLike
  with Matchers {

  def this() = this(ActorSystem.create("MySpec"))

  val withinHelper: WithinFuture = new WithinFuture {
    override protected def executionContext: ExecutionContext = system.dispatcher

    override protected def scheduler: Scheduler = system.scheduler
  }

  "within cassandra storage" should {
    "completes for successful requests" in {
      val f = withinHelper.withinJava[Int, Future[Int]](50.millis) {
        CompletableFuture.completedFuture(1)
      }
      f.get() shouldEqual 1
    }

    "completes for infinite requests" in {
      val executor = Executors.newSingleThreadExecutor()
      val f = executor.submit(new Runnable {
        override def run(): Unit = Thread.sleep(250)
      }).asInstanceOf[Future[Unit]]
      val g = withinHelper.withinJava[Unit, Future[Unit]](50.millis)(f)
      Thread.sleep(100)
      g.isCancelled shouldBe true
    }

    "completes for failed requests" in {

    }
  }
}
