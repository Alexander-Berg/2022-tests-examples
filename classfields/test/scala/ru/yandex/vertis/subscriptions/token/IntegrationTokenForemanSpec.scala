package ru.yandex.vertis.subscriptions.token

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import org.apache.curator.test.TestingServer
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.{IntTokens, Token}
import ru.yandex.vertis.subscriptions.ActorSpecBase
import ru.yandex.vertis.subscriptions.token.TokenForeman.{Notification, Subscribe}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.Random

/**
  * Local Test on Token Foreman
  */
@RunWith(classOf[JUnitRunner])
class IntegrationTokenForemanSpec(_system: ActorSystem) extends ActorSpecBase(_system) {

  val totalTokensCount = 70

  val tokens = new IntTokens(totalTokensCount)

  val zkServer = new TestingServer()

  def this() = this(ActorSystem("TokenForemanSpecSystem"))

  def checkAllDistributed(tokenForemanTestWrappers: ListBuffer[TokenForemanTestWrapper]): Unit = {
    tokenForemanTestWrappers.foreach { tfw =>
      tfw.tokenForeman.underlyingActor.tokensAcquired.size should be <= tfw.maxTokens
      if (tokenForemanTestWrappers.size >= totalTokensCount / tfw.maxTokens)
        tfw.foreman2token.get().values.foldLeft(0)((sum, set) => sum + set.size) shouldBe totalTokensCount
    }
  }

  def addTestTokenForemenToEnv(
      tokenForemanTestWrappers: ListBuffer[TokenForemanTestWrapper],
      testProbe: TestProbe,
      countToInstantiate: Int = 1,
      totalForemenCount: Int,
      zkServer: TestingServer = zkServer) = {
    (1 to countToInstantiate).foreach { _ =>
      val tokenForemanWrapper = new TokenForemanTestWrapper(
        ("notifier-actor", s"test_${Random.nextInt}"),
        totalForemenCount,
        tokens = tokens,
        zkServer = zkServer
      )
      testProbe.send(tokenForemanWrapper.tokenForeman, TokenForeman.Subscribe.NotifyDistributed)
      tokenForemanTestWrappers += tokenForemanWrapper
    }
  }

  def removeTestTokenForemenFromEnv(tokenForemanTestWrappers: ListBuffer[TokenForemanTestWrapper], nums: Int*) = {
    nums.map { num =>
      tokenForemanTestWrappers(num).tokenForeman.stop()
      tokenForemanTestWrappers(num).ownershipsKeeperActor.stop()
      tokenForemanTestWrappers -= tokenForemanTestWrappers(num)
    }
  }

  "TokenForeman" should {
    def localMainForemanWrapper(actor: ActorRef) = TestActorRef(
      new Actor {
        var internalTokenSet = Set.empty[Token]

        def receive = {
          case Notification.Acquired(setAcquired) => internalTokenSet ++= setAcquired
          case Notification.Released(setReleased) => internalTokenSet --= setReleased
        }

        def subscribe = {
          actor ! Subscribe.LocalTokenCommands
        }
      }
    )
    val testProbe = TestProbe("Environment")
    val testForemen = new ListBuffer[TokenForemanTestWrapper]
    addTestTokenForemenToEnv(testForemen, testProbe, 4, 4)
    val localMainSub = localMainForemanWrapper(testForemen.head.tokenForeman)
    localMainSub.underlyingActor.subscribe

    "redistribute tokens if additional instances added" in {
      testForemen.foreach(wrapper => testProbe.send(wrapper.tokenForeman, TokenForeman.Subscribe.NotifyDistributed))
      testProbe.receiveN(4, 15.seconds)
      checkAllDistributed(testForemen)
      localMainSub.underlyingActor.internalTokenSet should
        contain theSameElementsAs testForemen.head.tokenForeman.underlyingActor.tokensAcquired
    }
    "correctly redistribute if one crashed" in {
      removeTestTokenForemenFromEnv(testForemen, 2)
      testForemen.foreach(wrapper => testProbe.send(wrapper.tokenForeman, TokenForeman.Subscribe.NotifyDistributed))
      testProbe.receiveN(3, 15.seconds)
      checkAllDistributed(testForemen)
      localMainSub.underlyingActor.internalTokenSet should
        contain theSameElementsAs testForemen.head.tokenForeman.underlyingActor.tokensAcquired
    }
    "correctly redistribute if added back" in {
      addTestTokenForemenToEnv(testForemen, testProbe, 1, 4)
      testForemen.foreach(wrapper => testProbe.send(wrapper.tokenForeman, TokenForeman.Subscribe.NotifyDistributed))
      testProbe.receiveN(4, 15.seconds)
      checkAllDistributed(testForemen)
      localMainSub.underlyingActor.internalTokenSet should
        contain theSameElementsAs testForemen.head.tokenForeman.underlyingActor.tokensAcquired
    }
    "correctly work when  only two actors available" in {
      val zkServer = new TestingServer()
      val testProbe = TestProbe("Environment")
      val testForemen = new ListBuffer[TokenForemanTestWrapper]
      addTestTokenForemenToEnv(testForemen, testProbe, 2, 2, zkServer)
      testForemen.foreach(wrapper => testProbe.send(wrapper.tokenForeman, TokenForeman.Subscribe.NotifyDistributed))
      testProbe.receiveN(2, 15.seconds)
      testForemen.foreach(x => println(x.tokenForeman.underlyingActor.tokensAcquired))
      testForemen.foreach(x => println(x.maxTokens))
      testForemen
        .map(_.tokenForeman.underlyingActor.tokensAcquired)
        .foldLeft(0)((sum, set) => sum + set.size) shouldBe totalTokensCount
    }
  }
}
