package ru.auto.api.services.billing.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain
import ru.yandex.vertis.banker.model.ApiModel.PaymentSystemId

class TicketIdTest extends AnyFunSuite with Matchers {

  private val salesmanDomain = SalesmanDomain.AutoruSalesmanDomain

  test("TicketId parse salesman ticket successfully") {
    TicketId("???", salesmanDomain) shouldBe SalesmanTicketId("???", salesmanDomain)
  }

  test("TicketId parse banker ticket successfully") {
    TicketId("banker:???:TRUST", salesmanDomain) shouldBe BankerTicketId("???", PaymentSystemId.TRUST)
  }

  test("TicketId parse compound ticket successfully") {
    TicketId("salesman:ABC:banker:DEF:TRUST", salesmanDomain) shouldBe CompoundTicketId(
      SalesmanTicketId("ABC", salesmanDomain),
      BankerTicketId("DEF", PaymentSystemId.TRUST)
    )
  }

  test("TicketId parse salesman ticket with separator successfully") {
    TicketId("???:ABC:DEF", salesmanDomain) shouldBe SalesmanTicketId("???:ABC:DEF", salesmanDomain)
  }
}
