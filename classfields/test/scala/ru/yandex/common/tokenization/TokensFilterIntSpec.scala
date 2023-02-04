package ru.yandex.common.tokenization

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.common.ZooKeeperAware
import ru.yandex.common.tokenization.TokensDistributor.Config

import scala.concurrent.duration._

/**
 * Specs on [[ru.yandex.common.tokenization.TokensFilterActor]]
 */
class TokensFilterIntSpec
  extends TestKit(ActorSystem("unit-test"))
  with Matchers
  with WordSpecLike
  with ZooKeeperAware {

  val log = LoggerFactory.getLogger(getClass)

  val tokens = new IntTokens(32)

  val curator = curatorBase.usingNamespace("tokens-filter-spec")

  val distributionConfig = Config(
    distributePeriod = 100.milliseconds,
    redistributionStateTimeout = 300.milliseconds,
    nearlyDistributedTimeout = 300.milliseconds
  )

  def blockedFilter(name: String, id: String, salt: String) = new BlockingTokensFilter(
    s"$name-$salt",
    id,
    tokens,
    curator.usingNamespace(s"tokens-filter-spec-$name-$salt"),
    distributionConfig
  )(system)

  override protected def afterAll() = {
    shutdown(system)
    super.afterAll()
  }

  "BlockedTokensFilter" should {
    "filter objects with 1 instance" in {
      testWithNFilters(n = 1, nrOfStop = 0, nrOfMessages = 100)
    }
    "filter objects with 2 instances" in {
      testWithNFilters(n = 2, nrOfStop = 1, nrOfMessages = 100)
    }
    "filter object with 4 instances" in {
      testWithNFilters(n = 4, nrOfStop = 2, nrOfMessages = 100)
    }
  }

  def testWithNFilters(n: Int, nrOfStop: Int, nrOfMessages: Int) {
    val filters = for (id <- 1 to n) yield
      blockedFilter("foo", id.toString, n.toString)

    val messages = 1 to nrOfMessages

    for (msg <- messages)
      filters.count(f => f.isAcceptable(msg)) should be(1)

    if (nrOfStop > 0) {
      filters.take(nrOfStop).foreach(f => f.stop())

      val alive = filters.drop(nrOfStop)

      for (msg <- messages)
        alive.count(f => f.isAcceptable(msg)) should be(1)
    }
  }

}
