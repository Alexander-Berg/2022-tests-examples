package ru.yandex.vertis.story.api.handlers.story

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.Mockito.verify
import org.scalatest.{FunSuiteLike, Matchers}
import ru.yandex.vertis.baker.util.api.directives.RequestDirectives.wrapRequest
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.story.api.managers.story.StoriesManager
import ru.yandex.vertis.story.api.managers.xml.OfferXmlGenerator
import ru.yandex.vertis.story.api.model.StoriesContainer
import scala.concurrent.Future

class StoryHandlerSpec extends FunSuiteLike with Matchers with MockitoSupport with ScalatestRouteTest {

  val fileGenerationClient: OfferXmlGenerator = mock[OfferXmlGenerator]
  val storiesManager: StoriesManager = mock[StoriesManager]
  val storiesContainer: StoriesContainer = StoriesContainer(Seq.empty, "")

  when(storiesManager.get(?, ?)(?)).thenReturn(Future.successful(storiesContainer))
  when(fileGenerationClient.getXmlForVideo(?)).thenReturn(Future.successful(<story></story>))
  when(fileGenerationClient.getXmlForImage(?)).thenReturn(Future.successful(<story></story>))
  when(fileGenerationClient.getXmlForOffer(?)(?)).thenReturn(Future.successful(<story></story>))

  val route: Route = wrapRequest(new StoryHandler(storiesManager, fileGenerationClient).route)

  val httpRequestXml: HttpRequest =
    Get("/test_video/xml") ~>
      addHeader(Accept(MediaTypes.`application/xml`))

  val httpRequestStory: HttpRequest =
    Get("/test_video") ~>
      addHeader(Accept(MediaTypes.`application/json`))

  test("xml") {
    httpRequestXml ~> route ~> check {
      contentType.mediaType shouldBe MediaTypes.`application/xml`
      verify(fileGenerationClient).getXmlForVideo(?)
      status shouldBe StatusCodes.OK
    }
  }

  test("story") {
    httpRequestStory ~> route ~> check {
      contentType shouldBe ContentTypes.`application/json`
      verify(storiesManager).get(?, ?)(?)
      status shouldBe StatusCodes.OK
    }
  }
}
