package ru.auto.api.managers.balance

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.NeedAuthentication
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.yandex.vertis.mockito.MockitoSupport

class BalanceManagerSpec extends BaseSpec with MockitoSupport with TestRequest with ScalaCheckPropertyChecks {

  private val cabinetApiClient = mock[CabinetApiClient]
  private val manager = new BalanceManager(cabinetApiClient)

  "BalanceManager.getBalanceClient()" should {

    "get dealer balance client" in {
      forAll(balanceClientGen, dealerRequestGen) { (balanceClient, request) =>
        reset(cabinetApiClient)
        when(cabinetApiClient.getBalanceClient(?)(?)).thenReturnF(balanceClient)
        val res = manager.getBalanceClient()(request).futureValue
        res.balanceClientId shouldBe balanceClient.balanceClientId
        res.balanceAgencyId shouldBe balanceClient.balanceAgencyId
        verify(cabinetApiClient).getBalanceClient(?)(?)
        verifyNoMoreInteractions(cabinetApiClient)
      }
    }

    "not get private balance client" in {
      reset(cabinetApiClient)
      val res = manager.getBalanceClient().failed.futureValue
      res shouldBe a[NeedAuthentication]
      verifyNoMoreInteractions(cabinetApiClient)
    }
  }
}
