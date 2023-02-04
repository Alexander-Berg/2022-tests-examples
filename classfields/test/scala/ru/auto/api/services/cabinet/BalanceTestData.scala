package ru.auto.api.services.cabinet

import ru.auto.api.auth.Application.swagger
import ru.auto.api.model.billing.BalanceId
import ru.auto.api.model.{AutoruDealer, AutoruUser, RequestParams}
import ru.auto.api.util.{Request, RequestImpl}

object BalanceTestData {

  val testRequestId = "test-request-id"
  val dealerId = 16453
  val agencyDealerId = 10105
  val dealerUserId = 11913489
  val agencyDealerUserId = 10576915
  val wrongUserId = 10987453
  val dealer = AutoruDealer(dealerId)
  val agencyDealer = AutoruDealer(agencyDealerId)
  val accountId = Some(1)
  val dealerBalanceId = BalanceId(6768467)
  val agencyDealerBalanceId = BalanceId(6837255)
  val agencyBalanceId = BalanceId(41468582)

  val dealerRequest: Request = {
    val r = new RequestImpl {
      override def requestId: String = testRequestId
    }
    r.setApplication(swagger)
    r.setUser(AutoruUser(dealerUserId))
    r.setDealer(dealer)
    r.setRequestParams(RequestParams.empty)
    r
  }

  val agencyDealerRequest: Request = {
    val r = new RequestImpl {
      override def requestId: String = testRequestId
    }
    r.setApplication(swagger)
    r.setUser(AutoruUser(agencyDealerUserId))
    r.setDealer(agencyDealer)
    r.setRequestParams(RequestParams.empty)
    r
  }

  val wrongUserRequest: Request = {
    val r = new RequestImpl {
      override def requestId: String = testRequestId
    }
    r.setApplication(swagger)
    r.setUser(AutoruUser(wrongUserId))
    r.setDealer(agencyDealer)
    r.setRequestParams(RequestParams.empty)
    r
  }
}
