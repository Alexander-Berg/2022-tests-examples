package ru.auto.salesman.dao.impl.jdbc

import java.sql.{BatchUpdateException, SQLTransactionRollbackException}

import cats.data.NonEmptyList
import org.scalacheck.Gen
import ru.auto.salesman.model.RetryCount
import ru.auto.salesman.test.{BaseSpec, TestException}
import zio.ZIO

class JdbcUtilSpec extends BaseSpec {

  private val retryCount = RetryCount(3)

  "withRetryOnDeadlock" should {

    "succeed immediately" in {
      forAll(Gen.posNum[Long]) { result =>
        withRetryOnDeadlock(retryCount)(result).success.value shouldBe result
      }
    }

    "succeed with three retries" in {
      forAll(Gen.posNum[Long]) { result =>
        val f = mockFunction[Long]
        val e = new SQLTransactionRollbackException
        inSequence {
          f.expects().throwing(e).repeat(3)
          f.expects().returning(result)
        }
        withRetryOnDeadlock(retryCount)(f()).success.value shouldBe result
      }
    }

    "fail on four failures" in {
      val f = mockFunction[Unit]
      val e = new SQLTransactionRollbackException
      f.expects().throwing(e).repeat(4)
      withRetryOnDeadlock(retryCount)(f()).failure.exception shouldBe e
    }

    "fail immediately on unexpected exception" in {
      val e = new TestException
      withRetryOnDeadlock(retryCount)(throw e).failure.exception shouldBe e
    }

    "fail after retries on unexpected exception" in {
      val f = mockFunction[Unit]
      val e = new SQLTransactionRollbackException
      val unexpected = new TestException
      inSequence {
        f.expects().throwing(e).twice()
        f.expects().throwing(unexpected)
      }
      withRetryOnDeadlock(retryCount)(f()).failure.exception shouldBe unexpected
    }
  }

  "batchWithRetryOnDeadlock" should {

    "succeed immediately" in {
      forAll(Gen.posNum[Long]) { result =>
        batchWithRetryOnDeadlock(retryCount)(
          ZIO.succeed(result)
        ).success.value shouldBe result
      }
    }

    "succeed with three retries" in {
      forAll(Gen.posNum[Long]) { result =>
        val f = mockFunction[Long]
        val e = new BatchUpdateException("", "", 1213, Array[Int]())
        inSequence {
          f.expects().throwing(e).repeat(3)
          f.expects().returning(result)
        }
        batchWithRetryOnDeadlock(retryCount)(
          ZIO.effect(f())
        ).success.value shouldBe result
      }
    }

    "fail on four failures" in {
      val f = mockFunction[Unit]
      val e = new BatchUpdateException("", "", 1213, Array[Int]())
      f.expects().throwing(e).repeat(4)
      batchWithRetryOnDeadlock(retryCount)(
        ZIO.effect(f())
      ).failure.exception shouldBe e
    }

    "fail immediately on unexpected exception" in {
      val e = new TestException
      batchWithRetryOnDeadlock(retryCount)(
        ZIO.fail(e)
      ).failure.exception shouldBe e
    }

    "fail after retries on unexpected exception" in {
      val f = mockFunction[Unit]
      val e = new BatchUpdateException("", "", 1213, Array[Int]())
      val unexpected = new TestException
      inSequence {
        f.expects().throwing(e).twice()
        f.expects().throwing(unexpected)
      }
      batchWithRetryOnDeadlock(retryCount)(
        ZIO.effect(f())
      ).failure.exception shouldBe unexpected
    }

    "fail immediately on non-deadlock error code" in {
      val e = new BatchUpdateException("", "", 1214, Array[Int]())
      batchWithRetryOnDeadlock(retryCount)(
        ZIO.fail(e)
      ).failure.exception shouldBe e
    }
  }

  "toSqlInList" should {

    "convert NonEmptyList of one element" in {
      NonEmptyList.one("package_turbo").toSqlInList shouldBe "('package_turbo')"
    }

    "convert NonEmptyList of two elements" in {
      NonEmptyList
        .of("package_turbo", "special")
        .toSqlInList shouldBe "('package_turbo','special')"
    }

    "convert NonEmptyList of several elements" in {
      NonEmptyList
        .of("one", "two", "three", "four")
        .toSqlInList shouldBe "('one','two','three','four')"
    }
  }

  "toSqlOrCond" should {
    "convert NonEmptyList of one element" in {
      NonEmptyList.one("x = 1").toSqlOrCond shouldBe "x = 1"
    }

    "convert NonEmptyList of two elements" in {
      NonEmptyList
        .of("x = 1", "x = 2 AND y > 0")
        .toSqlOrCond shouldBe "(x = 1) OR (x = 2 AND y > 0)"
    }

    "convert NonEmptyList of several elements" in {
      NonEmptyList
        .of(
          "x = 1",
          "x = 2 AND y > 0",
          "c = 'a' AND d = 'b' AND e = 'f'",
          "user_id=anton"
        )
        .toSqlOrCond shouldBe "(x = 1) OR (x = 2 AND y > 0) OR (c = 'a' AND d = 'b' AND e = 'f') OR (user_id=anton)"
    }
  }
}
