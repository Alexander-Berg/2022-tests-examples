package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.api.ApiOfferModel.{Offer, State}
import ru.auto.api.CommonModel.Photo
import ru.auto.cabinet.model.{
  Location,
  OfferCategories,
  OfferSections,
  SaleInfo
}
import ru.auto.cabinet.service.vos.VosClient
import ru.auto.cabinet.test.JdbcSpecTemplate
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito._

import java.time.OffsetDateTime
import scala.concurrent.Future

class JdbcCarSalesDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {
  private val saleId = 1L

  private val vosClient = mock[VosClient]

  private val offer = Offer
    .newBuilder()
    .setId(s"$saleId-abcd")
    .setState(
      State
        .newBuilder()
        .addImageUrls(Photo.newBuilder().setName("first").build())
        .addImageUrls(Photo.newBuilder().setName("second").build())
        .addImageUrls(Photo.newBuilder().setName("third").build())
        .addImageUrls(Photo.newBuilder().setName("fourth").build())
        .addImageUrls(Photo.newBuilder().setName("fives").build())
        .build())
    .build()

  private def saleInfo(pictureCount: Option[Int] = Some(5)) = SaleInfo(
    client1Id,
    saleId,
    OfferCategories.Motorcycle,
    OfferSections.Used,
    Some(Location(1, Some(1))),
    1,
    1,
    OffsetDateTime.parse("2019-01-17T17:22:06.602+03:00"),
    2019,
    100,
    Some("vin"),
    pictureCount
  )

  private val carSalesDao =
    new JdbcCarSalesDao(office7Database, office7Database, vosClient)

  "JdbcCarSalesDao" should {

    "create SaleInfo success" in {
      when(vosClient.getOffer(saleId.toString))
        .thenReturn(Future.successful(Some(offer)))
      for {
        sales <- carSalesDao.getRecentClientSales(
          client1Id,
          OffsetDateTime.parse("2019-01-17T17:22:06.602+03:00"))
      } yield sales.headOption.get shouldBe saleInfo()
    }

    "when offer is not found" in {
      when(vosClient.getOffer(saleId.toString))
        .thenReturn(Future.successful(None))
      for {
        sales <- carSalesDao.getRecentClientSales(
          client1Id,
          OffsetDateTime.parse("2019-01-17T17:22:06.602+03:00"))
      } yield sales.headOption.get shouldBe saleInfo(pictureCount = None)
    }
  }
}
