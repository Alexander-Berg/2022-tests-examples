package ru.auto.api.services.video

import akka.http.scaladsl.model.HttpMethods.GET
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.{Paging, RequestParams}
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class YandexVideoClientSpec extends HttpClientSpec with MockedHttpClient with MockitoSupport {

  "Yandex video client" should {
    "search for video, feature is off" in {
      val featureManager: FeatureManager = mock[FeatureManager]
      val feature: Feature[Boolean] = mock[Feature[Boolean]]
      when(feature.value).thenReturn(false)
      when(featureManager.yandexVideoPreviews).thenReturn(feature)
      val videoClient = new YandexVideoClient(http, featureManager)

      implicit val trace: Traced = Traced.empty
      implicit val request: RequestImpl = {
        val req = new RequestImpl
        req.setApplication(Application.desktop)
        req.setTrace(trace)
        req.setRequestParams(RequestParams.empty)
        req
      }

      http.expectUrl(GET, "/video/result?text=BMW&client=auto.ru&numdoc=10&p=0")
      http.respondWithJsonFrom("/video/search.json")

      val result = videoClient.search("BMW", Paging(1, 10)).futureValue.getVideosList.asScala
      result.length shouldBe 10
      result.head.getYoutubeId shouldBe "5CXAfomgG_M"
      result.head.getDurationInSeconds shouldBe 1502
      result.head.getPreviewsMap.get("thumb") shouldBe "https://i.ytimg.com/vi/5CXAfomgG_M/0.jpg"
      result.head.getPreviewsMap.get("mqdefault") shouldBe "https://i.ytimg.com/vi/5CXAfomgG_M/mqdefault.jpg"
    }

    "search for video, feature is on" in {
      val featureManager: FeatureManager = mock[FeatureManager]
      val feature: Feature[Boolean] = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.yandexVideoPreviews).thenReturn(feature)
      val videoClient = new YandexVideoClient(http, featureManager)

      implicit val trace: Traced = Traced.empty
      implicit val request: RequestImpl = {
        val req = new RequestImpl
        req.setApplication(Application.desktop)
        req.setTrace(trace)
        req.setRequestParams(RequestParams.empty)
        req
      }

      http.expectUrl(GET, "/video/result?text=BMW&client=auto.ru&numdoc=10&p=0")
      http.respondWithJsonFrom("/video/search.json")

      val result = videoClient.search("BMW", Paging(1, 10)).futureValue.getVideosList.asScala

      result.length shouldBe 10
      result.head.getYoutubeId shouldBe "5CXAfomgG_M"
      result.head.getDurationInSeconds shouldBe 1502
      result.head.getPreviewsMap
        .get("thumb") shouldBe "https://avatars.mds.yandex.net/get-video_thumb_fresh/936936/843a5899614b12b1674771d158ae27220121/400x300"
      result.head.getPreviewsMap
        .get("mqdefault") shouldBe "https://avatars.mds.yandex.net/get-video_thumb_fresh/936936/843a5899614b12b1674771d158ae27220121/320x180"
    }
  }
}
