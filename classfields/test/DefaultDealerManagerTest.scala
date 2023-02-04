package auto.c2b.donald.logic.test

import auto.c2b.donald.donald_model.Requisites
import auto.c2b.common.postgresql.{QueueDao, QueueDaoImpl}
import auto.c2b.donald.logic.AccountManager.AccountManager
import auto.c2b.donald.logic.{AccountManager, DealerManager}
import auto.c2b.donald.logic.DealerManager.DealerManager
import auto.c2b.donald.model.AccountType
import auto.c2b.donald.model.Types.{CmeDealerId, Money}
import auto.c2b.donald.model.errors.DonaldError
import auto.c2b.donald.model.errors.DonaldError.{AlreadyExists, DealerNotFound}
import auto.c2b.donald.storage.AccountDao.AccountDao
import auto.c2b.donald.storage.DealerDao.DealerDao
import auto.c2b.donald.storage.{DealerDao, DomainEventQueue}
import auto.c2b.donald.storage.DomainEventQueue.DomainEventQueue
import auto.c2b.donald.storage.postgresql.{PgAccountDao, PgDealerDao, PgDomainEventQueue}
import common.zio.doobie.testkit.TestPostgresql
import common.zio.doobie.syntax._
import common.zio.logging.Logging
import doobie.Transactor
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import zio.magic._
import zio.{Has, Task, URIO, ZIO, ZLayer}
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect.sequential
import zio.test.environment.TestEnvironment

object DefaultDealerManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val suit = suite("DefaultDealerManagerTest")(
      createDealerWithSuccess,
      createDealerWithAlreadyExist,
      addHoodBuyoutWithDealerNotFound,
      addHoodBuyoutWithSuccess,
      addedHoodBuyoutWithAccountAlreadyExist
    ) @@ sequential

    suit.provideCustomLayerShared {
      ZLayer
        .wireSome[TestEnvironment, DealerManager with Logging.Logging with Has[
          Transactor[Task]
        ] with DealerDao with DomainEventQueue with Has[QueueDao] with AccountManager with AccountDao](
          TestPostgresql.managedTransactor,
          DealerManager.live,
          Logging.live,
          PgDealerDao.live,
          PgDomainEventQueue.live,
          QueueDaoImpl.live,
          AccountManager.live,
          PgAccountDao.live
        )
    }
  }

  val cmeDealerId100: CmeDealerId = CmeDealerId("test100")
  val cmeDealerId101: CmeDealerId = CmeDealerId("test101")
  val cmeDealerId102: CmeDealerId = CmeDealerId("test102")

  private def createDealerWithSuccess = testM("create account with success and push event about dealer is created") {
    for {
      queue <- ZIO.service[DomainEventQueue.Service]
      accountManager <- ZIO.service[AccountManager.Service]
      dealerDao <- ZIO.service[DealerDao.Service]
      tx <- ZIO.service[Transactor[Task]]
      result <- createDealer(cmeDealerId100)
      events <- queue.poll(10).transactIO(tx)
      dealer <- dealerDao.findByCmeDealerId(cmeDealerId100).transactIO
      accounts <- dealer.map(d => accountManager.getAccountsBy(d.id)).getOrElse(ZIO.succeed(List.empty))
      _ <- queue.remove(events.keys.toSeq).transactIO(tx)
    } yield {
      assertTrue(result.isRight) &&
      assertTrue(events.values.headOption.exists(_.event.isDealerCreated)) &&
      assertTrue(events.values.headOption.exists(_.meta.exists(_.createdAt.nonEmpty))) &&
      assertTrue(dealer.nonEmpty) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Commission))
    }
  }

  private def createDealerWithAlreadyExist =
    testM("create account with already exist and push event about dealer is created once") {
      for {
        queue <- ZIO.service[DomainEventQueue.Service]
        success <- createDealer(cmeDealerId101)
        alreadyExist <- createDealer(cmeDealerId101)
        events <- queue.poll(10).transactIO
        _ <- queue.remove(events.keys.toSeq).transactIO
      } yield {
        assertTrue(success.isRight) &&
        assertTrue(alreadyExist.left.exists(_.isInstanceOf[AlreadyExists])) &&
        assertTrue(events.values.headOption.exists(_.event.isDealerCreated)) &&
        assertTrue(events.values.headOption.exists(_.meta.exists(_.createdAt.nonEmpty))) &&
        assertTrue(events.size == 1)
      }
    }

  private def addHoodBuyoutWithDealerNotFound =
    testM("try to add dealer in hood buyout, but dealer not found") {
      for {
        queue <- ZIO.service[DomainEventQueue.Service]
        nonNegative <- ZIO.fromEither(refineV[NonNegative](11L))
        notFound <- addedHoodBuyout(CmeDealerId("not_found"), nonNegative)
        events <- queue.poll(10).transactIO
      } yield {
        assertTrue(notFound.isLeft) &&
        assertTrue(notFound.left.exists(_.isInstanceOf[DealerNotFound])) &&
        assertTrue(events.isEmpty)
      }
    }

  private def addHoodBuyoutWithSuccess = testM("add dealer in hood buyout with success") {
    for {
      dealerDao <- ZIO.service[DealerDao.Service]
      accountManager <- ZIO.service[AccountManager.Service]
      queue <- ZIO.service[DomainEventQueue.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](100L))
      _ <- createDealer(cmeDealerId102)
      _ <- addedHoodBuyout(cmeDealerId102, nonNegative)
      dealer <- dealerDao.findByCmeDealerId(cmeDealerId102).transactIO
      accounts <- dealer.map(dealer => accountManager.getAccountsBy(dealer.id)).getOrElse(ZIO.succeed(List.empty))
      events <- queue.poll(10).transactIO
      _ <- queue.remove(events.keys.toSeq).transactIO
    } yield {
      assertTrue(dealer.exists(_.cmeDealerId == cmeDealerId102)) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Settlement)) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Deposit)) &&
      assertTrue(accounts.exists(_.id.accountType == AccountType.Commission)) &&
      assertTrue(events.size == 2) &&
      assertTrue(events.values.exists(_.event.isDealerCreated)) &&
      assertTrue(events.values.exists(_.event.isHoodBuyoutAdded)) &&
      assertTrue(accounts.size == 3)
    }
  }

  private def addedHoodBuyoutWithAccountAlreadyExist = testM("dealer has been added in hood buyout already") {
    for {
      queue <- ZIO.service[DomainEventQueue.Service]
      nonNegative <- ZIO.fromEither(refineV[NonNegative](100L))
      _ <- createDealer(cmeDealerId102)
      _ <- addedHoodBuyout(cmeDealerId102, nonNegative)
      events <- queue.poll(10).transactIO
      _ <- queue.remove(events.keys.toSeq).transactIO
      res <- addedHoodBuyout(cmeDealerId102, nonNegative)
      events2 <- queue.poll(10).transactIO
    } yield {
      assertTrue(events2.isEmpty) &&
      assertTrue(res.left.exists(_.isInstanceOf[AlreadyExists]))
    }
  }

  private def createDealer(cmeDealerId: CmeDealerId): URIO[Has[DealerManager.Service], Either[DonaldError, Unit]] = {
    for {
      dealerManager <- ZIO.service[DealerManager.Service]
      res <- dealerManager.createDealer(cmeDealerId, Requisites.of()).fold(f => Left(f), s => Right(s))
    } yield res
  }

  private def addedHoodBuyout(
      cmeDealerId: CmeDealerId,
      depositSum: Money): URIO[Has[DealerManager.Service], Either[DonaldError, Unit]] = {
    for {
      dealerManager <- ZIO.service[DealerManager.Service]
      res <- dealerManager.addHoodBuyout(cmeDealerId, depositSum).fold(f => Left(f), s => Right(s))
    } yield res
  }
}
