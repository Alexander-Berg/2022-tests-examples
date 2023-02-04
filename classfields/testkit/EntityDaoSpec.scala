package ru.yandex.vertis.general.bonsai.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import general.bonsai.internal.internal_api.PagingRequest
import general.common.fail_policy.FailPolicy
import ru.yandex.vertis.general.bonsai.model.BonsaiEntity._
import ru.yandex.vertis.general.bonsai.model._
import ru.yandex.vertis.general.bonsai.model.testkit.BonsaiEntityUpdate
import ru.yandex.vertis.general.bonsai.model.testkit.BonsaiEntityUpdate._
import ru.yandex.vertis.general.bonsai.storage.EntityDao
import ru.yandex.vertis.general.bonsai.storage.EntityDao._
import zio._
import zio.test.Assertion._
import zio.test._

object EntityDaoSpec {

  def spec[T: BonsaiEntity: BonsaiEntityUpdate](
      label: String
    )(implicit entityGen: Gen[
        zio.ZEnv with Sized,
        T
      ]): Spec[zio.ZEnv with EntityDao with HasTxRunner with Sized, TestFailure[Any], TestSuccess] = {
    suite(s"$label should")(
      testM("add entity") {
        for {
          entity <- entityGen.runHead.someOrFailException
          _ <- runTx(EntityDao.createEntity(entity))
          saved <- runTx(EntityDao.getEntity[T](entity.id, Latest))
        } yield assert(saved.id)(equalsIgnoreCase(entity.id))

      },
      testM("get batch of entities with latest version") {
        for {
          entities <- Gen.listOfN(3)(entityGen).noShrink.runHead.someOrFailException
          ids = entities.map(_.id).toSet
          _ <- ZIO.foreach_(entities)(e => runTx(EntityDao.createEntity[T](e)))
          result <- runTx(EntityDao.getEntitiesWithLatestVersion[T](ids, failPolicy = FailPolicy.FAIL_FAST))
        } yield assert(result.map(_.id))(equalTo(ids))
      },
      testM("get batch of entities with exact version") {
        for {
          entities <- Gen.listOfN(3)(entityGen).noShrink.runHead.someOrFailException
          ids = entities.map(e => IdWithExactVersion(e.id, ExactVersion(e.version))).toSet
          _ <- ZIO.foreach_(entities)(e => runTx(EntityDao.createEntity[T](e)))
          result <- runTx(EntityDao.getEntitiesWithExactVersion[T](ids, failPolicy = FailPolicy.FAIL_FAST))
        } yield assert(result.map(e => IdWithExactVersion(e.id, ExactVersion(e.version))))(equalTo(ids))
      },
      testM("fails on duplicated create") {
        assertM({
          for {
            entity <- entityGen.runHead.someOrFailException
            _ <- runTx(EntityDao.createEntity(entity))
            _ <- runTx(EntityDao.createEntity(entity))
          } yield ()
        }.run)(fails(isSubtype[EntityAlreadyExists](Assertion.anything)))
      },
      testM("fails on non existing read") {
        assertM({
          for {
            entity <- entityGen.runHead.someOrFailException
            saved <- runTx(EntityDao.getEntity[T](entity.id, Latest))
          } yield saved
        }.run)(fails(isSubtype[EntityNotFound](Assertion.anything)))
      },
      testM("fails on partial non existing batch read with latest version") {
        assertM({
          for {
            entity <- entityGen.runHead.someOrFailException
            _ <- runTx(EntityDao.createEntity[T](entity))
            result <- runTx(
              EntityDao.getEntitiesWithLatestVersion[T](Set(entity.id, "non-exist"), failPolicy = FailPolicy.FAIL_FAST)
            )
          } yield result
        }.run)(fails(isSubtype[EntitiesNotFound](Assertion.anything)))
      },
      testM("fails on partial non existing batch read with exact version") {
        assertM({
          for {
            entity <- entityGen.runHead.someOrFailException
            _ <- runTx(EntityDao.createEntity[T](entity))
            result <- runTx(
              EntityDao.getEntitiesWithExactVersion[T](
                Set(
                  IdWithExactVersion(entity.id, ExactVersion(entity.version)),
                  IdWithExactVersion("non-exist", ExactVersion(0))
                ),
                failPolicy = FailPolicy.FAIL_FAST
              )
            )
          } yield result
        }.run)(fails(isSubtype[EntitiesNotFound](Assertion.anything)))

      },
      testM("updates an entity") {
        for {
          entity <- entityGen.runHead.someOrFailException
          _ <- runTx(EntityDao.createEntity(entity))
          updated <- ZIO.succeed(entity.withVersion(entity.version + 1))
          _ <- runTx(
            EntityDao.getEntity[T](entity.id, ExactVersion(entity.version)) *>
              EntityDao.updateEntity(updated)
          )
          result <- runTx(EntityDao.getEntity[T](entity.id, ExactVersion(updated.version)))
        } yield assert(result.id)(equalTo(entity.id))
      },
      testM("Batched updated of entities") {
        for {
          entity1 <- entityGen.runHead.someOrFailException
          entity2 <- entityGen.runHead.someOrFailException
          _ <- runTx(EntityDao.createEntity(entity1))
          _ <- runTx(EntityDao.createEntity(entity2))
          updated1 <- ZIO.succeed(entity1.withVersion(entity1.version + 13))
          updated2 <- ZIO.succeed(entity2.withVersion(entity2.version + 13))
          _ <- runTx {
            EntityDao.getEntitiesWithLatestVersion(Set(entity1.id, entity2.id), failPolicy = FailPolicy.FAIL_FAST) *>
              EntityDao.updateEntities(Seq(updated1, updated2))
          }
          result <- runTx(
            EntityDao.getEntitiesWithLatestVersion(Set(entity1.id, entity2.id), failPolicy = FailPolicy.FAIL_FAST)
          )
        } yield assert(result)(exists(hasField("version", (_: T).version, equalTo(updated1.version)))) &&
          assert(result)(exists(hasField("version", (_: T).version, equalTo(updated2.version))))
      },
      testM("fails on updating with same version") {
        assertM({
          for {
            entity <- entityGen.runHead.someOrFailException
            _ <- runTx(EntityDao.createEntity(entity))
            _ <- runTx(
              EntityDao.getEntity[T](entity.id, ExactVersion(entity.version)) *>
                EntityDao.updateEntity(entity)
            )
          } yield ()
        }.run)(fails(isSubtype[EntityVersionAlreadyExists](Assertion.anything)))
      },
      testM("list entity history") {
        for {
          initialEntity <- entityGen.runHead.someOrFailException
          _ <- runTx(EntityDao.createEntity(initialEntity))
          entitiesToUpdate =
            Iterator
              .iterate(initialEntity) { previous =>
                previous.withVersion(previous.version + 1)
              }
              .slice(1, 7)
              .toList
          _ <- ZIO.foldLeft(entitiesToUpdate)(initialEntity) { case (prev, entity) =>
            runTx(
              EntityDao.getEntity[T](prev.id, ExactVersion(prev.version)) *>
                EntityDao.updateEntity[T](entity).as(entity)
            )
          }
          result <- runTx(EntityDao.getHistory[T](initialEntity.id, blankPagingHistoryRequest))
        } yield assert(result.values)(equalTo(entitiesToUpdate.reverse :+ initialEntity))
      },
      testM("list entity history paging") {
        for {
          initialEntity <- entityGen.runHead.someOrFailException
          _ <- runTx(EntityDao.createEntity(initialEntity))
          entitiesToUpdate =
            Iterator
              .iterate(initialEntity) { previous =>
                previous.withVersion(previous.version + 1)
              }
              .slice(1, 7)
              .toList
          _ <- ZIO.foldLeft(entitiesToUpdate)(initialEntity) { case (prev, entity) =>
            runTx(
              // сначала нужно обратиться к записи, инчане обновиться не получится
              EntityDao.getEntity[T](prev.id, ExactVersion(prev.version)) *>
                EntityDao.updateEntity[T](entity).as(entity)
            )
          }
          result <- runTx(
            EntityDao.getHistory[T](initialEntity.id, PagingRequest(entitiesToUpdate.last.version.toString, 2))
          )
        } yield assertTrue(result.values.lengthCompare(2) == 0) &&
          assert(result.paging.cursorEnd)(equalTo(entitiesToUpdate.dropRight(2).last.version.toString)) &&
          assertTrue(result.paging.hasNextPage)
      },
      testM("filter entities") {
        for {
          entities <- Gen.listOfN(3)(entityGen).noShrink.runHead.someOrFailException
          _ <- ZIO.foreach(entities)(e => runTx(EntityDao.createEntity[T](e)))
          result <- runTx(EntityDao.filter[T](new EntityFilter(paging = PagingRequest(limit = Int.MaxValue - 100))))
        } yield assert(result.values.map(_.id).toSet)(hasSubset(entities.map(_.id).toSet))
      },
      testM("filter entities with paging") {
        for {
          entities <- Gen.listOfN(6)(entityGen).noShrink.runHead.someOrFailException
          _ <- ZIO.foreach(entities)(e => runTx(EntityDao.createEntity[T](e)))
          result <- runTx(EntityDao.filter[T](EntityFilter(paging = PagingRequest(limit = 5))))
        } yield assert(result.values.length)(isLessThanEqualTo(5))
      }
    )
  }
}
