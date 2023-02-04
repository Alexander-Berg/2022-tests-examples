package ru.yandex.vos2.reviews

import java.io.ByteArrayOutputStream

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.scalatest.{FunSuite, Matchers}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.reviews.ReviewModel.Review.Item.Auto
import ru.auto.api.reviews.ReviewModel.Review._
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.reviews.dao.comment.CommentsDao
import ru.yandex.vos2.reviews.features.ReviewsFeatures
import ru.yandex.vos2.reviews.utils.{DockerReviewCoreComponents, ReviewValidator}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 04/08/2017.
  */
trait BaseReviewsTest extends FunSuite with Matchers with DockerReviewCoreComponents{
  protected val stream = new ByteArrayOutputStream()

  //------------Offer1----------------------
  private def createAuto1 = {
    Auto.newBuilder()
      .setCategory(Category.CARS)
      .setMark("Audu")
      .setModel("TT")
      .setSuperGenId(1)
      .setCategory(Category.CARS)
      .build()
  }

  private def createItem1 = {
    Item.newBuilder()
      .setAuto(createAuto1)
      .build()
  }

  private def createContnet = {

    val imgContent = ContentValue.newBuilder().setValue("image")
    Content.newBuilder().setType(Content.Type.IMAGE).addContentValue(imgContent)
  }

  private def createTags11 = Tag.newBuilder().setName("kuzov").setType(Tag.Type.RATING5).setValue("5")

  private def createTags12 = Tag.newBuilder().setName("podveska").setType(Tag.Type.RATING5).setValue("3")

  private def createTags13 = Tag.newBuilder().setName("dvigatel").setType(Tag.Type.RATING5).setValue("2")

  private def protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

  private def createReview1 = {
    val protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

    Review.newBuilder()
      .setId("111")
      .setPublished(protobufTimestamp)
      .setLikeNum(10)
      .setDislikeNum(5)
      .setCountComments(10)
      .addTags(createTags11)
      .addTags(createTags12)
      .addTags(createTags13)
      .setItem(createItem1)
      .addContent(createContnet)
      .setReviewer(ReviewUser.newBuilder().setId("user:1"))
      .setStatus(Status.ENABLED)
      .addModerationHistory(Review.Moderation.newBuilder()
        .setTime(protobufTimestamp).setStatus(Moderation.Status.ACCEPTED).build())
      .build()
  }

  protected def createOffer1: OfferModel.Offer = {
    OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setUserRef("a_1")
      .setTimestampCreate(new DateTime(System.currentTimeMillis()).minusDays(8).getMillis)
      .setTimestampUpdate(1)
      .setReview(createReview1)
      .build()

  }

  //-----------Offer 2-----------------

  private def createAuto2 = {
    Auto.newBuilder()
      .setCategory(Category.CARS)
      .setMark("BMW")
      .setModel("X5")
      .setSuperGenId(1)
      .build()
  }

  private def createItem2 = {
    Item.newBuilder()
      .setAuto(createAuto2)
      .build()
  }

  private def createTags21 = Tag.newBuilder().setName("kuzov").setType(Tag.Type.RATING5).setValue("3")

  private def createTags22 = Tag.newBuilder().setName("podveska").setType(Tag.Type.RATING5).setValue("4")

  private def createTags23 = Tag.newBuilder().setName("dvigatel").setType(Tag.Type.RATING5).setValue("1")


  private def createReview2 = {
    val protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

    Review.newBuilder()
      .setId("222")
      .setPublished(protobufTimestamp)
      .setLikeNum(5)
      .setDislikeNum(15)
      .setCountComments(20)
      .addTags(createTags21)
      .addTags(createTags22)
      .addTags(createTags23)
      .setItem(createItem2)
      .build()
  }

  protected def createOffer2: OfferModel.Offer = {
    OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setUserRef("user")
      .setTimestampCreate(new DateTime(System.currentTimeMillis()).plusDays(2).getMillis)
      .setTimestampUpdate(2)
      .setReview(createReview2)
      .build()

  }

  //-----------Offer 3-----------------

  private def createAuto3 = {
    Auto.newBuilder()
      .setCategory(Category.CARS)
      .setMark("BMW")
      .setModel("X5")
      .setSuperGenId(2)
      .build()
  }

