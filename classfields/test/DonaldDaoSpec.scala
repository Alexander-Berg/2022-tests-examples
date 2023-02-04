package auto.c2b.donald.storage.test

import auto.c2b.donald.donald_model.Requisites
import auto.c2b.donald.model.{Account, AccountType}
import auto.c2b.donald.model.errors.DonaldError.AlreadyExists
import auto.c2b.donald.model.Types.CmeDealerId
import auto.c2b.donald.storage.domain_events.{DealerCreated, DomainEvent, Meta}
import auto.c2b.donald.storage.{AccountDao, BillDao, DealerDao, DomainEventQueue}
import auto.c2b.donald.storage.postgresql.{PgAccountDao, PgBillDao, PgDealerDao, PgDomainEventQueue}
import auto.c2b.common.postgresql.QueueDaoImpl
import common.scalapb.ScalaProtobuf
import zio.test.TestAspect.sequential
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import zio.ZIO
import zio.test._

import java.time.Instant

object DonaldDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("DonaldDaoSpec")(
      createDealerWithSuccess,
      creteDealerWithAlreadyExist,
      findDealerWithSome,
      findDealerWithNone,
      createAccountWithConstraintFailure,
      createAccountWithSuccess,
      createAccountWithDuplicate,
      findAccountWithNoneResult,
      findAccountWithSomeResult,
      findAllAccountsByDealerIdWithEmpty,
      findAllAccountsByDealerNonEmpty,
      findAllAccountsByCmeDealerIdWithEmpty,
      findAllAccountsByCmeDealerIdWithNonEmpty,
      createBillWithConstraintFailure,
      createBillWithSuccess,
      getEmptyBillsList,
      getNonEmptyBillsList,
      checkPushAndPoll,
      checkOrder,
      createAccountsBatchWithSuccess,
      tryToCreateAccountsEmptyList,
      tryToCreateAccountsWithDuplicate
    ) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> (PgDealerDao.live ++ PgAccountDao.live ++ PgBillDao.live ++ QueueDaoImpl.live) >+> PgDomainEventQueue.live
    )
  }

  val cmeDealerId1: CmeDealerId = CmeDealerId("test1")
  val cmeDealerId2: CmeDealerId = CmeDealerId("test2")
  val cmeDealerId3: CmeDealerId = CmeDealerId("test3")
  val cmeDealerId4: CmeDealerId = CmeDealerId("test4")
  val cmeDealerId5: CmeDealerId = CmeDealerId("test5")
  val cmeDealerId6: CmeDealerId = CmeDealerId("test6")
  val cmeDealerId7: CmeDealerId = CmeDealerId("test7")
  val cmeDealerId8: CmeDealerId = CmeDealerId("test8")
  val cmeDealerId9: CmeDealerId = CmeDealerId("test9")
  val cmeDealerId10: CmeDealerId = CmeDealerId("test10")
  val cmeDealerId11: CmeDealerId = CmeDealerId("test11")
  val cmeDealerId12: CmeDealerId = CmeDealerId("test12")
  val cmeDealerId13: CmeDealerId = CmeDealerId("test13")
  val cmeDealerId14: CmeDealerId = CmeDealerId("test14")
  val cmeDealerId15: CmeDealerId = CmeDealerId("test15")
  val cmeDealerId16: CmeDealerId = CmeDealerId("test16")

  private def createDealerWithSuccess = testM("create dealer with success") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      dealer <- dealerDao.create(cmeDealerId1, Requisites.of()).transactIO
    } yield {
      assertTrue(dealer.exists(_.cmeDealerId == cmeDealerId1))
    }
  }

  private def creteDealerWithAlreadyExist =
    testM("create dealer with already exist") {
      for {
        dealerDao <- ZIO.service[DealerDao.Service]
        _ <- dealerDao.create(cmeDealerId2, Requisites.of()).transactIO
        error <- dealerDao.create(cmeDealerId2, Requisites.of()).transactIO
      } yield {
        assertTrue(error.isLeft) && assertTrue(error.left.exists(_.isInstanceOf[AlreadyExists]))
      }
    }

  private def findDealerWithSome = testM("find dealer with Some") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      _ <- dealerDao.create(cmeDealerId3, Requisites.of()).transactIO
      dealer <- dealerDao.findByCmeDealerId(cmeDealerId3).transactIO
    } yield {
      assertTrue(dealer.exists(_.cmeDealerId == cmeDealerId3))
    }
  }

  private def findDealerWithNone = testM("find dealer with None") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      dealer <- dealerDao.findByCmeDealerId(cmeDealerId4).transactIO
    } yield {
      assertTrue(dealer.isEmpty)
    }
  }

  private def createAccountWithConstraintFailure =
    testM("create account with failure. Dealer should be created before") {
      for {
        accountDao <- ZIO.service[AccountDao.Service]
        nonNegative <- ZIO.fromEither(refineV[NonNegative](11L))
        error <- accountDao
          .create(Account.AccountId(nonNegative, AccountType.Deposit))
          .transactIO
          .fold(db => Left(db), r => Right(r))
      } yield {
        assertTrue(error.isLeft)
      }
    }

  private def createAccountWithSuccess = testM("create account with success") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId5, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      account <- accountDao.create(Account.AccountId(dealer.id, AccountType.Deposit)).transactIO
    } yield {
      assertTrue(account.exists(_.id == Account.AccountId(dealerId = dealer.id, accountType = AccountType.Deposit))) &&
      assertTrue(account.exists(_.createdAt.isEmpty))
    }
  }

  private def createAccountWithDuplicate = testM("create account with duplicate") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId6, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      _ <- accountDao.create(Account.AccountId(dealer.id, AccountType.Deposit)).transactIO
      alReadyExist <- accountDao.create(Account.AccountId(dealer.id, AccountType.Deposit)).transactIO
    } yield {
      assertTrue(alReadyExist.isLeft) && assertTrue(alReadyExist.left.exists(_.isInstanceOf[AlreadyExists]))
    }
  }

  private def findAccountWithNoneResult = testM("find account with none result") {
    for {
      accountDao <- ZIO.service[AccountDao.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](11L))
      account <- accountDao.get(Account.AccountId(nonNegative, AccountType.Deposit)).transactIO
    } yield {
      assertTrue(account.isEmpty)
    }
  }

  private def findAccountWithSomeResult = testM("find account with some result") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId7, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountId = Account.AccountId(dealerId = dealer.id, accountType = AccountType.Deposit)
      saved <- accountDao.create(accountId).transactIO
      found <- accountDao
        .get(accountId)
        .transactIO
    } yield {
      assertTrue(found.nonEmpty) && assertTrue(saved.exists(s => found.contains(s)))
    }
  }

  private def findAllAccountsByDealerIdWithEmpty = testM("find all accounts with empty result") {
    for {
      accountDao <- ZIO.service[AccountDao.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](11L))
      accounts <- accountDao.getAccountsBy(nonNegative).transactIO
    } yield {
      assertTrue(accounts.isEmpty)
    }
  }

  private def findAllAccountsByDealerNonEmpty = testM("find all accounts with non empty result") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId10, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountDeposit = Account.AccountId(dealerId = dealer.id, accountType = AccountType.Deposit)
      accountCommission = Account.AccountId(dealerId = dealer.id, accountType = AccountType.Commission)
      accountSettlement = Account.AccountId(dealerId = dealer.id, accountType = AccountType.Settlement)
      _ <- accountDao.create(accountDeposit).transactIO
      _ <- accountDao.create(accountCommission).transactIO
      _ <- accountDao.create(accountSettlement).transactIO
      accounts <- accountDao.getAccountsBy(dealer.id).transactIO
    } yield {
      assertTrue(accounts.size == 3) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Deposit)) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Settlement)) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Commission))
    }
  }

  private def findAllAccountsByCmeDealerIdWithEmpty = testM("find all accounts by cmeDealerId with empty result") {
    for {
      accountDao <- ZIO.service[AccountDao.Service]
      accounts <- accountDao.getAccountsBy(cmeDealerId11).transactIO
    } yield {
      assertTrue(accounts.isEmpty)
    }
  }

  private def findAllAccountsByCmeDealerIdWithNonEmpty =
    testM("find all accounts by cmeDealerId with non empty result") {
      for {
        dealerDao <- ZIO.service[DealerDao.Service]
        accountDao <- ZIO.service[AccountDao.Service]
        dealerOpt1 <- dealerDao.create(cmeDealerId15, Requisites.of()).transactIO
        dealerOpt2 <- dealerDao.create(cmeDealerId16, Requisites.of()).transactIO
        dealer1 <- ZIO.fromEither(dealerOpt1)
        dealer2 <- ZIO.fromEither(dealerOpt2)

        accountDeposit1 = Account.AccountId(dealer1.id, AccountType.Deposit)
        accountCommission1 = Account.AccountId(dealer1.id, AccountType.Commission)

        accountDeposit2 = Account.AccountId(dealer2.id, AccountType.Deposit)
        accountCommission2 = Account.AccountId(dealer2.id, AccountType.Commission)
        accountSettlement2 = Account.AccountId(dealer2.id, AccountType.Settlement)

        _ <- accountDao.create(Seq(accountDeposit1, accountCommission1)).transactIO
        _ <- accountDao.create(Seq(accountDeposit2, accountCommission2, accountSettlement2)).transactIO

        accounts1 <- accountDao.getAccountsBy(cmeDealerId15).transactIO
        accounts2 <- accountDao.getAccountsBy(cmeDealerId16).transactIO
      } yield {
        assertTrue(accounts1.size == 2) &&
        assertTrue(accounts1.exists(_.id.accountType == AccountType.Deposit)) &&
        assertTrue(accounts1.exists(_.id.accountType == AccountType.Commission)) &&
        assertTrue(accounts2.size == 3)
      }
    }

  private def createBillWithConstraintFailure = testM("create bill with constraint failure") {
    for {
      billDao <- ZIO.service[BillDao.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](11L))
      accountId = Account.AccountId(nonNegative, AccountType.Deposit)
      error <- billDao
        .create(accountId, nonNegative)
        .transactIO
        .fold(error => Left(error), s => Right(s))
      bills <- billDao.listSortedById(accountId).transactIO
    } yield {
      assertTrue(error.isLeft) && assertTrue(bills.isEmpty)
    }
  }

  private def createBillWithSuccess = testM("create bill with success") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      billDao <- ZIO.service[BillDao.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](1000L))
      dealerOpt <- dealerDao.create(cmeDealerId8, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountOpt <- accountDao.create(Account.AccountId(dealer.id, AccountType.Deposit)).transactIO
      account <- ZIO.fromEither(accountOpt)
      bill <- billDao.create(account.id, nonNegative).transactIO
    } yield {
      assertTrue(bill.accountId == account.id) &&
      assertTrue(bill.commonSum.value == 1000L) &&
      assertTrue(bill.paidSum.value == 0L)
    }
  }

  private def getEmptyBillsList = testM("get empty bills list") {
    for {
      billDao <- ZIO.service[BillDao.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](10000L))
      accountId = Account.AccountId(nonNegative, AccountType.Deposit)
      bills <- billDao.listSortedById(accountId).transactIO
    } yield {
      assertTrue(bills.isEmpty)
    }
  }

  private def getNonEmptyBillsList = testM("get non empty bills list") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      billDao <- ZIO.service[BillDao.Service]
      nonNegative1 <- ZIO.fromEither(refineV[NonNegative](1000L))
      nonNegative2 <- ZIO.fromEither(refineV[NonNegative](1001L))
      dealerOpt <- dealerDao.create(cmeDealerId9, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountDepositOpt <- accountDao.create(Account.AccountId(dealer.id, AccountType.Deposit)).transactIO
      accountCommissionOpt <- accountDao.create(Account.AccountId(dealer.id, AccountType.Commission)).transactIO
      accountDeposit <- ZIO.fromEither(accountDepositOpt)
      accountCommission <- ZIO.fromEither(accountCommissionOpt)
      bill1 <- billDao.create(accountDeposit.id, nonNegative1).transactIO
      bill2 <- billDao.create(accountDeposit.id, nonNegative2).transactIO
      bill3 <- billDao.create(accountCommission.id, nonNegative1).transactIO
      bill4 <- billDao.create(accountCommission.id, nonNegative2).transactIO
      sortedBillsDep = List(bill1, bill2).sortBy(_.id.value)
      sortedBillsCom = List(bill3, bill4).sortBy(_.id.value)
      billsDeposit <- billDao.listSortedById(accountDeposit.id).transactIO
      billsCommission <- billDao.listSortedById(accountCommission.id).transactIO
    } yield {
      assertTrue(sortedBillsDep == billsDeposit) && assertTrue(sortedBillsCom == billsCommission)
    }
  }

  private def checkPushAndPoll = testM("Push and poll from PgDomainEventQueue") {
    for {
      domainEventsQueue <- ZIO.service[DomainEventQueue.Service]
      currentTime = Instant.now()
      _ <- push(currentTime, 1)
      eventsMap <- domainEventsQueue.poll(10).transactIO
      events = eventsMap.values
      times = events.flatMap(_.meta.flatMap(m => m.createdAt.map(t => ScalaProtobuf.timestampToInstant(t))))
      _ <- domainEventsQueue.remove(eventsMap.keys.toSeq).transactIO
    } yield {
      assertTrue(events.nonEmpty) &&
      assertTrue(times.headOption.contains(currentTime)) &&
      assertTrue(events.headOption.exists(_.event.isDealerCreated))
    }
  }

  private def checkOrder = testM("order from PgDomainEventQueue") {
    for {
      domainEventsQueue <- ZIO.service[DomainEventQueue.Service]
      currentTime1 = Instant.now()
      currentTime2 = currentTime1.plusSeconds(1)
      currentTime3 = currentTime1.plusSeconds(3)
      _ <- push(currentTime1, 1)
      _ <- push(currentTime2, 2)
      _ <- push(currentTime3, 3)
      eventsMap <- domainEventsQueue.poll(10).transactIO
      events = eventsMap.values.toList
      _ <- domainEventsQueue.remove(eventsMap.keys.toSeq).transactIO
    } yield {
      assertTrue(events.size == 3) &&
      assertTrue(
        events.head.meta.exists(_.createdAt.exists(dbTime => ScalaProtobuf.timestampToInstant(dbTime) == currentTime1))
      ) &&
      assertTrue(
        events(1).meta.exists(_.createdAt.exists(dbTime => ScalaProtobuf.timestampToInstant(dbTime) == currentTime2))
      ) &&
      assertTrue(
        events(2).meta.exists(_.createdAt.exists(dbTime => ScalaProtobuf.timestampToInstant(dbTime) == currentTime3))
      )
    }
  }

  private def createAccountsBatchWithSuccess = testM("create accounts batch with success") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId12, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountDeposit = Account.AccountId(dealer.id, AccountType.Deposit)
      accountCommission = Account.AccountId(dealer.id, AccountType.Commission)
      accountSettlement = Account.AccountId(dealer.id, AccountType.Settlement)
      _ <- accountDao.create(Seq(accountDeposit, accountCommission, accountSettlement)).transactIO
      accounts <- accountDao.getAccountsBy(cmeDealerId12).transactIO
    } yield {
      assertTrue(accounts.size == 3)
    }
  }

  private def tryToCreateAccountsEmptyList = testM("try to create accounts batch with empty list") {
    for {
      accountDao <- ZIO.service[AccountDao.Service]
      _ <- accountDao.create(Seq()).transactIO
      accounts <- accountDao.getAccountsBy(cmeDealerId13).transactIO
    } yield {
      assertTrue(accounts.isEmpty)
    }
  }

  private def tryToCreateAccountsWithDuplicate = testM("try to create accounts with duplicate") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountDao <- ZIO.service[AccountDao.Service]
      dealerOpt <- dealerDao.create(cmeDealerId14, Requisites.of()).transactIO
      dealer <- ZIO.fromEither(dealerOpt)
      accountDeposit = Account.AccountId(dealer.id, AccountType.Deposit)
      accountCommission = Account.AccountId(dealer.id, AccountType.Commission)
      accountDepCreated <- accountDao.create(accountDeposit).transactIO
      error <- accountDao.create(Seq(accountDeposit, accountCommission)).transactIO
      accounts <- accountDao.getAccountsBy(dealer.id).transactIO
    } yield {
      assertTrue(accountDepCreated.exists(_.id.dealerId == dealer.id)) &&
      assertTrue(error.left.exists(_.isInstanceOf[AlreadyExists])) &&
      assertTrue(accounts.size == 1)
    }
  }

  private def push(instant: Instant, dealerId: Long) = {
    for {
      domainEventsQueue <- ZIO.service[DomainEventQueue.Service]
      _ <- domainEventsQueue
        .push(
          DomainEvent.of(
            meta = Some(Meta.of(Some(ScalaProtobuf.instantToTimestamp(instant)))),
            event = DomainEvent.Event.DealerCreated(DealerCreated.of(dealerId, requisites = Some(Requisites.of())))
          )
        )
        .transactIO
    } yield ()
  }
}
