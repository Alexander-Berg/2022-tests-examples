package ru.yandex.vos2.reviews.stage

import org.scalatest.{FunSuite, Matchers}
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.{Content, ContentValue}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.reviews.client.MdsClient
import ru.yandex.vos2.reviews.model.MdsPutResponse
import ru.yandex.vos2.util.StageUtils
import ru.yandex.vos2.reviews.utils.ReviewModelUtils._
import scala.collection.JavaConverters._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 16/08/2018.
  */

class PhotoMigrationStageTest extends FunSuite with Matchers with MockitoSupport with StageUtils {

  private val mdsClient = mock[MdsClient]

  test("has old namespace") {
    val contentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue)
      .build()
    val review = Review.newBuilder()
      .addContent(content)
      .build()

    review.hasOldMdsNamespace shouldBe true
  }

  test("dont have old namespace") {
    val contentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue)
      .build()
    val review = Review.newBuilder()
      .addContent(content)
      .build()

    review.hasOldMdsNamespace shouldBe false
  }

  test("change namespace") {

    when(mdsClient.putImage(?, ?, ?)).thenReturn(MdsPutResponse(42185, "b415d6ae2fa73a652bd1d06fb39beb4a"))
    when(mdsClient.prefix).thenReturn("//avatars.mdst.yandex.net/get-")

    val oldContentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val oldContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(oldContentValue)
      .build()
    val oldReview = Review.newBuilder()
      .addContent(oldContent)
      .build()

    val oldContentValue1 = ContentValue.newBuilder()
      .setValue("https://avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val oldContent1 = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(oldContentValue1)
      .build()
    val oldReview1 = Review.newBuilder()
      .addContent(oldContent1)
      .build()

    val contentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .setNewValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue)
      .build()
    val review = Review.newBuilder()
      .addContent(content)
      .build()

    val contentValue1 = ContentValue.newBuilder()
      .setValue("https://avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .setNewValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content1 = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue1)
      .build()
    val review1 = Review.newBuilder()
      .addContent(content1)
      .build()

    oldReview.withUpdatedMdsNamespace(mdsClient).getContentList.asScala shouldBe review.getContentList.asScala
    oldReview1.withUpdatedMdsNamespace(mdsClient).getContentList.asScala shouldBe review1.getContentList.asScala
  }

  test("change namespace for reviw with deleted images") {
    val oldContentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val deletedContentValue1 = ContentValue.newBuilder()
      .setDeleted(true)
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val deletedContentValue2 = ContentValue.newBuilder()
      .setDeleted(true)
      .setValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val oldContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(oldContentValue)
      .build()
    val oldDeletedContent = Content.newBuilder()
      .setKey(DeletedImgKey)
      .setType(Content.Type.IMAGE)
      .addContentValue(deletedContentValue1)
      .addContentValue(deletedContentValue2)
      .build()
    val oldReview = Review.newBuilder()
      .addContent(oldContent)
      .addContent(oldDeletedContent)
      .build()

    val contentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .setNewValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue)
      .build()
    val deletedContent = Content.newBuilder()
      .setKey(DeletedImgKey)
      .setType(Content.Type.IMAGE)
      .build()
    val review = Review.newBuilder()
      .addContent(content)
      .addContent(deletedContent)
      .build()

    oldReview.withUpdatedMdsNamespace(mdsClient).getContentList.asScala shouldBe review.getContentList.asScala

  }

  test("replace image value") {
    val oldContentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .setNewValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val oldContent = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(oldContentValue)
      .build()
    val oldReview = Review.newBuilder()
      .addContent(oldContent)
      .build()

    val contentValue = ContentValue.newBuilder()
      .setValue("//avatars.mdst.yandex.net/get-autoru-reviews/42185/b415d6ae2fa73a652bd1d06fb39beb4a")
      .build()
    val content = Content.newBuilder()
      .setType(Content.Type.IMAGE)
      .addContentValue(contentValue)
      .build()
    val review = Review.newBuilder()
      .addContent(content)
      .build()

    oldReview.withUpdatedMdsNamespace(mdsClient)
      .withReplacedImageValue.getContentList.asScala shouldBe review.getContentList.asScala
  }

}
