package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.model_core.{Correction, CorrectionRequest, OrderTransactions}
import ru.yandex.vertis.billing.service.impl.OrderServiceImpl
import ru.yandex.vertis.billing.settings.BalanceSettings
import ru.yandex.vertis.billing.util.AutomatedContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[CheckedOrderService]]
  *
  * @author ruslansd
  */
class CheckedOrderServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {

  private val Dao = {
    val m = mock[OrderDao]
    val order = OrderGen.next
    val transaction = orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Correction)).next
      .asInstanceOf[Correction]

    when(m.correct(?, ?, ?, ?, ?))
      .thenReturn(Success((transaction, order)))
    m
  }
  private val Balance = mock[Balance]
  private val BalanceSettings = mock[BalanceSettings]

  implicit val oc = AutomatedContext("test")

  def service(serviceName: String) = new OrderServiceImpl(Dao, Balance, BalanceSettings) with CheckedOrderService {
    override def checker: UserInputChecker = ???

    override def service: String = serviceName
  }

  "CheckedOrderService" should {

    "check comment for realty commercial only" in {
      val realtyCommercial = service(SupportedServices.RealtyCommercial)
      val request = CorrectionRequest(100, 100, "VSBILLING-123")
      realtyCommercial.execute2(request).get

      realtyCommercial.execute2(request.copy(comment = "https://st.yandex-team.ru/VSBILLING-2056")).get

      realtyCommercial.execute2(request.copy(comment = "https://st.yandex-team.ru/VSBILLING-2056 correction")).get

      realtyCommercial.execute2(request.copy(comment = "correction  https://st.yandex-team.ru/VSBILLING-2056")).get

      intercept[IllegalArgumentException] {
        realtyCommercial.execute2(request.copy(comment = "-")).get
      }

      intercept[IllegalArgumentException] {
        realtyCommercial.execute2(request.copy(comment = "VSBILLIN")).get
      }

      val realty = service(SupportedServices.Realty)

      realty.execute2(request).get

      realty.execute2(request.copy(comment = "Manual correction")).get
    }
  }

}
