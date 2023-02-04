package ru.yandex.vertis.billing.banker.tasks

import akka.actor.ActorRef
import akka.pattern.AskTimeoutException
import akka.testkit.TestProbe
import akka.util.Timeout
import ru.yandex.vertis.billing.banker.actor.EffectActorProtocol
import ru.yandex.vertis.billing.banker.tasks.EffectAsyncTaskSpec.{EffectAsyncTaskImpl, RequestImpl}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Failure

/**
  * Runnable spec on [[EffectAsyncTask]]
  *
  * @author alex-kovalenko
  */
class EffectAsyncTaskSpec extends EffectAsyncTaskSpecBase("EffectAsyncTaskSpec") {

  val probe = TestProbe()

  "EffectAsyncTask" should {
    "pass request to actor when prepared" in {
      val task = new EffectAsyncTaskImpl(Future.successful(RequestImpl), probe.ref)
      val future = task.execute()
      probe.expectMsg(RequestImpl)
      probe.reply(EffectActorProtocol.Done)
      Await.result(future, timeout.duration)
    }

    "not pass when fail to prepare request" in {
      val task = new EffectAsyncTaskImpl(Future.failed(new RuntimeException("artificial")), probe.ref)
      val future = task.execute()
      probe.expectNoMessage()
      future.value should matchPattern { case Some(Failure(_: RuntimeException)) =>
      }
    }

    "fail when actor replies with unexpected message" in {
      val task = new EffectAsyncTaskImpl(Future.successful(RequestImpl), probe.ref)
      val future = task.execute()
      probe.expectMsg(RequestImpl)
      probe.reply(new AnyRef)
      Await.ready(future, timeout.duration)
      future.isCompleted shouldBe true
      future.value should matchPattern { case Some(Failure(_: IllegalStateException)) =>
      }
    }

    "fail when actor does not reply" in {
      val task = new EffectAsyncTaskImpl(Future.successful(RequestImpl), probe.ref)
      val future = task.execute()
      probe.expectMsg(RequestImpl)
      Await.ready(future, timeout.duration * 2)
      future.value should matchPattern { case Some(Failure(_: AskTimeoutException)) =>
      }
    }
  }
}

object EffectAsyncTaskSpec {

  trait Request
  object RequestImpl extends Request

  class EffectAsyncTaskImpl(prepare: Future[Request], actor: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
    extends EffectAsyncTask(actor) {
    type Request = EffectAsyncTaskSpec.Request
    protected def prepareRequest: Future[Request] = prepare
  }
}
