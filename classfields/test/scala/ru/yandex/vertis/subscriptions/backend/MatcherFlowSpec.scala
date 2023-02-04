package ru.yandex.vertis.subscriptions.backend

import ru.yandex.common.tokenization
import ru.yandex.common.tokenization.OwnershipsKeeper.Event.Initialized
import ru.yandex.common.tokenization.{IntTokens, Owner, Ownerships, OwnershipsKeeper, Token, TokensDistributor}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.ActorSystemSpecBase
import ru.yandex.vertis.subscriptions.MatcherInternalApi.MatchedSubscriptions
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao
import ru.yandex.vertis.subscriptions.token.TokenForeman
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.{documents, subscriptions}
import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.broker.client.simple.{BrokerClient, TestBrokerClient}
import ru.yandex.vertis.subscriptions.KafkaUtil.NotifierKafkaProducer

import java.util.concurrent.atomic.AtomicReference
import ru.yandex.vertis.subscriptions.backend.util.NopDeduplicator
import ru.yandex.vertis.subscriptions.model.notifier.BrokerEvent

import scala.collection.JavaConversions.asJavaIterable
import scala.concurrent.duration.DurationInt

class MatcherFlowSpec(_system: ActorSystem) extends ActorSystemSpecBase(_system) with MockitoSupport {
  val tokens = new IntTokens(60)

  val token2foreman = new AtomicReference(Map.empty[Token, ActorSelection])
  val token2notifier = new AtomicReference(Map.empty[Token, ActorRef])

  val brokerClient = TestBrokerClient

  val kafkaProducer = new NotifierKafkaProducer[BrokerEvent] {
    override def sendAsync(id: String, message: BrokerEvent): Unit = ()
  }

  val subscriptionsDao = mock[SubscriptionsDao]

  def this() = this(ActorSystem("TestSystem", ConfigFactory.load("application.unit.conf")))

  "Notifier match flow" should {
    val me = Owner("123", Map("test-actor" -> "akka://TestSystem/user/test"))
    val testProbeZk = TestProbe()
    val tokenForemanLocal = TestActorRef(
      new TokenForeman(
        tokens,
        testProbeZk.ref,
        me,
        token2foreman,
        TokenForeman.Config(
          totalForemen = 3,
          "test-actor",
          TokensDistributor.Config().copy(distributePeriod = 100.millis, subscribePeriod = 5.minutes)
        )
      )
    )
    val activeSubsTestProbe = TestProbe()

    val globalMatchesActor = TestActorRef(new GlobalMatchesAcceptorActor(tokens, token2foreman))
    TestActorRef(new LocalMatchesAcceptorActor(tokens, token2notifier, new NopDeduplicator))
    TestActorRef(
      new NotifierTokenForemanSupport(tokens, subscriptionsDao, token2notifier, brokerClient, kafkaProducer) {
        override protected def tokenForeman: ActorRef = tokenForemanLocal

        override protected def activeSubscriptionsDao: ActorRef = activeSubsTestProbe.ref

        override protected def latestDocumentsDao: ActorRef = TestProbe().ref

        override protected def formedNotificationsReceiver: ActorRef = TestProbe().ref
      },
      "test"
    )

    "correctly pass to active subscriptions" in {
      val testSubs = subscriptions.next(4)
      val testTokens = testSubs.map(sub => tokens.token(sub.getId)).toSet
      val ownerships =
        Ownerships(testTokens.map(s => tokenization.Ownership(me, s)), tokens)
      testProbeZk.send(tokenForemanLocal, TokensDistributor.Command.Current(testTokens, ownerships))
      tokenForemanLocal ! OwnershipsKeeper.Notification(Initialized, ownerships)

      testSubs.foreach { sub =>
        globalMatchesActor ! MatchedSubscriptions
          .newBuilder()
          .setMatches(
            MatchedSubscriptions.Matches
              .newBuilder()
              .setDocument(documents.next)
              .addAllSubscriptionIds(Iterable(sub.getId))
          )
          .build()
      }

      activeSubsTestProbe.receiveN(4, 15.seconds)
    }
  }
}
