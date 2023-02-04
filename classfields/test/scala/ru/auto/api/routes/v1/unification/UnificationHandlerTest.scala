package ru.auto.api.routes.v1.unification

import akka.http.scaladsl.model.StatusCodes
import ru.auto.api.ApiSpec
import ru.auto.api.services.MockedClients
import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}

import scala.concurrent.Future

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 27.02.17
  */
class UnificationHandlerTest extends ApiSpec with MockedClients {

  val simplePayload: CarsUnificationCollection = CarsUnificationCollection
    .newBuilder()
    .addEntries {
      CarsUnificationEntry
        .newBuilder()
        .setRawMark("audi")
        .setRawModel("a3")
        .build()
    }
    .build()

  "/1.0/unification/cars" should {

    when(searcherClient.unifyCars(?)(?)).thenReturn(Future.successful(simplePayload))

    "be implemented" in {
      Post("/1.0/unification/cars", simplePayload) ~>
        addHeader("x-authorization", "Vertis swagger") ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[CarsUnificationCollection] shouldBe simplePayload
        }
    }
    "check grant" in {
      Post("/1.0/unification/cars") ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }
}
