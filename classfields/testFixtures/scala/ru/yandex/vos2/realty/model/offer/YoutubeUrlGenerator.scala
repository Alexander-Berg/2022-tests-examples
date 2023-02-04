package ru.yandex.vos2.realty.model.offer

import com.google.protobuf.{BoolValue, StringValue}
import org.scalacheck.Gen
import ru.yandex.realty.proto.offer.VideoReview
import ru.yandex.vos2.model.CommonGen._

object YoutubeUrlGenerator {

  val YoutubeUrlGen = for {
    schema <- Gen.oneOf("", "http://", "https://", "www.", "https://www.", "http://www.")
    baseUrl <- Gen.oneOf(
      Seq(
        "youtube.com/watch?v=",
        "youtu.be/"
      )
    )
    path = ShortEngStringGen.generate(7)
  } yield {
    schema + baseUrl + path
  }

  val VideoReviewGen: Gen[VideoReview] =
    for {
      url <- YoutubeUrlGen
      onlineShow <- BoolGen
    } yield {
      VideoReview
        .newBuilder()
        .setOnlineShow(BoolValue.of(onlineShow))
        .setYoutubeVideoReviewUrl(StringValue.of(url))
        .build()
    }
}
