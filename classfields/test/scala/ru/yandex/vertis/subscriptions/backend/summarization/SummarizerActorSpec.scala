package ru.yandex.vertis.subscriptions.backend.summarization

import akka.actor.{ActorRef, ActorSystem, LoggingFSM, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.backend.summarization.SummarizerActor.Notification.Summarized
import ru.yandex.vertis.subscriptions.storage.summarization.Summary

import scala.concurrent.duration._

/** Specs on [[SummarizerActor]]
  */
@RunWith(classOf[JUnitRunner])
class SummarizerActorSpec
  extends TestKit(ActorSystem("unit-test", ConfigFactory.empty()))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  val preserveLastN = 3

  val config = SummaryConfig(
    collectFor = 1.seconds,
    waitNextDataFor = 100.milliseconds,
    maxWaitNextDataFor = 500.milliseconds
  )

  "SummarizedActor" should {
    "collect data for full collectFor duration" in {
      val client = TestProbe()

      val originalSummary = Summary.emptySinceNow[Int](preserveLastN)

      val summarizerActor =
        system.actorOf(summarizerActorProps(config, client.ref, originalSummary), "SummarizerActor-1")

      summarizerActor ! SummarizerActor.Command.Include(1)
      client.expectNoMessage(config.waitNextDataFor)
      client.expectMsgPF(config.collectFor) {
        case Summarized(summary: Summary[Int]) =>
          summary.count should be(1)
          summary.lastN should be(Iterable(1))
      }

      summarizerActor ! SummarizerActor.Command.ShowSummary
      expectMsgPF() {
        case summary: Summary[Int] =>
          summary should be(empty)
          summary.since shouldBe >(originalSummary.since)
      }
    }

    "collect data for rest of summarizeFor duration" in {
      val client = TestProbe()

      val summary =
        Summary.emptySince[Int](System.currentTimeMillis() - config.collectFor.toMillis / 2, 3).updated(1).updated(2)

      val summarizerActor =
        system.actorOf(Props(new SummarizerActor[Int](config, client.ref, summary)), "SummarizerActor-2")

      summarizerActor ! SummarizerActor.Command.Include(3)
      client.expectMsgPF(config.collectFor / 2 + 50.milliseconds) {
        case Summarized(summary: Summary[Int]) =>
          summary.count should be(3)
          summary.lastN should be(Seq(3, 2, 1))
      }
    }

    "correct play extended scenario" in {
      val client = TestProbe()
      val summary = Summary.emptySinceNow[Int](3)

      val summarizerActor =
        system.actorOf(Props(new SummarizerActor[Int](config, client.ref, summary)), "SummarizerActor-3")

      for (i <- 1 to 100)
        summarizerActor ! SummarizerActor.Command.Include(i)

      client.expectNoMessage(config.waitNextDataFor)

      client.expectMsgPF(config.collectFor) {
        case Summarized(summary: Summary[Int]) =>
          summary.count should be(100)
          summary.lastN should be(Seq(100, 99, 98))
      }

      summarizerActor ! SummarizerActor.Command.Include(101)

      client.expectNoMessage(config.collectFor)

      client.expectMsgPF() {
        case Summarized(summary: Summary[Int]) =>
          summary.count should be(1)
          summary.lastN should be(Seq(101))
      }

      for (i <- 102 to 200)
        summarizerActor ! SummarizerActor.Command.Include(i)

      client.expectNoMessage(config.collectFor)

      client.expectMsgPF() {
        case Summarized(summary: Summary[Int]) =>
          summary.count should be(99)
          summary.lastN should be(Seq(200, 199, 198))
      }

      client.expectNoMessage(config.collectFor)
    }
  }

  override protected def afterAll() = {
    shutdown(system)
  }

  private def summarizerActorProps(config: SummaryConfig, client: ActorRef, summary: Summary[Int]): Props = {
    Props(new SummarizerActor[Int](config, client, summary) with LoggingFSM[SummarizerActor.State, Summary[Int]] {
      override def logDepth = 10
    })
  }
}
