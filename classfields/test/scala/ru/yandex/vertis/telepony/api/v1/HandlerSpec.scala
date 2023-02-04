package ru.yandex.vertis.telepony.api.v1

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.{RequestDirectives, RouteTest}
import ru.yandex.vertis.telepony.geo.RegionGeneralizerService
import ru.yandex.vertis.telepony.geo.model.RegionType
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.PhoneService

import scala.concurrent.Future

/**
  * @author evans
  */
class HandlerSpec extends RouteTest with MockitoSupport {

  private def buildRoute(ps: PhoneService, rgs: RegionGeneralizerService): Route = {
    RequestDirectives.wrapRequest {
      RequestDirectives.seal(
        new UtilHandler {
          override def phoneService: PhoneService = ps

          override def regionGeneralizer: RegionGeneralizerService = rgs
        }.route
      )
    }
  }

  "Handler" should {
    "handle" in {
      val rawPhone = "89312320032"
      val uri = Uri("/phone/unify").withQuery(Query("raw-phone" -> rawPhone))
      val expectedResponse = PhoneInfo(Phone("+79312320032"), 1, PhoneTypes.Local, 1, Operators.Beeline.toString)
      val phoneService = mock[PhoneService]
      when(phoneService.provideInfo(eq(rawPhone))).thenReturn(Future.successful(Some(expectedResponse)))
      val route = buildRoute(phoneService, mock[RegionGeneralizerService])
      Get(uri) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        import ru.yandex.vertis.telepony.api.v1.json.SprayPhoneInfoView.modelUnmarshaller
        responseAs[PhoneInfo] shouldEqual expectedResponse
      }
    }
    "fail handle" when {
      "turkish phone number passed" in {
        val rawPhone = "+905340775966"
        val uri = Uri("/phone/unify").withQuery(Query("raw-phone" -> rawPhone))
        val phoneService = mock[PhoneService]
        when(phoneService.provideInfo(eq(rawPhone))).thenReturn(Future.successful(None))
        val route = buildRoute(phoneService, mock[RegionGeneralizerService])
        Get(uri) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
        }
      }
    }
    "generalize region" in {
      val spbRegion = 2
      val regionType = RegionType.COUNTRY
      val russiaRegion = 225
      val uri = Uri(s"/region/generalize/$spbRegion/${regionType.name()}")
      val regionGeneralizer = mock[RegionGeneralizerService]
      when(regionGeneralizer.generalizeTo(eq(spbRegion), eq(regionType)))
        .thenReturn(Future.successful(Some(russiaRegion)))
      val route = buildRoute(mock[PhoneService], regionGeneralizer)
      Get(uri) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        import ru.yandex.vertis.telepony.api.v1.json.SprayRegionView.modelUnmarshaller
        responseAs[GeoId] shouldEqual russiaRegion
      }
    }
    "return 404 when cant generalize region" in {
      val spbRegion = 2
      val regionType = RegionType.SUBJECT_FEDERATION_DISTRICT
      val uri = Uri(s"/region/generalize/$spbRegion/${regionType.name()}")
      val regionGeneralizer = mock[RegionGeneralizerService]
      when(regionGeneralizer.generalizeTo(eq(spbRegion), eq(regionType)))
        .thenReturn(Future.successful(None))
      val route = buildRoute(mock[PhoneService], regionGeneralizer)
      Get(uri) ~> route ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
  }
}
