package ru.yandex.vertis.parsing.api.routes.v1.photo

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.api.managers.PhotoManager
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.util.api.directives.RequestDirectives

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class PhotoHandlerSpec extends FunSuite with Matchers with ScalatestRouteTest with MockitoSupport {
  private val photoManager = mock[PhotoManager]
  private val handler = new PhotoHandler(photoManager)

  private val route = RequestDirectives.wrapRequest {
    handler.route
  }

  test("GET /") {
    val offerId = "1111-hash"
    val mdsNamespace = "autoru-all"
    val photoUrl = testAvitoPhotoUrl
    when(photoManager.getOrAddPhoto(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(""))
    Get(s"/$mdsNamespace/$offerId?url=$photoUrl&retry=1") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(photoManager).getOrAddPhoto(eq(offerId), eq(Category.TRUCKS), eq(mdsNamespace), eq(photoUrl), eq(true))(?)
  }

  test("GET /trucks") {
    val offerId = "1111-hash"
    val mdsNamespace = "autoru-all"
    val photoUrl = testAvitoPhotoUrl
    when(photoManager.getOrAddPhoto(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(""))
    Get(s"/$mdsNamespace/trucks/$offerId?url=$photoUrl&retry=1") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(photoManager).getOrAddPhoto(eq(offerId), eq(Category.TRUCKS), eq(mdsNamespace), eq(photoUrl), eq(true))(?)
  }

  test("GET /cars") {
    val offerId = "1111-hash"
    val mdsNamespace = "autoru-all"
    val photoUrl = testAvitoPhotoUrl
    when(photoManager.getOrAddPhoto(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(""))
    Get(s"/$mdsNamespace/cars/$offerId?url=$photoUrl&retry=1") ~> route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    verify(photoManager).getOrAddPhoto(eq(offerId), eq(Category.CARS), eq(mdsNamespace), eq(photoUrl), eq(true))(?)
  }
}