  private def createItem3 = {
    Item.newBuilder()
      .setAuto(createAuto3)
      .build()
  }

  private def createReview3 = {
    val protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

    Review.newBuilder()
      .setId("333")
      .setPublished(protobufTimestamp)
      .setLikeNum(7)
      .setDislikeNum(7)
      .setCountComments(15)
      .addTags(createTags31)
      .addTags(createTags32)
      .addTags(createTags33)
      .setItem(createItem3)
      .build()
  }

  private def createTags31 = Tag.newBuilder().setName("kuzov").setType(Tag.Type.RATING5).setValue("5")

  private def createTags32 = Tag.newBuilder().setName("podveska").setType(Tag.Type.RATING5).setValue("5")

  private def createTags33 = Tag.newBuilder().setName("dvigatel").setType(Tag.Type.RATING5).setValue("5")

  protected def createOffer3: OfferModel.Offer = {
    OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setUserRef("user")
      .setTimestampCreate(new DateTime(System.currentTimeMillis()).plusDays(3).getMillis)
      .setTimestampUpdate(3)
      .setReview(createReview3)
      .build()

  }

  //-----------Offer 4-----------------

  private def createAuto4 = {
    Auto.newBuilder()
      .setCategory(Category.CARS)
      .setMark("BMW")
      .setModel("X5")
      .setSuperGenId(2)
      .build()
  }

  private def createItem4 = {
    Item.newBuilder()
      .setAuto(createAuto4)
      .build()
  }

  private def createTags41 = Tag.newBuilder().setName("kuzov").setType(Tag.Type.RATING5).setValue("4")

  private def createTags42 = Tag.newBuilder().setName("podveska").setType(Tag.Type.RATING5).setValue("4")

  private def createTags43 = Tag.newBuilder().setName("dvigatel").setType(Tag.Type.RATING5).setValue("4")

  private def createReview4 = {
    val protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

    Review.newBuilder()
      .setId("444")
      .setPublished(protobufTimestamp)
      .setLikeNum(8)
      .setDislikeNum(5)
      .setCountComments(5)
      .addTags(createTags41)
      .addTags(createTags42)
      .addTags(createTags43)
      .setItem(createItem4)
      .build()
  }

  protected def createOffer4: OfferModel.Offer = {
    OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setUserRef("user")
      .setTimestampCreate(new DateTime(System.currentTimeMillis()).plusDays(4).getMillis)
      .setTimestampUpdate(4)
      .setReview(createReview4)
      .build()

  }

  //-----------Offer 5-----------------

  private def createAuto5 = {
    Auto.newBuilder()
      .setCategory(Category.MOTO)
      .setMark("BMW")
      .setSubCategory("motorcycles")
      .build()
  }

  private def createItem5 = {
    Item.newBuilder()
      .setAuto(createAuto5)
      .build()
  }

  private def createTags51 = Tag.newBuilder().setName("kuzov").setType(Tag.Type.RATING5).setValue("4")

  private def createTags52 = Tag.newBuilder().setName("podveska").setType(Tag.Type.RATING5).setValue("4")

  private def createTags53 = Tag.newBuilder().setName("dvigatel").setType(Tag.Type.RATING5).setValue("4")

  private def createReview5 = {
    val protobufTimestamp = Timestamps.fromMillis(System.currentTimeMillis)

    Review.newBuilder()
      .setId("555")
      .setPublished(protobufTimestamp)
      .setLikeNum(8)
      .setDislikeNum(5)
      .setCountComments(5)
      .addTags(createTags51)
      .addTags(createTags52)
      .addTags(createTags53)
      .setItem(createItem5)
      .build()
  }

  protected def createOffer5: OfferModel.Offer = {
    OfferModel.Offer.newBuilder()
      .setOfferService(OfferService.OFFER_AUTO)
      .setUserRef("user")
      .setTimestampCreate(new DateTime(System.currentTimeMillis()).plusDays(4).getMillis)
      .setTimestampUpdate(4)
      .setReview(createReview5)
      .build()

  }

  def reviewValidator: ReviewValidator = new ReviewValidator(mdsClient)
}
