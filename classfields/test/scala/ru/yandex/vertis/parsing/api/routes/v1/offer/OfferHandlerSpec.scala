package ru.yandex.vertis.parsing.api.routes.v1.offer

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.api.managers.offers.OffersManager
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.auto.workers.importers.AutoImportResultImpl
import ru.yandex.vertis.parsing.common.UrlOrHashOrId
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.api.directives.RequestDirectives

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class OfferHandlerSpec extends FunSuite with Matchers with ScalatestRouteTest with MockitoSupport {
  //private val components = TestApiComponents

  private val offersManager = mock[OffersManager]
  private val handler = new OfferHandler(offersManager)

  private val route = RequestDirectives.wrapRequest {
    handler.route
  }

  test("GET /full") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = TestDataUtils.testRow(url)
    val hash = row.hash
    when(offersManager.getFullData(?)(?)).thenReturn(Future.successful(row.data))
    Get(s"/full?hash=$hash") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).getFullData(eq(UrlOrHashOrId.Hash(hash)))(?)
  }

  test("GET /full by url") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = TestDataUtils.testRow(url)
    when(offersManager.getFullData(?)(?)).thenReturn(Future.successful(row.data))
    Get(s"/full?url=$url") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).getFullData(eq(UrlOrHashOrId.Url(url)))(?)
  }

  test("GET /full by id") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = TestDataUtils.testRow(url)
    val id = row.data.getOffer.getAdditionalInfo.getRemoteId
    when(offersManager.getFullData(?)(?)).thenReturn(Future.successful(row.data))
    Get(s"/full?id=$id") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).getFullData(eq(UrlOrHashOrId.Id(id)))(?)
  }

  test("POST /") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = ImportRow(url, Json.obj())
    when(offersManager.addOffer(?, ?)(?)).thenReturn(Future.successful(AutoImportResultImpl()))
    Post(
      s"/",
      Json
        .obj(
          "url" -> url,
          "data" -> "{}"
        )
        .toString()
    ) ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).addOffer(eq(Category.TRUCKS), eq(row))(?)
  }

  test("POST /trucks") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = ImportRow(url, Json.obj())
    when(offersManager.addOffer(?, ?)(?)).thenReturn(Future.successful(AutoImportResultImpl()))
    Post(
      s"/trucks",
      Json
        .obj(
          "url" -> url,
          "data" -> "{}"
        )
        .toString()
    ) ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).addOffer(eq(Category.TRUCKS), eq(row))(?)
  }

  test("POST /cars") {
    val url = TestDataUtils.testAvitoCarsUrl
    val row = ImportRow(url, Json.obj())
    when(offersManager.addOffer(?, ?)(?)).thenReturn(Future.successful(AutoImportResultImpl()))
    Post(
      s"/cars",
      Json
        .obj(
          "url" -> url,
          "data" -> "{}"
        )
        .toString()
    ) ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).addOffer(eq(Category.CARS), eq(row))(?)
  }

  test("GET /photo: provide published phones") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = TestDataUtils.testRow(url)
    val hash = row.hash
    val offerId = "100500-hash"
    val phone1 = "phone1"
    val phone2 = "phone2"
    when(offersManager.getPhotos(?, ?, ?)(?)).thenReturn(Future.successful(row.data))
    Get(s"/photo?hash=$hash&offer-id=$offerId&phone=$phone1&phone=$phone2") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).getPhotos(eq(UrlOrHashOrId.Hash(hash)), eq(Some(offerId)), eq(Seq(phone1, phone2)))(?)
  }

  test("GET /photo: no published phones provided") {
    val url = TestDataUtils.testAvitoTrucksUrl
    val row = TestDataUtils.testRow(url)
    val hash = row.hash
    val offerId = "100500-hash"
    when(offersManager.getPhotos(?, ?, ?)(?)).thenReturn(Future.successful(row.data))
    Get(s"/photo?hash=$hash&offer-id=$offerId") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(offersManager).getPhotos(eq(UrlOrHashOrId.Hash(hash)), eq(Some(offerId)), eq(Seq()))(?)
  }
}
