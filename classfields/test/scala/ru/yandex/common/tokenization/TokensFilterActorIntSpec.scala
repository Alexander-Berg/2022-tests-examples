package ru.yandex.common.tokenization

import akka.actor.{ActorSystem, Props, Terminated}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Ignore, Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.common.ZooKeeperAware
import ru.yandex.common.tokenization.TokensDistributor.Config

import scala.concurrent.duration._


object TokensFilterActorIntSpec {
  val config = ConfigFactory.parseString(
    """
      |akka.actor.one-place-mailbox {
      |  mailbox-type = "akka.dispatch.BoundedMailbox"
      |  mailbox-capacity = 1
      |  mailbox-push-timeout-time = 1m
      |}
    """.stripMargin
  )
}

/**
 * Specs on [[ru.yandex.common.tokenization.TokensFilterActor]]
 */
class TokensFilterActorIntSpec
  extends TestKit(ActorSystem("unit-test", TokensFilterActorIntSpec.config))
  with Matchers
  with WordSpecLike
  with ZooKeeperAware {

  val log = LoggerFactory.getLogger(getClass)

  val tokens = new IntTokens(32)

  val curator = curatorBase.usingNamespace("tokens-filter-actor-spec")

  val distributionConfig = Config(
    distributePeriod = 100.milliseconds,
    redistributionStateTimeout = 300.milliseconds,
    nearlyDistributedTimeout = 300.milliseconds
  )

  def blockedFilter(name: String, id: String, receiver: TokensFilterActor.Receiver,
                    salt: String) = {
    val props = Props(
      new BlockingTokensFilterActor(
        name,
        id,
        tokens,
        curator.usingNamespace(s"tokens-filter-actor-spec-$name-$salt"),
        distributionConfig,
        receiver
      )
    )
    TestActorRef[BlockingTokensFilterActor](
      props.withMailbox("akka.actor.one-place-mailbox"),
      s"tokens-filter-$name-$id-$salt")
  }

  override protected def afterAll() = {
    shutdown(system)
    super.afterAll()
  }

  "BlockedTokensFilterActor" should {
    "pass messages through one filter" in {
      testWithNFilters(n = 1, nrOfStop = 0, nrOfMessages = 100)
    }
    "pass messages through two filter" in {
      testWithNFilters(n = 2, nrOfStop = 1, nrOfMessages = 100)
    }

    "pass messages through four filter" in {
      testWithNFilters(n = 4, nrOfStop = 2, nrOfMessages = 100)
    }
  }

  def testWithNFilters(n: Int, nrOfStop: Int, nrOfMessages: Int) {
    val probe = TestProbe()
    val receiver = TokensFilterActor.ActorReceiver(probe.ref)

    val filters = for (id <- 1 to n) yield {
      val f = blockedFilter("foo", id.toString, receiver, n.toString)
      watch(f)
      f
    }

    val messages = 1 to nrOfMessages
    for (msg <- messages) {
      for (filter <- filters)
        filter ! msg
      probe.expectMsg(10.seconds, msg)
    }

    if (nrOfStop > 0) {
      filters.take(nrOfStop).foreach(f => system.stop(f))

      (1 to nrOfStop) foreach {
        _ =>
          expectMsgPF() {
            case Terminated(ref) => true
          }
      }

      val alive = filters.drop(nrOfStop)

      filters(nrOfStop).underlyingActor.distribution.getTokens

      for (msg <- messages) {
        for (filter <- alive) {
          filter ! msg
        }

        probe.expectMsg(10.seconds, msg)
      }
    }
  }

}
