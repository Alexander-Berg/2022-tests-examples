package ru.yandex.vertis.billing.service

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CyclicBarrier, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.model_core.HoldResponse._
import ru.yandex.vertis.billing.model_core.gens.{HoldRequestGen, Producer}
import ru.yandex.vertis.billing.model_core.{AccountId, HoldResponse}
import ru.yandex.vertis.billing.service.HoldService.AccountAmount

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
  * Specs on [[HoldService]].
  *
  * @author dimas
  */
trait HoldServiceSpec extends AsyncSpecBase {

  def holdService: HoldService

  implicit override protected val ec = ExecutionContext.fromExecutor(
    Executors.newScheduledThreadPool(
      5,
      new ThreadFactoryBuilder()
        .setNameFormat("HoldServiceSpec-%d")
        .build()
    )
  )

  "HoldService" should {
    "accept request" in {
      val request = HoldRequestGen.next
      holdService.tryHold(nextAccount, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
    }

    "not accept the same request twice" in {
      val request = HoldRequestGen.next
      val account = nextAccount
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }

      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(AlreadyExists(`request`, Some(_))) =>
      }
    }

    "take into account held funds" in {
      val request = HoldRequestGen.next
      val account = nextAccount
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      holdService.holdAmount(account) shouldBe Success(AccountAmount(account, request.amount))
    }

    "not take into account commit funds" in {
      val request = HoldRequestGen.next
      val account = nextAccount
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      holdService.commit(request.id) shouldBe Success(())
      holdService.holdAmount(account) shouldBe Success(AccountAmount(account, 0L))
    }

    "not take into account expired hold" in {
      val timeout = 500.millis
      val request = HoldRequestGen.next.copy(ttl = timeout)
      val account = nextAccount
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      Thread.sleep(3 * timeout.toMillis)
      holdService.holdAmount(account) should be(Success(AccountAmount(account, 0L)))
    }

    "accept the same hold request after expiration of the first one" in {
      val timeout = 500.millis
      val request = HoldRequestGen.next.copy(ttl = timeout)
      val account = nextAccount
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      Thread.sleep(3 * timeout.toMillis)
      holdService.tryHold(account, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
    }

    "reject hold request if there is no enough funds" in {
      val request = HoldRequestGen.next
      val account = nextAccount
      holdService.tryHold(account, request, request.amount - 1) should be(Success(NoEnoughFunds(request)))
    }

    "reject some of requests if funds not enough for all of them" in {
      val requests = HoldRequestGen.next(10).toSeq
      val holdNoMoreThan = requests.take(6).map(_.amount).sum
      val account = nextAccount
      val Success(responses) = holdService.tryHold(account, requests, holdNoMoreThan)
      val reqAndRes = requests.zip(responses)
      reqAndRes.take(6).foreach { case (req, res) =>
        res should matchPattern { case Ok(`req`, Some(_)) =>
        }
      }
      reqAndRes.drop(6).foreach { case (req, res) => res should be(NoEnoughFunds(req)) }
    }

    "correct handle the same concurrent requests" in {
      val parties = 3
      val barrier = new CyclicBarrier(parties)
      val request = HoldRequestGen.next
      val account = nextAccount
      val results = Iterator
        .continually(Future {
          barrier.await()
          holdService.tryHold(account, request, request.amount).get
        })
        .take(parties)
        .toList
      val result = Future.sequence(results).futureValue.toSet
      result.toList.sortBy(_.getClass.getSimpleName) should matchPattern {
        case HoldResponse.AlreadyExists(`request`, Some(_)) ::
            HoldResponse.Ok(`request`, Some(_)) ::
            Nil =>
      }
    }

    "correct count hold amount and commit with multiple accounts" in {
      val requests = HoldRequestGen.next(5).toList
      val expected = requests.map { r =>
        val account = nextAccount
        holdService.tryHold(account, r, r.amount)
        AccountAmount(account, r.amount)
      }

      holdService.holdAmount(expected.map(_.accountId)) match {
        case Success(holds) =>
          holds should contain theSameElementsAs expected
        case other =>
          fail(s"Unepeceted $other")
      }

      holdService.commit(requests.map(_.id)).get

      holdService.holdAmount(expected.map(_.accountId)) match {
        case Success(holds) =>
          val expectedAfterCommit =
            expected.map(e => AccountAmount(e.accountId, 0L))
          holds should contain theSameElementsAs expectedAfterCommit
        case other =>
          fail(s"Unepeceted $other")
      }
    }

    "correct handle concurrent requests on the same account" in {
      val parties = 3
      val barrier = new CyclicBarrier(parties)
      val account = nextAccount
      val noMoreThan = 10L
      val results = Iterator
        .continually(Future {
          val request = HoldRequestGen.next.copy(amount = 4)
          barrier.await()
          holdService.tryHold(account, request, noMoreThan).get
        })
        .take(parties)
        .toList
      val result = Future.sequence(results).futureValue
      result.count {
        case HoldResponse.Ok(_, Some(_)) => true
        case _ => false
      } should be(2)
      result.count {
        case _: HoldResponse.NoEnoughFunds => true
        case _ => false
      } should be(1)
    }

    "keep hold records after commit" in {
      val request = HoldRequestGen.next
      holdService.tryHold(nextAccount, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      holdService.commit(request.id) should be(Success(()))
      holdService.tryHold(nextAccount, request, request.amount) should matchPattern {
        case Success(AlreadyExists(`request`, Some(_))) =>
      }
    }

    "return already existing hold even there is no money left" in {
      val request = HoldRequestGen.next
      holdService.tryHold(nextAccount, request, request.amount) should matchPattern {
        case Success(Ok(`request`, Some(_))) =>
      }
      holdService.commit(request.id) shouldBe Success(())
      holdService.tryHold(nextAccount, request, 0L) should matchPattern {
        case Success(AlreadyExists(`request`, Some(_))) =>
      }
    }

    "fail on hold duplicates" in {
      val request = HoldRequestGen.next
      val requests = Seq(request, request)
      val noMoreThan = requests.map(_.amount).sum
      intercept[IllegalArgumentException] {
        holdService.tryHold(nextAccount, requests, noMoreThan).get
      }
    }
  }

  private val accounts = new AtomicInteger()
  private def nextAccount: AccountId = accounts.incrementAndGet().toString

}
