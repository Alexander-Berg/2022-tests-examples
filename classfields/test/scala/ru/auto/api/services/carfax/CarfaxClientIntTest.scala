package ru.auto.api.services.carfax

import ru.auto.api.exceptions.{UnexpectedResponseException, VinResolutionNotFound}
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.OfferID
import ru.auto.api.model.vin.VinResolutionRequest
import ru.auto.api.services.HttpClientSuite

class CarfaxClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("carfax-api-http.vrts-slb.test.vertis.yandex.net", 80)

  private val carfaxClient = new CarfaxClient(http)

  test("check mark and model") {
    val res = carfaxClient.check("Z94CC41BBER184593", "HYUNDAI", "SOLARIS").futureValue
    res shouldBe Resolution.Ok
  }

  test("get offer resolution by offer id and vin") {
    val res = carfaxClient
      .getResolutionByVin(
        "Z8T4C5FS9BM005269",
        VinResolutionRequest(
          OfferID(1066271252, Some("e521c")),
          mark = Some("PEUGEOT"),
          model = Some("308"),
          year = Some(2011),
          powerHp = Some(120),
          displacement = Some(1600),
          bodyType = Some("SEDAN"),
          ownersCount = Some(2),
          sellerType = Some("PRIVATE"),
          color = Some("200204"),
          kmAge = Some(40000),
          price = Some(40000),
          userId = None,
          format = None,
          geobaseId = 1,
          None
        )
      )
      .futureValue
    res.getEntriesCount shouldNot be(0)
  }

  test("get offer resolution by offer id and license plate") {
    val res = carfaxClient
      .getResolutionByLicensePlate(
        "H116XP174",
        VinResolutionRequest(
          OfferID(1079527530, Some("940de")),
          mark = None,
          model = None,
          year = Some(2013),
          powerHp = Some(123),
          displacement = Some(1600),
          bodyType = Some("SEDAN"),
          ownersCount = Some(2),
          sellerType = Some("PRIVATE"),
          color = Some("200204"),
          kmAge = Some(40000),
          price = Some(40000),
          userId = None,
          format = None,
          geobaseId = 1,
          None
        )
      )
      .futureValue
    res.getEntriesCount shouldNot be(0)
  }

  test("throw vin resolution not found exception ") {
    val res = carfaxClient.getResolutionByVin(
      "ZZZ4Z5FS9BM005000",
      VinResolutionRequest(
        offerId = OfferID(0, None),
        mark = None,
        model = None,
        year = None,
        powerHp = None,
        displacement = None,
        bodyType = None,
        ownersCount = None,
        sellerType = None,
        color = None,
        kmAge = None,
        price = None,
        userId = None,
        format = None,
        geobaseId = 1,
        None
      )
    )
    res.failed.futureValue shouldBe an[VinResolutionNotFound]
  }

  test("response ok for vin") {
    val res = carfaxClient.reloadVinResolution("ZZZ4Z5FS9BM005269", "1114991796-c0786272", "dealer:123")
    res.futureValue shouldBe (())
  }

  test("response error for reload invalid") {
    val res = carfaxClient.reloadVinResolution("q1", "", "")
    res.failed.futureValue shouldBe an[UnexpectedResponseException]
  }

}
