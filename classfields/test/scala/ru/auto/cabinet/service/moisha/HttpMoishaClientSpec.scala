package ru.auto.cabinet.service.moisha

import com.typesafe.config.ConfigFactory
import org.mockito.Mockito.{mock => _, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.Category.{CARS, CATEGORY_UNKNOWN}
import ru.auto.api.ApiOfferModel.Section.NEW
import ru.auto.cabinet.dao.jdbc.JdbcClientDao
import ru.auto.cabinet.model.moisha.Products._
import ru.auto.cabinet.model.moisha.TransportExtractException
import ru.auto.cabinet.model.offer.buildOffer
import ru.auto.cabinet.model.{Client, ClientProperties, ClientStatuses}
import ru.auto.cabinet.service.moishaPoint
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.{environment, TestActorSystem}

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

//noinspection TypeAnnotation
class HttpMoishaClientSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with TestActorSystem {

  System.setProperty("config.resource", "test.conf")
  implicit private val rc = Context.unknown

  val moishaConfig = MoishaConfig(
    ConfigFactory.load().getString("cabinet.autoru.office.moisha.uri"))
  val clientDao = mock[JdbcClientDao]
  val client = new HttpMoishaClient(moishaConfig)(clientDao)

  val from = OffsetDateTime.parse("2017-08-12T00:00:00.000+03:00")
  val to = OffsetDateTime.parse("2017-08-12T23:59:59.999+03:00")

  "Moisha client" should "get premium price for new car" in {
    val clientId = 1234
    val regionId = 1
    val cityId = 1
    val offer = buildOffer(builder => {
      builder.setUserRef(s"dealer:$clientId")
      builder.getPriceInfoBuilder.setPrice(2415329f)
      builder.getAdditionalInfoBuilder.setCreationDate(1501152085000L)
      builder.setSection(NEW).setCategory(CARS)
    })

    when(clientDao.get(clientId)).thenReturn(
      Future.successful(
        Client(
          clientId,
          properties = ClientProperties(
            regionId,
            cityId,
            "test",
            ClientStatuses.Active,
            environment.now,
            "",
            Some("website.test"),
            "test@yandex.ru",
            Some("manager@yandex.ru"),
            Some(from),
            None,
            multipostingEnabled = true,
            firstModerated = true,
            isAgent = false
          )
        )
      ))

    implicit val patienceConfig = PatienceConfig(Span(10, Seconds))
    client
      .getPrices(offer, Seq(Premium, Boost, Special), from, to)
      .futureValue should contain only (
      moishaPoint(Premium, 25000),
      moishaPoint(Boost, 15000),
      moishaPoint(Special, 5900)
    )
  }

  it should "throw error in case of bad offer category" in {
    val offer = buildOffer(_.setCategory(CATEGORY_UNKNOWN))
    val cause =
      Try(
        client
          .getPrices(offer, Seq(Premium), from, to)
          .futureValue).failed.get.getCause
    cause shouldBe a[TransportExtractException]
  }

}
