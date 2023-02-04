package ru.auto.salesman.dao

import ru.auto.salesman.model.BalanceClientId
import ru.auto.salesman.test.BaseSpec

trait BalanceClientDaoSpec extends BaseSpec {

  def balanceClientDao: BalanceClientDao

  "BalanceClientDao" should {

    "get nothing for filter with empty result" in {
      balanceClientDao.get(1L).success.value shouldBe empty
    }

    "get one record for filter by id" in {
      val clientId = 16281L
      val c = balanceClientDao.get(clientId).success.value.value
      c.clientId shouldBe clientId
      c.balanceClientId shouldBe 6769492L
      c.balanceAgencyId shouldBe None
      c.accountId shouldBe 15579L
      c.amount shouldBe BigDecimal(14250L)
    }

    "get mapping from billing to autoru clients" in {
      val rs = balanceClientDao
        .getBillingAutoruClientsMapping(Set[BalanceClientId](6769492, 6837755))
        .success
        .value

      rs shouldBe Map(6769492 -> 16281, 6837755 -> 16283)
    }

    "get no records if no order and products present" in {
      val clientId = 1111L
      balanceClientDao.get(clientId).success.value shouldBe empty
    }

    "get record if no order and products present" in {
      val clientId = 1111L
      val c = balanceClientDao.getCore(clientId).success.value.value
      c.clientId shouldBe clientId
      c.balanceClientId shouldBe 6837732
      c.balanceAgencyId shouldBe None
    }
  }
}
