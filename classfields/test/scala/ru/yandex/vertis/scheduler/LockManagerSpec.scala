package ru.yandex.vertis.scheduler

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.Success

/**
 * Basic specs on [[ru.yandex.vertis.scheduler.LockManager]]
 *
 * @author dimas
 */
trait LockManagerSpec
  extends Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  import TestData._

  def lockManager: LockManager

  "LockManager" should {
    "acquire free task" in {
      lockManager.acquire(instance1, "1") should be(Success(true))
    }
    "not acquire already acquired task" in {
      lockManager.acquire(instance1, "1") should be(Success(false))
      lockManager.acquire(instance2, "1") should be(Success(false))
    }
    "release acquired task" in {
      lockManager.release(instance1, "1") should be(Success(()))
    }
  }

  override protected def afterAll(): Unit = {
    lockManager.shutdown()
    super.afterAll()
  }
}