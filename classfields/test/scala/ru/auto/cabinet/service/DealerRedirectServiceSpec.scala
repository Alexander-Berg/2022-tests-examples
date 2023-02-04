package ru.auto.cabinet.service

import java.time.LocalDateTime
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import org.scalatest.OneInstancePerTest
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.auto.api.ApiOfferModel
import ru.auto.api.search.SearchModel.State
import ru.auto.api.CarsModel.CarInfo
import ru.auto.cabinet.service.public_api.HttpPublicApiClient
import ru.auto.cabinet.service.dealer_pony.DealerPonyClient
import ru.auto.cabinet.service.telepony.{Phone, Redirect, TeleponyClient}
import ru.auto.cabinet.ApiModel.{DealerRedirect, GetDealerRedirectsResult}
import ru.auto.cabinet.service.geobase.GeoBaseClient
import ru.auto.dealer_pony.proto.ApiModel.TeleponyInfoResponse
import ru.auto.api.ApiOfferModel.TeleponyInfo
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DealerRedirectServiceSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with OneInstancePerTest {
  private def ?[T]: T = any()
  implicit private val rc = Context.unknown

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  val publicApiClient: HttpPublicApiClient = mock[HttpPublicApiClient]
  val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
  val teleponyClient: TeleponyClient = mock[TeleponyClient]
  val geobaseClient: GeoBaseClient = mock[GeoBaseClient]

  val objectId = "objectId"
  val tag = "tag"

  val qualifier = TeleponyInfo
    .newBuilder()
    .setObjectId(objectId)
    .setTag(tag)
    .build()

  val dealerPonyResponse = TeleponyInfoResponse
    .newBuilder()
    .setResponse(qualifier)
    .build()

  val dealerId = "95"
  val salonCode = "radiator_springs"

  val mark = "Lightning"
  val model = "McQueen"

  val carInfo: CarInfo =
    CarInfo.newBuilder().setMark(mark).setModel(model).build()

  "getDealerRedirects" should "return DealerRedirects for given mark and model " in {
    val phoneSource = Phone("+70000000000")
    val phoneTarget = Phone("+79999999999")
    val salon = ApiOfferModel.Salon
      .newBuilder()
      .setDealerId(dealerId)
      .setCode(salonCode)
      .addPhones(
        ApiOfferModel.Phone
          .newBuilder()
          .setPhone(phoneSource.value)
          .build())
      .build()
    val carInfo = CarInfo.newBuilder().setMark(mark).setModel(model).build()
    val offer = ApiOfferModel.Offer
      .newBuilder()
      .setSalon(salon)
      .setCarInfo(carInfo)
      .build()
    val redirect =
      Redirect(
        "",
        phoneSource,
        phoneTarget,
        LocalDateTime.now,
        objectId,
        Some(tag))

    when(publicApiClient.getSalons(mark, ApiOfferModel.Category.CARS))
      .thenReturn(Source.single(salon))
    when(publicApiClient.getFirstOffer(dealerId, mark, model, State.NEW))
      .thenReturn(Future.successful(offer))
    when(dealerPonyClient.getTeleponyInfoByOffer(offer))
      .thenReturn(Future.successful(dealerPonyResponse))
    when(teleponyClient
      .getOrCreate(phoneSource, qualifier.getObjectId, Some(qualifier.getTag)))
      .thenReturn(Future.successful(redirect))
    when(geobaseClient.in(?, ?)(?))
      .thenReturn(Future.successful(true))

    val dealerRedirect = DealerRedirect
      .newBuilder()
      .setMark(mark)
      .setModel(model)
      .setRedirectSource(redirect.source.value)
      .setRedirectTarget(redirect.target.value)
      .setUrl(
        s"https://auto.ru/diler-oficialniy/cars/new/$salonCode/${mark.toLowerCase}")
      .build()

    val result = GetDealerRedirectsResult
      .newBuilder()
      .addDealerRedirects(dealerRedirect)
      .build()

    new DealerRedirectService(
      publicApiClient,
      dealerPonyClient,
      teleponyClient,
      geobaseClient)
      .getDealerRedirects(mark, model)
      .futureValue shouldBe result
  }

  "getDealerRedirects" should "retry and skip failed phones " in {
    val phoneFailed = Phone("+77777777777")

    val salon = ApiOfferModel.Salon
      .newBuilder()
      .setDealerId(dealerId)
      .setCode(salonCode)
      .addPhones(
        ApiOfferModel.Phone
          .newBuilder()
          .setPhone(phoneFailed.value)
          .build())
      .build()
    val offer = ApiOfferModel.Offer
      .newBuilder()
      .setSalon(salon)
      .setCarInfo(carInfo)
      .build()

    when(publicApiClient.getSalons(mark, ApiOfferModel.Category.CARS))
      .thenReturn(Source.single(salon))
    when(publicApiClient.getFirstOffer(dealerId, mark, model, State.NEW))
      .thenReturn(Future.successful(offer))
    when(dealerPonyClient.getTeleponyInfoByOffer(offer))
      .thenReturn(Future.successful(dealerPonyResponse))
    when(teleponyClient.getOrCreate(phoneFailed, qualifier.getObjectId, None))
      .thenReturn(Future.failed(new Exception("Invalid phone format")))
    when(geobaseClient.in(?, ?)(?))
      .thenReturn(Future.successful(false))

    val result = GetDealerRedirectsResult.newBuilder().build()

    new DealerRedirectService(
      publicApiClient,
      dealerPonyClient,
      teleponyClient,
      geobaseClient)
      .getDealerRedirects(mark, model)
      .futureValue shouldBe result

    Mockito
      .verify(
        teleponyClient,
        Mockito.atLeast(DealerRedirectService.RetryAttempts + 1))
      .getOrCreate(phoneFailed, qualifier.getObjectId, None)
  }
}
