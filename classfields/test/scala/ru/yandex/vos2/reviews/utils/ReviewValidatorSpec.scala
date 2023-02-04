package ru.yandex.vos2.reviews.utils

import ru.auto.api.ApiOfferModel.Category
import org.scalatest.FunSuite
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vos2.reviews.client.MdsClient
import ru.yandex.vos2.reviews.model.MdsPutResponse
import ru.auto.api.reviews.ReviewModel.Review
import org.scalatest.EitherValues._
import org.scalatest.Matchers._

class ReviewValidatorSpec extends FunSuite {
  val mdsClient = mock[MdsClient]
  when(mdsClient.prefix).thenReturn("foo")
  val validUrl = ReviewModelUtils.buildImageUrl(MdsPutResponse(1, "x"), mdsClient)
  val invalidUrl = validUrl + "#"
  val reviewValidator = new ReviewValidator(mdsClient)

  test("validate image urls") {
    val a = reviewWithImages()()
    noException shouldBe thrownBy (reviewValidator.validate(a))

    val b = reviewWithImages()(Seq(validUrl))
    noException shouldBe thrownBy (reviewValidator.validate(b))

    val c = reviewWithImages()(Seq(invalidUrl))
    an[IllegalReviewException] shouldBe thrownBy (reviewValidator.validate(c))

    val d = reviewWithImages()(Seq(validUrl, invalidUrl))
    an[IllegalReviewException] shouldBe thrownBy (reviewValidator.validate(d))

    val e = reviewWithImages()(Seq(validUrl), Seq(invalidUrl))
    an[IllegalReviewException] shouldBe thrownBy (reviewValidator.validate(e))

    val f = reviewWithImages(Category.MOTO)(Seq(validUrl))
    noException shouldBe thrownBy (reviewValidator.validate(f))

    val g = reviewWithImages(Category.MOTO)(Seq(invalidUrl))
    an[IllegalReviewException] shouldBe thrownBy (reviewValidator.validate(g))
  }

  private def reviewWithImages(category: Category = Category.CARS)(urlsByContent: Seq[String]*): Review = {
    val builder = Review.newBuilder()
    builder.getItemBuilder().getAutoBuilder().setCategory(category)

    urlsByContent.foreach { urls =>
      val content = builder.addContentBuilder().setType(Review.Content.Type.IMAGE)
      val contentValue = content.addContentValueBuilder()
      urls.zipWithIndex.foreach {
        case (url, ix) =>
          contentValue.putImageUrl(ix.toString, url)
      }
    }

    builder.build()
  }
}
