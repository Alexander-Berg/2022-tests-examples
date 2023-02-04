package ru.yandex.common.tokenization

import akka.actor._
import akka.testkit.{TestActorRef, TestKitBase, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.ZooKeeperAware

import scala.concurrent.duration._

/**
  * Specs on [[OwnershipsKeeper]]
  */
trait TokensDistributorSpecBase
    extends Matchers
        with TestKitBase
        with WordSpecLike
        with ZooKeeperAware {

  def tokens: Tokens = new IntTokens(32)

  def ownerConfig(id: Int): OwnerConfig

  val curator = curatorBase.usingNamespace("tokens-distributor-spec")

  def newTokenDistributor(ownerConfig: OwnerConfig): TokensDistributor

  override protected def afterAll() = {
    shutdown(system)
    super.afterAll()
  }

  "TokensDistributor" should {
    val client1 = TestActorRef(new TokensCollector, "collector-1")
    val client2 = TestActorRef(new TokensCollector, "collector-2")
    val client3 = TestActorRef(new TokensCollector, "collector-3")
    val client4 = TestActorRef(new TokensCollector, "collector-4")
    val probe1 = TestProbe()
    val probe2 = TestProbe()
    val probe3 = TestProbe()
    val probe4 = TestProbe()

    var distributor1: Option[ActorRef] = None
    var distributor2: Option[ActorRef] = None
    var distributor3: Option[ActorRef] = None
    var distributor4: Option[ActorRef] = None

    "distribute tokens among 3 owners from scratch" in {
      distributor1 = tokensDistributor(ownerConfig(1), client1, probe1.ref)
      distributor2 = tokensDistributor(ownerConfig(2), client2, probe2.ref)
      distributor3 = tokensDistributor(ownerConfig(3), client3, probe3.ref)

      expectAllDistributed(probe1)
      expectAllDistributed(probe2)
      expectAllDistributed(probe3)

      checkDistribution(getTokens(client1), getTokens(client2), getTokens(client3), tokens.toSet)
    }

    "redistribute tokens after one owner shut down" ignore {
      stop(distributor3)

      expectRedistribution(probe1)
      expectRedistribution(probe2)

      expectDistributionViolation(probe1)
      expectDistributionViolation(probe2)

      expectAllDistributed(probe1)
      expectAllDistributed(probe2)

      checkDistribution(getTokens(client1), getTokens(client2), tokens.toSet)
    }

    "redistribute tokens after new owner arrives" in {
      distributor4 = tokensDistributor(ownerConfig(4), client4, probe4.ref)

      expectRedistribution(probe1)
      expectRedistribution(probe2)

      expectAllDistributed(probe1)
      expectAllDistributed(probe2)
      expectAllDistributed(probe4)

      checkDistribution(getTokens(client1), getTokens(client2), getTokens(client4), tokens.toSet)
    }

    "distribute tokens among single owner when others tear down" in {
      stop(distributor2)
      stop(distributor4)

      expectRedistribution(probe1)
      expectDistributionViolation(probe1)
      expectAllDistributed(probe1)

      getTokens(client1) should be(tokens.toSet)
    }
  }

  private def expectAllDistributed(probe: TestProbe) {
    probe.fishForMessage(tokens.toSet.size.seconds, "AllDistributed") {
      case TokensDistributor.Notification.Distributed => true
      case _ => false
    }
  }

  private def expectDistributionViolation(probe: TestProbe) {
    probe.fishForMessage(tokens.toSet.size.seconds, "DistributionViolation") {
      case TokensDistributor.Notification.DistributionViolation => true
      case e => false
    }
  }

  private def expectRedistribution(probe: TestProbe) {
    probe.fishForMessage(tokens.toSet.size.seconds, "Redistribution") {
      case TokensDistributor.Notification.Redistributing => true
      case _ => false
    }
  }

  private def checkDistribution(ts1: Set[Token], ts2: Set[Token], expect: Set[Token]) {
    ts1.size + ts2.size should be(expect.size)
    ts1 ++ ts2 should be(expect)
    ts1 intersect ts2 should be(empty)
  }

  private def checkDistribution(ts1: Set[Token], ts2: Set[Token], ts3: Set[Token], expect: Set[Token]) {
    checkDistribution(ts1, ts2, expect -- ts3)
    checkDistribution(ts1, ts3, expect -- ts2)
    checkDistribution(ts2, ts3, expect -- ts1)
  }

  private def stop(optRef: Option[ActorRef]) {
    optRef.foreach(ref => system.stop(ref))
  }

  private def tokensDistributor(ownerConfig: OwnerConfig, listeners: ActorRef*) = {
    val props = Props(newTokenDistributor(ownerConfig))
    val result = system.actorOf(props, s"distributor-${ownerConfig.owner.id}")
    for (listener <- listeners)
      result.tell(TokensDistributor.Subscribe, listener)
    Some(result)
  }

  private def getTokens(client: TestActorRef[TokensCollector]) =
    client.underlyingActor.tokens
}
