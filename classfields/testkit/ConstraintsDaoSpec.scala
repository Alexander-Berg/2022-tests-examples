package ru.yandex.vertis.general.bonsai.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.general.bonsai.model._
import ru.yandex.vertis.general.bonsai.storage.ConstraintsDao
import ru.yandex.vertis.general.bonsai.storage.ConstraintsDao.{ChangeValue, ConstraintsDao}
import zio.test.Assertion._
import zio.test._

object ConstraintsDaoSpec {

  val refA1: EntityRef = EntityRef("a", "1000")
  val refA2: EntityRef = EntityRef("a", "2000")
  val refB2: EntityRef = EntityRef("b", "9999")

  def spec(
      label: String): Spec[zio.ZEnv with ConstraintsDao with HasTxRunner, TestFailure[BonsaiError], TestSuccess] = {
    suite(label)(
      testM("add uniq value") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
        } yield assertCompletes
      },
      testM("add uniq value twice without error") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
        } yield assertCompletes
      },
      testM("fails with UniqConstraintViolation on insert") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
          res <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "vvv"))).run
        } yield assert(res)(fails(equalTo(UniqConstraintViolation(refA2, "abc", "vvv", refA1))))
      },
      testM("delete uniq value") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
          _ <- runTx(ConstraintsDao.deleteUniqValues(refA1, Map("abc" -> "vvv")))
          _ <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "vvv")))
        } yield assertCompletes
      },
      testM("delete dies on conflict") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
          res <- runTx(ConstraintsDao.deleteUniqValues(refA2, Map("abc" -> "vvv"))).run
        } yield assert(res.untraced)(dies(hasMessage(containsString("owned by another entity"))))
      },
      testM("check uniq value") {
        for {
          res1 <- runTx(ConstraintsDao.checkUniqValue(refA1, "abc", "vvv"))
          res2 <- runTx(ConstraintsDao.checkUniqValue(refA2, "abc", "vvv"))

          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))

          res3 <- runTx(ConstraintsDao.checkUniqValue(refA1, "abc", "vvv"))
          res4 <- runTx(ConstraintsDao.checkUniqValue(refA2, "abc", "vvv"))

        } yield assert(res1)(isTrue ?? "res1") &&
          assert(res2)(isTrue ?? "res2") &&
          assert(res3)(isTrue ?? "res3") &&
          assert(res4)(isFalse ?? "res4")
      },
      testM("change uniq value") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "1")))
          _ <- runTx(ConstraintsDao.changeUniqValues(refA1, Map("abc" -> ChangeValue("1", "2"))))

          res1 <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "1"))).run
          res2 <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "2"))).run

        } yield assert(res1)(succeeds(anything)) &&
          assert(res2)(fails(equalTo(UniqConstraintViolation(refA2, "abc", "2", refA1))))
      },
      testM("change uniq value is idempotent") {
        for {
          _ <- runTx(ConstraintsDao.changeUniqValues(refA1, Map("abc" -> ChangeValue("1", "2"))))
          _ <- runTx(ConstraintsDao.changeUniqValues(refA1, Map("abc" -> ChangeValue("1", "2"))))

          res <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "2"))).run
        } yield assert(res)(fails(equalTo(UniqConstraintViolation(refA2, "abc", "2", refA1))))
      },
      testM("change uniq value fails with error") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "2")))
          _ <- runTx(ConstraintsDao.addUniqValues(refA2, Map("abc" -> "1"))).run
          res <- runTx(ConstraintsDao.changeUniqValues(refA2, Map("abc" -> ChangeValue("1", "2")))).run

        } yield assert(res)(fails(equalTo(UniqConstraintViolation(refA2, "abc", "2", refA1))))
      },
      testM("different entity types do not conflict with each other") {
        for {
          _ <- runTx(ConstraintsDao.addUniqValues(refA1, Map("abc" -> "vvv")))
          _ <- runTx(ConstraintsDao.addUniqValues(refB2, Map("abc" -> "vvv")))
        } yield assertCompletes
      },
      testM("add reference") {
        for {
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
        } yield assertCompletes
      },
      testM("add reference is idempotent") {
        for {
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
        } yield assertCompletes
      },
      testM("get incoming references") {
        for {
          res1 <- runTx(ConstraintsDao.getIncomingReferences(refB2))
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
          res2 <- runTx(ConstraintsDao.getIncomingReferences(refB2))
        } yield assert(res1)(isEmpty) && assert(res2)(equalTo(List(refA1)))
      },
      testM("reference is one-way") {
        for {
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
          res1 <- runTx(ConstraintsDao.getIncomingReferences(refB2))
          res2 <- runTx(ConstraintsDao.getIncomingReferences(refA1))
        } yield assert(res1)(isNonEmpty) && assert(res2)(isEmpty)
      },
      testM("delete reference") {
        for {
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
          res1 <- runTx(ConstraintsDao.getIncomingReferences(refB2))
          _ <- runTx(ConstraintsDao.deleteReferences(refA1, Set(refB2)))
          res2 <- runTx(ConstraintsDao.getIncomingReferences(refB2))
        } yield assert(res1)(isNonEmpty) && assert(res2)(isEmpty)
      },
      testM("delete reference is safe and idempotent") {
        for {
          _ <- runTx(ConstraintsDao.deleteReferences(refA1, Set(refB2)))
          _ <- runTx(ConstraintsDao.deleteReferences(refA1, Set(refB2)))
        } yield assertCompletes
      },
      testM("check no incoming references") {
        for {
          _ <- runTx(ConstraintsDao.checkNoReferences(refB2))
        } yield assertCompletes
      },
      testM("check no incoming references fails on reference") {
        for {
          _ <- runTx(ConstraintsDao.addReferences(refA1, Set(refB2)))
          _ <- runTx(ConstraintsDao.addReferences(refA2, Set(refB2)))
          res <- runTx(ConstraintsDao.checkNoReferences(refB2)).run
        } yield assert(res)(fails(equalTo(ExternalReferenceViolation(refB2, List(refA1, refA2)))))
      }
    )
  }
}
