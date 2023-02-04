package ru.yandex.vertis.billing.banker.actor

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestActorRef}
import ru.yandex.vertis.billing.banker.actor.EffectActorProtocol.Request

import scala.concurrent.Future

/**
  * Runnable specs on [[RespondingEffectActor]]
  *
  * @author alex-kovalenko
  */
class EffectActorSpec extends ActorSpecBase("EffectActor") with ImplicitSender {

  val actor = TestActorRef(Props {
    new EffectActorSpec.TestActorImpl with RespondingEffectActor
  })

  "EffectActor" should {
    "not respond when not asked" in {
      actor ! EffectActorSpec.TestActor.Rq(false)
      expectNoMessage()
    }

    "respond when asked" in {
      actor ! EffectActorSpec.TestActor.Rq(true)
      expectMsg(EffectActorProtocol.Done)
    }
  }
}

object EffectActorSpec {

  class TestActorImpl extends ProcessRequestActor {

    override protected def process(request: Request): Future[Unit] =
      Future.successful(())
  }

  object TestActor {
    case class Rq(expectResponse: Boolean) extends Request
  }
}
