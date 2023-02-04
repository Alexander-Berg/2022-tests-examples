package ru.yandex.vertis.story.api.handlers.stories

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.mockito.Mockito.verify
import org.scalatest.{FunSuiteLike, Matchers}
import ru.yandex.vertis.baker.util.api.directives.RequestDirectives.wrapRequest
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.story.api.managers.story.StoriesManager
import ru.yandex.vertis.story.api.model.Story.Image
import ru.yandex.vertis.story.api.model.{StoriesContainer, Story}

import scala.concurrent.Future

class StoriesHandlerSpec extends FunSuiteLike with Matchers with MockitoSupport with ScalatestRouteTest {

  val storiesManager: StoriesManager = mock[StoriesManager]

  val story: Story = Story(
    id = "test_video",
    version = 1,
    pages = 1,
    image = "https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg",
    imageFull = "https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg",
    imagePreview = Some(
      "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
    ),
    image_sizes = Some(
      Image(
        Some(
          "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
        ),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x1.jpg"),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x2.jpg"),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg")
      )
    ),
    image_full_sizes = Some(
      Image(
        Some(
          "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
        ),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@1x.jpg"),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@2x.jpg"),
        Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@3x.jpg")
      )
    ),
    nativeStory = "autoru.api.host/1.0/story/test_video/xml?token=story-xml",
    jsonStory = None,
    raw = None,
    background = "#ffffff",
    text = "#ffffff",
    title = "Российские новинки недели",
    geo = None,
    tags = None,
    card_id = None,
    card_category = None,
    `x-android-app-version` = None,
    `x-ios-app-version` = None,
    `x-ios-app-version-to` = None,
    `x-android-app-version-to` = None
  )

  private def storiesContainer: StoriesContainer = {
    val secondStory = story.copy(id = "test_video2")
    val thirdStory = story.copy(id = "test_video3")
    StoriesContainer(Seq(story, secondStory, thirdStory), "")
  }

  when(storiesManager.search(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(storiesContainer))

  val httpRequest: HttpRequest =
    Get("/") ~> addHeader(Accept(MediaTypes.`application/json`))
  val route: Route = wrapRequest(new StoriesHandler(storiesManager).route)

  test("stories") {
    httpRequest ~> route ~> check {
      contentType shouldBe ContentTypes.`application/json`
      verify(storiesManager).search(?, ?, ?, ?, ?, ?, ?, ?)(?)
      status shouldBe StatusCodes.OK
    }
  }
}
