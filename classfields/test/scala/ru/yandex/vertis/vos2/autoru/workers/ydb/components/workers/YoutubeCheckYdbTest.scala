package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.yandex.ydb.ValueProtos.Type.PrimitiveTypeId
import com.yandex.ydb.ValueProtos.{Column, ResultSet, Type}
import com.yandex.ydb.table.YdbTable.ExecuteQueryResult
import com.yandex.ydb.table.query.DataQueryResult
import com.yandex.ydb.table.values.PrimitiveValue
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.result.{ResultSetWrapper, ResultSetWrapperImpl}
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Video, YoutubeVideo}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.youtube.YoutubeClient
import ru.yandex.vos2.getNow

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.DurationInt
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class YoutubeCheckYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val offer: Offer

    val client = mock[YoutubeClient]

    val youtubeWorker = new YoutubeCheckYdb(
      client
    ) with YdbWorkerTestImpl
  }

  def nextCheckAnswer(dateTime: DateTime): Iterator[ResultSetWrapper] = {
    val epoch = dateTime.getMillis / 1000
    val resultSetsList = ExecuteQueryResult
      .newBuilder()
      .addResultSets(
        ResultSet
          .newBuilder()
          .addColumns(
            Column
              .newBuilder()
              .setName("next_check")
              .setType(Type.newBuilder().setTypeId(PrimitiveTypeId.TIMESTAMP))
              .build()
          )
          .addRows(
            PrimitiveValue.timestamp(epoch).toPb
          )
      )
      .build()
      .getResultSetsList
    val data = new DataQueryResult("2", resultSetsList)
    val resultSet: ResultSetWrapper = new ResultSetWrapperImpl(
      data.getResultSet(0)
    )
    Iterator(
      resultSet
    )
  }

  "YoutubeCheckYdbTest YDB" should {

    "change youtube videos statuses " in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      val video1 = createYoutubeVideo("1", getNow - 2.days.toMillis, YoutubeVideoStatus.UNAVAILABLE)
      val video2 = createYoutubeVideo("2", getNow - 2.days.toMillis, YoutubeVideoStatus.UNKNOWN)
      builder.getOfferAutoruBuilder.addAllVideo(Seq(video1, video2).asJava)
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.AVAILABLE)

      assert(youtubeWorker.shouldProcess(offer, None).shouldProcess)
      val result = youtubeWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)

      result.nextCheck.isEmpty &&
        updatedOffer.getOfferAutoru.getVideo(0).getYoutubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE &&
        updatedOffer.getOfferAutoru.getVideo(1).getYoutubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE
    }

    "set delay 16 hours for available videos " in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      val video = createYoutubeVideo("1", getNow - 2.days.toMillis, YoutubeVideoStatus.AVAILABLE)
      builder.getOfferAutoruBuilder.addVideo(video)
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.AVAILABLE)

      val result = youtubeWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)
      val newYoutubeVideo = updatedOffer.getOfferAutoru.getVideo(0).getYoutubeVideo
      assert(newYoutubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE)
      assert(newYoutubeVideo.getLastCheckMoment != 0)
      result.nextCheck.get.getMillis shouldBe new DateTime().plus(16.hours.toMillis).getMillis +- 1000

    }
    "transit from unknown to available status if unknown status from check service received " in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      val video = createYoutubeVideo("1", getNow - 2.days.toMillis, YoutubeVideoStatus.UNKNOWN)
      builder.getOfferAutoruBuilder.addVideo(video)
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.UNKNOWN)

      val result = youtubeWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)

      val newYoutubeVideo = updatedOffer.getOfferAutoru.getVideo(0).getYoutubeVideo
      assert(newYoutubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE)
      assert(newYoutubeVideo.getLastCheckMoment != 0)
      result.nextCheck.get.getMillis shouldBe new DateTime().plus(16.hours.toMillis).getMillis +- 1000

    }
    "so not update status if youtube check failed " in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      val video = createYoutubeVideo("1", getNow - 2.days.toMillis, YoutubeVideoStatus.AVAILABLE)
      builder.getOfferAutoruBuilder.addVideo(video)
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.UNKNOWN)

      val result = youtubeWorker.process(offer, None)
      val updatedOffer = result.updateOfferFunc.get(offer)

      val newYoutubeVideo = updatedOffer.getOfferAutoru.getVideo(0).getYoutubeVideo
      assert(newYoutubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE)
      assert(newYoutubeVideo.getLastCheckMoment != 0)
      result.nextCheck.get.getMillis shouldBe new DateTime().plus(16.hours.toMillis).getMillis +- 1000

    }
    "set delay 6 hours " in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      builder.getOfferAutoruBuilder.addAllVideo(
        Seq(createYoutubeVideo("1", getNow - 3.hours.toMillis), createYoutubeVideo("2", getNow - 10.hours.toMillis)).asJava
      )
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.AVAILABLE)
      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.AVAILABLE)

      val result = youtubeWorker.process(offer, None)
      assert(result.updateOfferFunc.isEmpty)
      result.nextCheck.get.getMillis shouldBe new DateTime().plus(6.hours.toMillis).getMillis +- 1000
    }

    "don't process offers without youtube videos" in new Fixture {
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.clearVideo()
      val offer = builder.build()

      when(client.checkVideoAvailable(?)).thenReturn(YoutubeVideoStatus.UNKNOWN)

      assert(youtubeWorker.shouldProcess(offer, None).shouldProcess === false)

    }

  }

  private def createYoutubeVideo(id: String,
                                 lastCheck: Long,
                                 status: YoutubeVideoStatus = YoutubeVideoStatus.AVAILABLE): Video = {
    val builder = Video.newBuilder()
    builder.setCreated(123L).setUpdated(123L)
    val youtubeBuilder = YoutubeVideo.newBuilder()
    youtubeBuilder.setLastCheckMoment(lastCheck)
    youtubeBuilder.setYoutubeId(id)
    youtubeBuilder.setStatus(status)
    builder.setYoutubeVideo(youtubeBuilder)
    builder.build()
  }
}
