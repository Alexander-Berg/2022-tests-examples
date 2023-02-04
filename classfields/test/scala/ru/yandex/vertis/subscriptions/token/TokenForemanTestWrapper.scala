package ru.yandex.vertis.subscriptions.token

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ActorSelection, ActorSystem}
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import ru.yandex.common.tokenization.{IntTokens, Owner, Ownerships, OwnershipsKeeper, Token, Tokens, TokensDistributor}
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TokenForemanTestWrapper(
    ownerProperties: (String, String),
    totalForemenInstances: Int,
    val tokens: Tokens = new IntTokens(70),
    zkServer: TestingServer = new TestingServer()) {

  implicit val system: ActorSystem = ActorSystem("TokenForemanWrapperSystem-" + BasicGenerators.readableString.next)

  lazy val ownershipsKeeperActor = TestActorRef(new OwnershipsKeeper(localCurator, tokens))

  lazy val tokenForeman = TestActorRef(
    new TokenForeman(
      tokens,
      ownershipsKeeperActor,
      owner,
      token2foreman,
      TokenForeman.Config(
        totalForemen = totalForemenInstances,
        ownerProperties._1,
        TokensDistributor.Config().copy(distributePeriod = 100.millis, subscribePeriod = 5.minutes)
      )
    ) {

      override def updateGlobalForemenMap(ownerships: Ownerships): Unit = {
        super.updateGlobalForemenMap(ownerships)
        recalcCurrentForemenState()
      }
    }
  )

  lazy val tokenForemanActorSelection: ActorSelection = system.actorSelection(tokenForeman.path)

  val localCurator: CuratorFramework = CuratorFrameworkFactory.builder
    .connectString(zkServer.getConnectString)
    .retryPolicy(new RetryOneTime(1))
    .build

  {
    localCurator.start()
    localCurator.blockUntilConnected()
    localCurator.usingNamespace("ownerships")
  }

  val answerTimeWait: FiniteDuration = 15.seconds

  val owner: Owner = Owner(BasicGenerators.readableString.next, Map(ownerProperties))

  val token2foreman = new AtomicReference(Map.empty[Token, ActorSelection])

  val foreman2token = new AtomicReference(Map.empty[ActorSelection, Set[Token]])

  /** Formula used in TokenForemen */
  val maxTokens: Int = tokens.toSet.size / (totalForemenInstances - 1) + 1

  implicit val timeout: Timeout = Timeout(answerTimeWait)

  def printForemanToTokensSize(): Unit = {
    recalcCurrentForemenState()
    foreman2token
      .get()
      .keySet
      .foreach(key =>
        println(s"$key - total:${foreman2token.get().get(key).map(_.size)}, Tokens: ${foreman2token.get().get(key)}")
      )
  }

  def recalcCurrentForemenState(): Unit =
    foreman2token.set(
      token2foreman.get.foldLeft(Map.empty[ActorSelection, Set[Token]]) {
        case (map, (token, actor)) =>
          map.updated(actor, map.get(actor) match {
            case None => Set.empty[Token] + token
            case Some(x) => x + token
          })
      }
    )
}
