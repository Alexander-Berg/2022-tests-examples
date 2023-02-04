package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.ErrorLevel
import ru.yandex.vertis.general.feed.model.ErrorLevel.{Error, Warning}
import ru.yandex.vertis.general.feed.model.testkit.{FailedOfferGen, FeedTaskGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.storage.OfferErrorsDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgOfferErrorsDao
import zio.ZIO
import zio.random.Random
import zio.test.Assertion._
import zio.test._

object PgOfferErrorsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgOfferErrors")(
      testM("insert and list errors") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedTaskGen.any.noShrink,
          Gen.chunkOfN(15)(FailedOfferGen.any).noShrink
        ) { (sellerId, namespaceId, task, failedOffers) =>
          for {
            dao <- ZIO.service[OfferErrorsDao.Service]
            _ <- dao.insertOrIgnore(sellerId, namespaceId, task.taskId, failedOffers.tail).transactIO
            _ <- dao.insertOrIgnore(sellerId, namespaceId, task.taskId, List(failedOffers.head)).transactIO
            savedFirstPage <- dao.list(sellerId, namespaceId, task.taskId, None, LimitOffset(10, 0)).transactIO
            savedSecondPage <- dao.list(sellerId, namespaceId, task.taskId, None, LimitOffset(10, 10)).transactIO
          } yield assert(savedFirstPage)(hasSize(equalTo(10))) &&
            assert(savedSecondPage)(hasSize(equalTo(5))) &&
            assert((savedFirstPage ++ savedSecondPage).map(error => (error.key.externalOfferId, error.key.errorId)))(
              isSorted
            ) &&
            assert(savedFirstPage ++ savedSecondPage)(hasSameElements(failedOffers))
        }
      },
      testM("count errors") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedTaskGen.any.noShrink,
          Gen.chunkOfN(25)(FailedOfferGen.any).noShrink
        ) { (sellerId, namespaceId, task, failedOffers) =>
          for {
            dao <- ZIO.service[OfferErrorsDao.Service]
            _ <- dao.insertOrIgnore(sellerId, namespaceId, task.taskId, failedOffers).transactIO
            count <- dao.count(sellerId, namespaceId, task.taskId, None).transactIO
          } yield assert(count)(equalTo(25))
        }
      },
      testM("считаем ошибки в зависимости от уровня") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedTaskGen.any.noShrink,
          Gen.chunkOfN(25)(FailedOfferGen.warnings).noShrink,
          Gen.chunkOfN(25)(FailedOfferGen.errors).noShrink
        ) { (sellerId, namespaceId, task, warnings, errors) =>
          for {
            dao <- ZIO.service[OfferErrorsDao.Service]
            _ <- dao.insertOrIgnore(sellerId, namespaceId, task.taskId, warnings).transactIO
            _ <- dao.insertOrIgnore(sellerId, namespaceId, task.taskId, errors).transactIO
            count <- dao.count(sellerId, namespaceId, task.taskId, None).transactIO
            countWarning <- dao.count(sellerId, namespaceId, task.taskId, Some(Warning)).transactIO
            countError <- dao.count(sellerId, namespaceId, task.taskId, Some(Error)).transactIO
          } yield assert(count)(equalTo(50)) &&
            assert(countWarning)(equalTo(25)) &&
            assert(countError)(equalTo(25))
        }
      },
      testM("insert and list errors with level filter") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedTaskGen.any.noShrink,
          Gen.chunkOfN(15)(FailedOfferGen.errors).noShrink,
          Gen.chunkOfN(15)(FailedOfferGen.warnings).noShrink
        ) { (sellerId, namespaceId, task, failedOffersErrors, failedOffersWarnings) =>
          for {
            dao <- ZIO.service[OfferErrorsDao.Service]
            _ <- dao
              .insertOrIgnore(sellerId, namespaceId, task.taskId, failedOffersErrors ++ failedOffersWarnings)
              .transactIO
            savedWarnings <- dao
              .list(sellerId, namespaceId, task.taskId, Some(ErrorLevel.Warning), LimitOffset(100, 0))
              .transactIO
            savedErrors <- dao
              .list(sellerId, namespaceId, task.taskId, Some(ErrorLevel.Error), LimitOffset(100, 0))
              .transactIO
          } yield assert(savedErrors)(hasSize(equalTo(15))) &&
            assert(savedWarnings)(hasSize(equalTo(15))) &&
            assert(savedWarnings)(hasSameElements(failedOffersWarnings)) &&
            assert(savedErrors)(hasSameElements(failedOffersErrors))
        }
      }
    )
      .provideCustomLayer {
        Random.live ++ TestPostgresql.managedTransactor >+> PgOfferErrorsDao.live
      }
  }
}
