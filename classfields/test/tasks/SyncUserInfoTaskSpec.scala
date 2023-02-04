package ru.yandex.vertis.billing.banker.tasks

import org.mockito.Mockito
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.AccountDao.ForAccountIds
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.ScanFilter.PaidSince
import ru.yandex.vertis.billing.banker.dao.{AccountDao, AccountTransactionDao}
import ru.yandex.vertis.billing.banker.model.gens._
import ru.yandex.vertis.billing.banker.model.{Account, AccountTransaction, AccountTransactions, EpochWithId, User}
import ru.yandex.vertis.billing.banker.service.{TypedKeyValueService, UserService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Random

/**
  * Spec on [[SyncUserInfoTask]]
  *
  * @author ruslansd
  */
class SyncUserInfoTaskSpec
  extends Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with MockitoSupport
  with BeforeAndAfterEach {

  private val userInfos = ArrayBuffer.empty[User]
  private val trs = ArrayBuffer.empty[AccountTransaction]
  private val accs = ArrayBuffer.empty[Account]
  private var returnEpochWithId = false

  override def beforeEach(): Unit = {
    super.beforeEach()
    userInfos.clear()
    trs.clear()
    accs.clear()
    returnEpochWithId = false
    Mockito.clearInvocations[Any](transactions, typedKeyValueService, userService, accounts)
  }

  private val transactions = {
    val m = mock[AccountTransactionDao]
    stub(m.scan _) {
      case PaidSince(EpochWithId(_, None), _) =>
        val result =
          if (returnEpochWithId) {
            List.empty
          } else {
            returnEpochWithId = true
            val result = accountTransactionGen(AccountTransactions.Incoming)
              .next(10)
              .toList
              .map(_.copy(epoch = Some(Random.nextInt())))
            trs ++= result
            result
          }
        Future.successful(result)
      case PaidSince(EpochWithId(_, Some(_)), _) =>
        Future.successful(List.empty)
    }
    m
  }

  private val typedKeyValueService = {
    val m = mock[TypedKeyValueService]
    when(m.get[EpochWithId](?)(?))
      .thenReturn(Future.successful(EpochWithId(0, None)))
    when(m.set[EpochWithId](?, ?)(?))
      .thenReturn(Future.successful(()))
    m
  }

  private val userService = {
    val m = mock[UserService]
    stub(m.batchPut _) { case batch =>
      userInfos ++= batch.map(_.getUser)
      Future.successful(())
    }
    m
  }

  private val accounts = {
    val m = mock[AccountDao]
    stub(m.get _) {
      case ForAccountIds(ids) =>
        Future.successful(ids.toList.map { id =>
          val acc = AccountGen.next.copy(id = id)
          accs += acc
          acc
        })
      case other =>
        fail(s"Unexpected filter $other")
    }
    m
  }

  private val task = new SyncUserInfoTask(
    transactions,
    userService,
    typedKeyValueService,
    accounts
  )

  "SyncUserInfoTask" should {
    "correctly process user transactions" in {
      task.execute().futureValue

      checkTransactionsAndUserInfo()

      Mockito.verify(userService).batchPut(?)
      Mockito.verify(typedKeyValueService).get[EpochWithId](?)(?)
      Mockito.verify(typedKeyValueService).set[EpochWithId](?, ?)(?)
      Mockito.verify(transactions, Mockito.times(2)).scan(?)
      Mockito.verify(accounts).get(?)
    }
  }

  private def checkTransactionsAndUserInfo(): Unit = {
    val expectedUsers = accs.map(_.user)
    val expectedAccounts = trs.map(_.account).toSet

    userInfos should contain theSameElementsAs expectedUsers
    accs.map(_.id) should contain theSameElementsAs expectedAccounts
    ()
  }

}
