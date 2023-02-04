package ru.yandex.common.tokenization.cost

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import ru.yandex.common.tokenization.TokensDistributor.Config
import ru.yandex.common.tokenization.{Owner, OwnerConfig, _}

import scala.concurrent.duration._

/**
  * @author evans
  */
class DcCostTokensDistributorIntSpec
    extends TestKit(ActorSystem("unit-test",
      ConfigFactory.parseString("akka.actor.debug.fsm=true").
          withFallback(ConfigFactory.defaultReference())))
        with TokensDistributorSpecBase {

  override val tokens: Tokens = new IntTokens(32)

  override def newTokenDistributor(ownerConfig: OwnerConfig): TokensDistributor =
    new TokensDistributor(
      ownerConfig,
      tokens,
      curator,
      Config(
        distributePeriod = 100.milliseconds,
        redistributionStateTimeout = 200.milliseconds,
        nearlyDistributedTimeout = 300.milliseconds
      )
    )

  override def ownerConfig(id: Int) = {
    def cost(token: Token) =
      if (token.toInt % 4 == id) 1 else 1000

    OwnerConfig(Owner(id.toString), cost)
  }
}
