package ru.yandex.vertis.billing.dao

import billing.common.testkit.zio.ZIOSpecBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, TryValues}

trait AutoruBalanceDaoSpec extends AnyWordSpec with Matchers with TryValues with BeforeAndAfterEach with ZIOSpecBase {
  def autoruBalanceDao: AutoruBalanceDao

  "AutoruBalanceDao" should {

    "find existing client" in {
      val client = autoruBalanceDao.findBalanceClientId(20L).unsafeRun()
      client should not be empty
      client.get should be(1002L)
    }

    "return empty for corrupted link" in {
      val client = autoruBalanceDao.findBalanceClientId(999L).unsafeRun()
      client should be(empty)
    }

    "return empty for unexisting dealer id" in {
      val client = autoruBalanceDao.findBalanceClientId(1000L).unsafeRun()
      client should be(empty)
    }
  }
}
