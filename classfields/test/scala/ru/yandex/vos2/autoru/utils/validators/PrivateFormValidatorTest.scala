package ru.yandex.vos2.autoru.utils.validators

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.auto.api.CommonModel.Photo
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class PrivateFormValidatorTest extends AnyFunSuite with InitTestDbs {

  val privateFormValidator = new PrivateFormValidator(components)

  test("should check the number of photos") {
    def errorsFor(photoCount: Int, params: FormWriteParams = FormWriteParams.empty): Seq[ValidationError] = {
      val builder = ApiOfferModel.Offer.newBuilder()
      builder
        .getStateBuilder()
        .addAllImageUrls(
          Seq.fill(photoCount)(Photo.getDefaultInstance()).asJava
        )

      val result = privateFormValidator.validate(ApiOfferModel.Category.CARS, builder.build(), None, None, params, 0)
      result.toEither.left.get.errors
    }

    assert(!errorsFor(MaxPhotoCount).exists(_.isInstanceOf[WrongPhotoCount]))
    assert(errorsFor(MaxPhotoCount + 1).contains(WrongPhotoCount(MaxPhotoCount)))
    assert(
      !errorsFor(MaxPhotoCount + 1, FormWriteParams(simplifiedValidation = true))
        .exists(_.isInstanceOf[WrongPhotoCount])
    )
  }
}
