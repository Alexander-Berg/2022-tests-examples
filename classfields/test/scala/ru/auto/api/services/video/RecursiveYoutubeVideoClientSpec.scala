package ru.auto.api.services.video

import org.mockito.ArgumentMatchers._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.Video
import ru.auto.api.ResponseModel.VideoListingResponse
import ru.auto.api.auth.Application
import ru.auto.api.model.{ModelGenerators, Paging, RequestParams}
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import org.mockito.Mockito._

import scala.jdk.CollectionConverters._
import ru.auto.api.model.gen.BasicGenerators._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class RecursiveYoutubeVideoClientSpec extends BaseSpec with MockitoSupport with BeforeAndAfter {
  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  private val yandexVideoClient: VideoClient = mock[VideoClient]
  private val videoClient: VideoClient = new VideoClientWrapper(yandexVideoClient) with RecursiveYoutubeVideoClient

  "return only youtube videos" in {
    val youtube = Gen.listOfN(2, ModelGenerators.VideoGen.filter(_.getVideoIdCase == Video.VideoIdCase.YOUTUBE_ID)).next
    val yandex = Gen.listOfN(8, ModelGenerators.VideoGen.filter(_.getVideoIdCase == Video.VideoIdCase.YANDEX_ID)).next
    val resp = VideoListingResponse
      .newBuilder()
      .addAllVideos(youtube.asJava)
      .addAllVideos(yandex.asJava)
      .build()

    val pagings = ArrayBuffer[Paging]()
    stub(yandexVideoClient.search(_: String, _: Paging)(_: Request)) {
      case (_, p, _) =>
        pagings += p
        Future.successful(resp)
    }
    val query = readableString.next
    val result = videoClient.search(query, Paging(1, 10)).futureValue

    val videos = result.getVideosList.asScala

    videos should have size 10

    videos.foreach(video => video.getVideoIdCase shouldBe Video.VideoIdCase.YOUTUBE_ID)

    verify(yandexVideoClient, times(5)).search(eq(query), argThat[Paging](p => p.pageSize == 30))(?)

    (pagings.map(_.page).toList should contain).theSameElementsInOrderAs(List(1, 2, 3, 4, 5))
  }
}
