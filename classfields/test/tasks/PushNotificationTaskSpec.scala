package ru.yandex.vertis.billing.banker.tasks

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.actor.NotifyActor.Request.Batch
import ru.yandex.vertis.billing.banker.config.NotifyClientSettings
import ru.yandex.vertis.billing.banker.actor.EffectActorProtocol
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.model.{AccountTransaction, AccountTransactions}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.{Activities, PushStatus, PushStatuses}
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  paymentSystemTransactionIdGen,
  AccountTransactionGen,
  Producer
}
import ru.yandex.vertis.billing.banker.push.AsyncNotificationClient.{
  DuplicateNotificationException,
  PushProcessFailureException
}
import ru.yandex.vertis.billing.banker.service.impl.GlobalAccountTransactionService
import ru.yandex.vertis.billing.banker.tasks.PushNotificationTaskSpec.{
  DuplicateNotificationFail,
  IllegalArgumentFail,
  NonRepeatableFailsGen,
  PossibleFailsGen,
  RepeatableFailsGen
}
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Spec on [[PushNotificationTask]]
  *
  * @author ruslansd
  */
class PushNotificationTaskSpec
  extends TestKit(ActorSystem("PushNotificationTaskSpec"))
  with AsyncSpecBase
  with MockFactory
  with Matchers
  with AnyWordSpecLike
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  case class AnswerSource(trs: Iterable[AccountTransaction], response: Option[Throwable] = None)

  private def withActorMock[T](sources: Iterable[AnswerSource])(action: ActorRef => Future[T]): Future[T] = {
    val probe = TestProbe()

    val result = action(probe.ref)

    sources.foreach { source =>
      val transactions = source.trs
      probe.expectMsgPF() {
        case Batch(`transactions`, true) =>
          val answer = source.response match {
            case Some(throwable) =>
              EffectActorProtocol.Fail(throwable)
            case None =>
              EffectActorProtocol.Done
          }
          probe.reply(answer)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    result
  }

  private def serviceMock(
      trsForFetch: Iterable[AccountTransaction],
      willBeMarkedAsFailed: Iterable[AccountTransaction] = Iterable.empty): GlobalAccountTransactionService = {
    val service = mock[GlobalAccountTransactionService]

    (service
      .get(_: Seq[TransactionsFilter])(_: RequestContext))
      .expects(*, *)
      .returns(Future.successful(trsForFetch))

    willBeMarkedAsFailed.foreach { tr =>
      (service
        .updatePushStatus(_: AccountTransaction, _: PushStatus)(_: RequestContext))
        .expects(tr, *, *)
        .onCall { (_, status, _) =>
          status.id shouldBe PushStatuses.Failed.id
          Future.unit
        }
    }

    service
  }

  private def serviceMockGetFail(throwable: Throwable): GlobalAccountTransactionService = {
    val service = mock[GlobalAccountTransactionService]
    (service
      .get(_: Seq[TransactionsFilter])(_: RequestContext))
      .expects(*, *)
      .returns(Future.failed(throwable))
    service
  }

  "PushNotificationTask" should {
    "process" when {
      "empty set passed" in {
        val source = AnswerSource(Iterable.empty)
        val result = withActorMock(Seq(source)) { actor =>
          val service = serviceMock(source.trs)
          val task = new PushNotificationTask(actor, service, NotifyClientSettings.Default)
          task.execute()
        }
        result.futureValue
      }
      "non empty set passed" in {
        val trs = AccountTransactionGen.next(100)
        val source = AnswerSource(trs)
        val result = withActorMock(Seq(source)) { actor =>
          val service = serviceMock(trs)
          val task = new PushNotificationTask(actor, service, NotifyClientSettings.Default)
          task.execute()
        }
        result.futureValue
      }
      "DuplicateNotificationException thrown on notify method with OneByOne policy" in {
        val trs = AccountTransactionGen.next(100)
        val head = AnswerSource(trs, Some(DuplicateNotificationFail))
        val tail = trs.map { tr =>
          AnswerSource(Seq(tr))
        }.toSeq
        val sources = head +: tail
        val result = withActorMock(sources) { actor =>
          val service = serviceMock(trs)
          val task = new PushNotificationTask(actor, service, NotifyClientSettings.OneByOne)
          task.execute()
        }
        result.toTry match {
          case Success(_) =>
            ()
          case other =>
            fail(s"Unexpected $other")
        }
      }
      "repeatable fail exception and then repeatable exceptions thrown on notify method with OneByOne policy" in {
        forAll(
          RepeatableFailsGen,
          Gen.nonEmptyListOf(AccountTransactionGen)
        ) { (repeatableFail, trs) =>
          val head = AnswerSource(trs, Some(repeatableFail))
          val tail = trs.map { tr =>
            val failOpt = Gen.option(RepeatableFailsGen).next
            AnswerSource(Seq(tr), failOpt)
          }
          val sources = head +: tail
          val result = withActorMock(sources) { actor =>
            val willBeMarkerAsFailedSources = tail.filter { source =>
              source.response.contains(DuplicateNotificationFail)
            }
            val willBeMarkerAsFailedTrs = willBeMarkerAsFailedSources.flatMap(_.trs)
            val service = serviceMock(trs, willBeMarkerAsFailedTrs)
            val task = new PushNotificationTask(actor, service, NotifyClientSettings.OneByOne)
            task.execute()
          }
          val expectedResult =
            if (repeatableFail == DuplicateNotificationFail) {
              Success(())
            } else {
              Failure(repeatableFail)
            }
          result.toTry shouldBe expectedResult
        }
      }

//      "send transaction activation with every refund" in {
//        val refunds = accountTransactionGen(AccountTransactions.Refund).next(5)
//        val refundsFor = refunds.map { r =>
//          val incoming = accountTransactionGen(AccountTransactions.Incoming).next
//          incoming.copy(id = r.refundFor.get, income = -r.income, refund = -r.income, activity = Activities.Active)
//        }
//        val inActiveTrs =
//          accountTransactionGen(AccountTransactions.Incoming).next(5).map(_.copy(activity = Activities.Inactive))
//        val activeTrs =
//          accountTransactionGen(AccountTransactions.Incoming).next(5).map(_.copy(activity = Activities.Active))
//
//        val trs = refunds ++ refundsFor ++ inActiveTrs
//        val source = AnswerSource(trs)
//        val result = withActorMock(Seq(source)) { actor =>
//          val service = serviceMock(trs)
//          val task = new PushNotificationTask(actor, service, NotifyClientSettings.Default)
//          task.execute()
//        }
//        result.futureValue
//      }
    }
    "fail" when {
      "get transactions throw exception" in {
        val result = withActorMock(Iterable.empty) { actor =>
          val service = serviceMockGetFail(IllegalArgumentFail)
          val task = new PushNotificationTask(actor, service, NotifyClientSettings.Default)
          task.execute()
        }
        result.toTry match {
          case Failure(`IllegalArgumentFail`) =>
            ()
          case other =>
            fail(s"Unexpected $other")
        }
      }
      "exception thrown on notify method call with Default policy" in {
        forAll(PossibleFailsGen, Gen.nonEmptyListOf(AccountTransactionGen)) { (cause, trs) =>
          val source = AnswerSource(trs, Some(cause))
          val result = withActorMock(Seq(source)) { actor =>
            val service = serviceMock(source.trs)
            val task = new PushNotificationTask(actor, service, NotifyClientSettings.Default)
            task.execute()
          }
          result.toTry match {
            case Failure(`cause`) =>
              ()
            case other =>
              fail(s"Unexpected $other")
          }
        }
      }
      "non repeatable exception thrown on notify method call with OneByOne policy" in {
        forAll(NonRepeatableFailsGen, Gen.nonEmptyListOf(AccountTransactionGen)) { (nonRepeatableFail, trs) =>
          val source = AnswerSource(trs, Some(nonRepeatableFail))
          val result = withActorMock(Seq(source)) { actor =>
            val service = serviceMock(trs)
            val task = new PushNotificationTask(actor, service, NotifyClientSettings.OneByOne)
            task.execute()
          }
          result.toTry match {
            case Failure(`nonRepeatableFail`) =>
              ()
            case other =>
              fail(s"Unexpected $other")
          }
        }
      }
      "repeatable fail exception and then non repeatable exception thrown on notify method with OneByOne policy" in {
        forAll(
          RepeatableFailsGen,
          NonRepeatableFailsGen,
          Gen.nonEmptyListOf(AccountTransactionGen)
        ) { (repeatableFail, nonRepeatableFail, trs) =>
          val head = AnswerSource(trs, Some(repeatableFail))
          val second = AnswerSource(Seq(trs.head), Some(nonRepeatableFail))
          val sources = Seq(head, second)
          val result = withActorMock(sources) { actor =>
            val service = serviceMock(trs)
            val task = new PushNotificationTask(actor, service, NotifyClientSettings.OneByOne)
            task.execute()
          }
          result.toTry match {
            case Failure(`nonRepeatableFail`) =>
              ()
            case other =>
              fail(s"Unexpected $other")
          }
        }
      }
    }
  }

}

object PushNotificationTaskSpec {

  private val IllegalArgumentFail = new IllegalArgumentException("fail")
  private val DuplicateNotificationFail = DuplicateNotificationException("fail")
  private val PushProcessFail = PushProcessFailureException("fail")
  private val IllegalStateFail = new IllegalStateException("fail")
  private val RuntimeFail = new RuntimeException("fail")

  private val RepeatableFails = Seq(
    IllegalArgumentFail,
    DuplicateNotificationFail,
    PushProcessFail,
    IllegalStateFail
  )

  private val RepeatableFailsGen: Gen[Throwable] = {
    Gen.oneOf(RepeatableFails)
  }

  private val NonRepeatableFails = Seq(
    RuntimeFail
  )

  private val NonRepeatableFailsGen: Gen[Throwable] = {
    Gen.oneOf(NonRepeatableFails)
  }

  private val PossibleFailsGen: Gen[Throwable] = {
    for {
      repeatable <- RepeatableFailsGen
      nonRepeatable <- NonRepeatableFailsGen
      throwable <- Gen.oneOf(repeatable, nonRepeatable)
    } yield throwable
  }

}
