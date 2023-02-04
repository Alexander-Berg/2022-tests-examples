package ru.yandex.vos2.watching.stages

import com.google.protobuf.StringValue
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.offer.VideoReview
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.realty.model.offer.YoutubeUrlGenerator._
import ru.yandex.vos2.watching.ProcessingState

@RunWith(classOf[JUnitRunner])
class ParseOfferDescriptionStageSpec extends WordSpec with Matchers with PropertyChecks {

  private val stage = ParseOfferDescriptionStage

  "ParseOfferDescriptionStage" should {

    "update nothing because youtube video already set" in {
      forAll(YoutubeUrlGen, VideoReviewGen) { (youtubeUrl, videoReview) =>
        val description = s"Bla $youtubeUrl"
        val preSetVideoReview = videoReview
        val offer = makeOffer(
          videoReview = preSetVideoReview,
          description = description
        )
        val state = stage.process(ProcessingState(offer, offer))
        state.offer.getOfferRealty.getVideoReview shouldBe preSetVideoReview
      }
    }

    "update video review field by link from description" in {
      forAll(YoutubeUrlGen, VideoReviewGen) { (youtubeUrl, videoReview) =>
        val url = youtubeUrl
        val description = s"Bla $url"
        val preSetVideoReview = videoReview.toBuilder.clearYoutubeVideoReviewUrl().build()
        val offer = makeOffer(
          videoReview = preSetVideoReview,
          description = description
        )
        val state = stage.process(ProcessingState(offer, offer))
        state.offer.getOfferRealty.getVideoReview.getYoutubeVideoReviewUrl.getValue shouldBe url
        state.offer.getOfferRealty.getVideoReview.getOnlineShow shouldBe preSetVideoReview.getOnlineShow
      }
    }

    "update video review field with video review empty string" in {
      forAll(YoutubeUrlGen, VideoReviewGen) { (youtubeUrl, videoReview) =>
        val url = youtubeUrl
        val description = s"Bla $url"
        val preSetVideoReview = videoReview.toBuilder
          .setYoutubeVideoReviewUrl(StringValue.of(""))
          .build()
        val offer = makeOffer(
          videoReview = preSetVideoReview,
          description = description
        )
        val state = stage.process(ProcessingState(offer, offer))
        state.offer.getOfferRealty.getVideoReview.getYoutubeVideoReviewUrl.getValue shouldBe url
        state.offer.getOfferRealty.getVideoReview.getOnlineShow shouldBe preSetVideoReview.getOnlineShow
      }
    }

    "not update video review because no link in description" in {
      forAll(VideoReviewGen) { videoReview =>
        val description = s"Bla bla bla"
        val preSetVideoReview = videoReview.toBuilder.clearYoutubeVideoReviewUrl().build()
        val offer = makeOffer(
          videoReview = preSetVideoReview,
          description = description
        )
        val state = stage.process(ProcessingState(offer, offer))
        state.offer.getOfferRealty.getVideoReview shouldBe preSetVideoReview
      }
    }

    "not update video review because wrong link on youtube 1" in {
      forAll(VideoReviewGen) { videoReview =>
        val description = s"Bla http://youtuber.com/watch?v=iwGFalTRHDA bla bla"
        val preSetVideoReview = videoReview.toBuilder.clearYoutubeVideoReviewUrl().build()
        val offer = makeOffer(
          videoReview = preSetVideoReview,
          description = description
        )
        val state = stage.process(ProcessingState(offer, offer))
        state.offer.getOfferRealty.getVideoReview shouldBe preSetVideoReview
      }
    }

    "not update video review because wrong link on youtube 2" in {
      val description = s"Bla http://youtube.site.com/watch?v=iwGFalTRHDA bla bla"
      val preSetVideoReview = VideoReview.newBuilder().build()
      val offer = makeOffer(
        videoReview = preSetVideoReview,
        description = description
      )
      val state = stage.process(ProcessingState(offer, offer))
      state.offer.getOfferRealty.getVideoReview shouldBe preSetVideoReview
    }

    "not update video review because wrong link on youtube 3" in {
      val description = s"Bla youtude.com/watch?v=iwGFalTRHDA bla bla"
      val preSetVideoReview = VideoReview.newBuilder().build()
      val offer = makeOffer(
        videoReview = preSetVideoReview,
        description = description
      )
      val state = stage.process(ProcessingState(offer, offer))
      state.offer.getOfferRealty.getVideoReview shouldBe preSetVideoReview
    }
  }

  private def makeOffer(
    videoReview: VideoReview,
    description: String
  ): OfferModel.Offer = {
    val builder = TestUtils.createOffer()
    builder.setDescription(description)
    builder.setOfferRealty(
      builder.getOfferRealty.toBuilder.setVideoReview {
        videoReview
      }
    )
    builder.build()
  }
}
