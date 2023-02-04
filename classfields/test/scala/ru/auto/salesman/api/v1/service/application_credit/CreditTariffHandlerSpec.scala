package ru.auto.salesman.api.v1.service.application_credit

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.service.application_credit.{
  CreditTariff,
  CreditTariffReadService,
  CreditTariffWriteService,
  TariffScope
}
import ru.auto.salesman.tariffs.CreditTariffs.{
  ApplicationCreditTariffs => ProtoApplicationCreditTariffs,
  DealersWithActiveApplicationCredit => ProtoDealersWithActiveApplicationCredit
}
import ru.auto.salesman.tariffs.credit_tariffs.Switch.toJavaProto
import ru.auto.salesman.tariffs.credit_tariffs.{
  ApplicationCreditTariffs,
  DealersWithActiveApplicationCredit,
  Switch,
  CreditTariff => ProtoCreditTariff,
  TariffScope => ProtoTariffScope
}

class CreditTariffHandlerSpec extends RoutingSpec {
  private val clientId = 123456

  def createRoute: (CreditTariffReadService, CreditTariffWriteService, Route) = {
    val service = mock[CreditTariffReadService]
    val opsService = mock[CreditTariffWriteService]
    val route = (new CreditTariffHandler(service, opsService)).route
    (service, opsService, route)
  }

  private val switch = toJavaProto(
    Switch(ProtoTariffScope.CARS_USED, ProtoCreditTariff.ACCESS)
  )

  "PUT /application-credit/tariff/client/{clientId}/turn-on" should {
    "return ok" in {
      val (service, opsService, route) = createRoute

      (opsService.turnOnTariff _)
        .expects(
          AutoruDealer(clientId),
          CreditTariff.Access,
          TariffScope.CarsUsed
        )
        .returningZ(())
        .once

      val uri = s"/tariff/client/$clientId/turn-on"

      Put(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(switch.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }

    }
  }

  "PUT /application-credit/tariff/client/{clientId}/turn-off" should {
    "return ok" in {
      val (service, opsService, route) = createRoute

      (opsService.turnOffTariff _)
        .expects(
          AutoruDealer(clientId),
          CreditTariff.Access,
          TariffScope.CarsUsed
        )
        .returningZ(())
        .once

      val uri = s"/tariff/client/$clientId/turn-off"
      Put(uri)
        .withHeaders(RequestIdentityHeaders)
        .withEntity(HttpEntity(switch.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }

    }
  }

  "GET /application-credit/tariff/clients" should {
    "return DealersWithActiveApplicationCredit" in {
      val uri = "/tariffs/clients"
      val (service, opsService, route) = createRoute

      (service.getDealersWithActiveApplicationCredit _)
        .expects()
        .returningZ(
          DealersWithActiveApplicationCredit.toJavaProto(
            DealersWithActiveApplicationCredit(Nil)
          )
        )
        .once

      Get(uri)
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[ProtoDealersWithActiveApplicationCredit]
        res.getGroupedDealersCount should equal(0)
      }

    }
  }

  "GET /application-credit/tariff/client/{clientId}" should {
    "return ApplicationCreditTariffs" in {
      val uri = s"/tariffs/client/$clientId"
      val (service, opsService, route) = createRoute

      (service.getTariffs _)
        .expects(AutoruDealer(clientId))
        .returningZ(
          ApplicationCreditTariffs.toJavaProto(ApplicationCreditTariffs(Nil))
        )
        .once

      Get(uri)
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[ProtoApplicationCreditTariffs]
        res.getTariffsCount should equal(0)
      }

    }
  }
}
