package ru.yandex.vos2.reviews.utils

import org.scalatest.FunSuite
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{Content, ContentValue}
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._

import scala.collection.JavaConverters._
import ru.yandex.vos2.reviews.model.MdsPutResponse
import ru.yandex.vos2.reviews.client.MdsClient
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-12.
  */
class ReviewModelUtilsTest extends FunSuite with MockitoSupport {

  test("edit url") {
    val builder = Review.newBuilder().setId("123")
    builder.getItemBuilder.getAutoBuilder.setCategory(Category.CARS)

    val review = builder.build()

    assert(review.createReviewEditUrl == "https://auto.ru/cars/reviews/edit/123/")
  }


  test("strip images") {
    val textContent = Content.newBuilder()
      .setType(Content.Type.TEXT)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()
    val imageContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()
    val deltedImageContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .setKey(DeletedImgKey)
      .build()
    val videoContent = Content.newBuilder()
      .setType(Content.Type.VIDEO)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()

    val review = Review.newBuilder()
      .addContent(textContent)
      .addContent(imageContent)
      .addContent(deltedImageContent)
      .addContent(videoContent)
      .build()

    val clearReview = review.stripDeletedImages

    assert(clearReview.getContentList.asScala.contains(textContent))
    assert(clearReview.getContentList.asScala.contains(videoContent))
    assert(clearReview.getContentList.asScala.contains(imageContent))
    assert(!clearReview.getContentList.asScala.contains(deltedImageContent))
  }

  test("has deleted image") {
    val textContent = Content.newBuilder()
      .setType(Content.Type.TEXT)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()
    val deltedImageContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .setKey(DeletedImgKey)
      .build()

    val review = Review.newBuilder()
      .addContent(textContent)
      .addContent(deltedImageContent)
      .build()

    assert(!review.hasImages)
  }

  test("has image + deleted images") {
    val textContent = Content.newBuilder()
      .setType(Content.Type.TEXT)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()
    val imageContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .build()
    val deltedImageContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(ContentValue.newBuilder().setValue("blabla"))
      .setKey(DeletedImgKey)
      .build()

    val review = Review.newBuilder()
      .addContent(textContent)
      .addContent(imageContent)
      .addContent(deltedImageContent)
      .build()

    assert(review.hasImages)
  }

  test("buildImageUrl") {
    val mdsClient = mock[MdsClient]

    when(mdsClient.prefix).thenReturn("//avatars.mdst.yandex.net/get-")
    assert(buildImageUrl(MdsPutResponse(1, "x"), mdsClient) == "//avatars.mdst.yandex.net/get-autoru-reviews/1/x")
    when(mdsClient.prefix).thenReturn("//avatars.mds.yandex.net/get-")
    assert(buildImageUrl(MdsPutResponse(1234, "foo"), mdsClient) == "//avatars.mds.yandex.net/get-autoru-reviews/1234/foo")
  }

  test("isValidImageUrl") {
    val prefix = "//avatars.mdst.yandex.net/get-"
    val mdsClient = mock[MdsClient]
    when(mdsClient.prefix).thenReturn(prefix)

    val resp = MdsPutResponse(123, "foo123")
    val validUrl = buildImageUrl(resp, mdsClient)

    assert(isValidImageUrl(validUrl, mdsClient) == true)
    assert(isValidImageUrl("/" + validUrl, mdsClient) == false)
    assert(isValidImageUrl(validUrl + "/", mdsClient) == false)
    assert(isValidImageUrl(validUrl.drop(1), mdsClient) == false)
  }
}
