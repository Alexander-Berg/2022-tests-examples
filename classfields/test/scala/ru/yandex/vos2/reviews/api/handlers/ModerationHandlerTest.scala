package ru.yandex.vos2.reviews.api.handlers

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.StatusCodes
import ru.auto.api.ResponseModel.SuccessResponse
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._
import ru.yandex.vos2.reviews.utils.ReviewGenerators._
import ru.yandex.vertis.moderation.proto.Model.{InstanceOpinion, Opinion}
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 24/11/2017.
  */

class ModerationHandlerTest extends BaseReviewHandlerTest {

  initReviewsDbs()

  test("receive moderation opinion") {
    val review = ReviewGen.sample.get
    val opinion = Opinion.newBuilder().setType(Opinion.Type.OK).setVersion(1).build()
    val instance = InstanceOpinion.newBuilder()
      .setExternalId(review.buildModerationExternalId)
      .setOpinion(opinion)
      .setVersion(1)
      .build()

    val stream = new ByteArrayOutputStream()
    instance.writeDelimitedTo(stream)
    val moderationReq = Post("/api/v1/reviews/moderation", stream.toByteArray)
    moderationReq ~> route ~> check {
      withClue(responseAs[SuccessResponse]) {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
