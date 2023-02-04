package ru.yandex.vos2.autoru.api.v1.salon

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.{Vos2ApiHandlerResponses, Vos2ApiSuite}

class SalonHandlerTest extends AnyFunSuite with InitTestDbs with Vos2ApiSuite with BeforeAndAfterAll {

  test("salon found") {
    val saleId = 9542L
    checkSimpleSuccessRequest(Get(s"/api/v1/salon/$saleId"))
  }

  test("salon not found") {
    val saleId = 10L
    checkErrorRequest(Get(s"/api/v1/salon/$saleId"), StatusCodes.NotFound, Vos2ApiHandlerResponses.salonNotFoundError)
  }

}
