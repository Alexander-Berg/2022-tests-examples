package ru.yandex.vos2.autoru.utils.validators

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.CommonModel.Photo
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.FormTestUtils.RichFormBuilder
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors._

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 1/12/17.
  */
@RunWith(classOf[JUnitRunner])
class DraftValidatorTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {
  initOldSalesDbs()
  components.featureRegistry.updateFeature(components.featuresManager.CatalogEquipments.name, true)
  components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, true)

  val draftValidator =
    new DraftValidator(components.autoruSalesDao, components.equipmentHolder, components.featuresManager)

  test("validate") {

    val draft1 = ApiOfferModel.Offer
      .newBuilder()
      .withPurchaseDate(0, 0)
      .withWarrantyExpire(-5, 13)
      .withVideo("m-hjbcvsdhj", None, "https://pewpew")
      .withColor("0000ff")
      .build()

    checkErrors(
      draftValidator.validate(draft1),
      WrongPurchaseDateMonth,
      WrongPurchaseDateYear,
      WrongWarrantyExpireMonth,
      WrongWarrantyExpireYear,
      UnknownColor,
      WrongVideoYoutube
    )

    val draft2 = draft1.toBuilder
      .withPurchaseDate(2011, 11, 36)
      .withWarrantyExpire(2013, 2, 29)
      .withoutVideo
      .withPhoto("test")
      .build()
    checkErrors(
      draftValidator.validate(draft2),
      WrongPurchaseDateDay(30),
      WrongWarrantyExpireDay(28),
      UnknownColor,
      WrongPhotoName("test")
    )

    val testPhotos = {
      (1 to 50).map { idx =>
        Photo.newBuilder().setName(idx + "-test").build()
      }
    }

    val draft3 = draft1.toBuilder
      .withPurchaseDate(2011, 11, 36)
      .withWarrantyExpire(2013, 2, 29)
      .withoutVideo

    draft3.getStateBuilder
      .addAllImageUrls(testPhotos.asJava)

    checkErrors(
      draftValidator.validate(draft3.build()),
      WrongPurchaseDateDay(30),
      WrongWarrantyExpireDay(28),
      UnknownColor,
      WrongPhotoCount(40)
    )
    val testPhotosWithDeleted = {
      (1 to 50).map { idx =>
        Photo.newBuilder().setName(idx + "-test1").setIsDeleted(true).build()
      } :+ (Photo.newBuilder().setName(55 + "-test1").setIsDeleted(false).build())
    }

    val draft4 = draft1.toBuilder
      .withPurchaseDate(2011, 11, 36)
      .withWarrantyExpire(2013, 2, 29)
      .withoutVideo

    draft4.getStateBuilder
      .addAllImageUrls(testPhotosWithDeleted.asJava)

    checkErrors(
      draftValidator.validate(draft4.build()),
      WrongPurchaseDateDay(30),
      WrongWarrantyExpireDay(28),
      UnknownColor
    )

    val correctDraft = draft1.toBuilder
      .withPurchaseDate(2011, 11)
      .withWarrantyExpire(2013, 4)
      .withVideo("m-63774-156a07376d0-87d395248cef988d", Some("MJIC9MWhrrs"), "https://youtu.be/MJIC9MWhrrs")
      .withColor("0000CC")
      .build()
    checkCorrectResult(draftValidator.validate(correctDraft))

    // без цвета черновик тоже прокатит
    val correctDraft2: Offer = correctDraft.toBuilder.withColor("").build()
    checkCorrectResult(draftValidator.validate(correctDraft2))

    val inncompatibleEquipmentDraft = draft1.toBuilder
      .withPurchaseDate(2011, 11)
      .withWarrantyExpire(2013, 4)
      .withVideo("m-63774-156a07376d0-87d395248cef988d", Some("MJIC9MWhrrs"), "https://youtu.be/MJIC9MWhrrs")
      .withColor("0000CC")
    inncompatibleEquipmentDraft.getCarInfoBuilder.putEquipment("passenger-seat-electric", true)
    inncompatibleEquipmentDraft.getCarInfoBuilder.putEquipment("driver-seat-electric", true)
    inncompatibleEquipmentDraft.getDocumentsBuilder.setLicensePlate("Х936РА77")
    components.featureRegistry.updateFeature(components.featuresManager.IncompatibleEquipmentCheck.name, true)

    checkErrors(
      draftValidator.validate(inncompatibleEquipmentDraft.build()),
      IncompatibleEquipment("driver-seat-electric", "passenger-seat-electric")
    )

  }

  private def checkCorrectResult(result: Either[List[ValidationError], Unit]): Unit = {
    fassert(result.isRight, s"validation must pass but failed with messages:\n${result.left.get.mkString(", ")}")
  }

  private def checkErrors(result: Either[List[ValidationError], Unit], errors: ValidationError*): Unit = {
    assert(result.isLeft, s"validation must fail with messages\n${errors.mkString(", ")}\nbut its passed")
    assert(
      errors.forall(result.left.get.contains),
      s"validation must fail with messages" +
        s"\n${errors.mkString(", ")},\nbut some or all of these messages wasn't " +
        s"found:\n${errors.diff(result.left.get).mkString(", ")}" +
        s"\nthese messages found:\n${result.left.get.mkString(", ")}"
    )
    assert(
      result.left.get.forall(errors.contains),
      s"unexpected messages found:" +
        s"\n${result.left.get.diff(errors).mkString(", ")}"
    )
  }

  private def fassert(expr: Boolean, message: => String): Unit = {
    if (!expr) fail(message)
  }
}
