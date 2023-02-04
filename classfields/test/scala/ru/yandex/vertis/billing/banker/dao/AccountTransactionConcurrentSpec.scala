package ru.yandex.vertis.billing.banker.dao

import java.util.concurrent.{CyclicBarrier, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.util.CleanableDao
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.AccountTransactionRequest.WithdrawRequest
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.gens.{incomingRqGen, withdrawRqGen, AccountGen, Producer, RequestParams}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.NotEnoughFunds

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author ruslansd
  */
trait AccountTransactionConcurrentSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with AsyncSpecBase
  with BeforeAndAfterEach {

  protected def accountDao: AccountDao with CleanableDao

  protected def transactionDao: AccountTransactionDao with CleanableDao

  private val account = AccountGen.next

  private val consumesCounts = 10
  private val incomeAmount = 100000

  require(incomeAmount % consumesCounts == 0)

  override def beforeEach(): Unit = {
    accountDao.clean()
    transactionDao.clean()

    accountDao.upsert(account).futureValue
    transactionDao.execute(income).futureValue
    super.beforeEach()
  }

  implicit override val ec = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(
      consumesCounts,
      new ThreadFactoryBuilder()
        .setNameFormat("AccountTransaction-%d")
        .build()
    )
  )

  private val income = incomingRqGen(
    RequestParams(
      activity = Some(Activities.Active),
      amount = Some(incomeAmount),
      account = Some(account.id),
      target = Some(Targets.Wallet)
    )
  ).next

  "AccountTransaction not enough money" should {
    val params = RequestParams(
      account = Some(account.id),
      amount = Some(incomeAmount / consumesCounts * 2),
      target = Some(Targets.Purchase),
      withdrawOpts = Some(WithdrawRequest.Options()),
      activity = Some(Activities.Active)
    )
    val consumes = withdrawRqGen(params).next(consumesCounts).toList

    "correctly work on sequence of consumes" in {
      consumes.foreach { c =>
        transactionDao
          .execute(c)
          .recover { case _: NotEnoughFunds =>
          }
          .futureValue
      }

      transactionDao.info(account.id).futureValue.balance shouldBe 0L
    }

    "correctly handle concurrent consumes" in {
      val barrier = new CyclicBarrier(consumesCounts)

      val futures = consumes.map { request =>
        Future(barrier.await()).flatMap(_ => transactionDao.execute(request))
      }

      intercept[NotEnoughFunds] {
        Future.sequence(futures).await
      }

      transactionDao.info(account.id).futureValue.balance shouldBe 0
    }
  }

  "AccountTransaction enough money" should {
    val params = RequestParams(
      account = Some(account.id),
      amount = Some(incomeAmount / consumesCounts),
      target = Some(Targets.Purchase),
      withdrawOpts = Some(WithdrawRequest.Options()),
      activity = Some(Activities.Active)
    )
    val consumes = withdrawRqGen(params).next(consumesCounts).toList

    "correctly handle sequence consumes" in {
      consumes.foreach { c =>
        transactionDao.execute(c).futureValue
      }

      transactionDao.info(account.id).futureValue.balance shouldBe 0L
    }

    "correctly handle concurrent consumes" in {
      val barrier = new CyclicBarrier(consumesCounts)

      val futures = consumes.map { request =>
        Future(barrier.await()).flatMap(_ => transactionDao.execute(request))
      }

      Future.sequence(futures).futureValue

      transactionDao.info(account.id).futureValue.balance shouldBe 0
    }
  }

}
