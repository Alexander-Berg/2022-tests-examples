package ru.auto.salesman.service.impl.push

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.salesman.model.DeprecatedDomains
import ru.auto.salesman.service.ProductDescriptionService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{
  BunkerModelGenerator,
  ServiceModelGenerators,
  UserModelGenerators
}
import ru.auto.salesman.util.AutomatedContext

class PushTextCreatorSpec
    extends BaseSpec
    with BunkerModelGenerator
    with UserModelGenerators
    with ServiceModelGenerators {

  implicit override lazy val domain = DeprecatedDomains.AutoRu
  val productDescriptionService = mock[ProductDescriptionService]

  val pushTextCreator = new PushTextCreator(productDescriptionService)

  implicit val rc = AutomatedContext("text")

  "PushTextCreator" should {

    "fail with empty name from bunker user product" in {
      forAll(UserProductGen, DescriptionGenerator.map(_.copy(name = None))) {
        (product, productDescription) =>
          (productDescriptionService.userDescription _)
            .expects(*)
            .returningZ(Some(productDescription))

          pushTextCreator
            .prolongationFailedText(product, None)
            .failure
            .exception shouldBe an[EmptyVasNameFromBunkerException]
      }
    }

    "fail with empty name from bunker offer product" in {
      forAll(
        OfferProductGen,
        DescriptionGenerator.map(_.copy(name = None)),
        Gen.some(OfferCategoryGen)
      ) { (product, productDescription, categoryOpt) =>
        (productDescriptionService.userDescription _)
          .expects(*)
          .returningZ(Some(productDescription))
          .anyNumberOfTimes()

        (productDescriptionService.offerDescription _)
          .expects(*, *)
          .returningZ(Some(productDescription))
          .anyNumberOfTimes()

        pushTextCreator
          .prolongationFailedText(product, categoryOpt)
          .failure
          .exception shouldBe an[EmptyVasNameFromBunkerException]
      }
    }

    "create prolongation failed text for user product" in {
      forAll(UserProductGen, DescriptionGeneratorWithName) {
        (userProduct, productDescription) =>
          val expectedText =
            s"Мы не смогли автоматически продлить опцию ${productDescription.name.get}. Нажмите на сообщение для решения проблемы."

          (productDescriptionService.userDescription _)
            .expects(*)
            .returningZ(Some(productDescription))

          pushTextCreator
            .prolongationFailedText(userProduct, None)
            .success
            .value shouldBe expectedText
      }
    }

    "create prolongation failed text for offer product" in {
      forAll(
        OfferProductGen,
        DescriptionGeneratorWithName,
        Gen.some(OfferCategoryGen)
      ) { (userProduct, productDescription, categoryOpt) =>
        val expectedText =
          s"Мы не смогли автоматически продлить опцию ${productDescription.name.get}. Нажмите на сообщение для решения проблемы."

        (productDescriptionService.offerDescription _)
          .expects(*, *)
          .returningZ(Some(productDescription))

        pushTextCreator
          .prolongationFailedText(userProduct, categoryOpt)
          .success
          .value shouldBe expectedText
      }
    }

    "create prolongation failed text" in {
      forAll(
        UserProductGen,
        DescriptionGeneratorWithName,
        Gen.option(OfferCategoryGen)
      ) { (userProduct, productDescription, categoryOpt) =>
        val expectedText =
          s"Мы не смогли автоматически продлить опцию ${productDescription.name.get}. Нажмите на сообщение для решения проблемы."

        (productDescriptionService.userDescription _)
          .expects(*)
          .returningZ(Some(productDescription))

        (productDescriptionService.offerDescription _)
          .expects(*, *)
          .returningZ(Some(productDescription))
          .anyNumberOfTimes()

        pushTextCreator
          .prolongationFailedText(userProduct, categoryOpt)
          .success
          .value shouldBe expectedText
      }
    }

    "create prolongation placement text with mark, model for cars offer" in {
      forAll(offerGenWithMarkModel(Gen.const(ApiOfferModel.Category.CARS))) { offer =>
        pushTextCreator
          .prolongationPlacementFailedText(offer)
          .success
          .value shouldBe
          s"Не удалось автопродлить ${offer.getCarInfo.getMarkInfo.getName} ${offer.getCarInfo.getModelInfo.getName} ${offer.getDocuments.getYear}г. " +
          s"Нажмите на сообщение для решения проблемы."
      }
    }

    "create prolongation placement text with mark, model for moto offer" in {
      forAll(offerGenWithMarkModel(Gen.const(ApiOfferModel.Category.MOTO))) { offer =>
        pushTextCreator
          .prolongationPlacementFailedText(offer)
          .success
          .value shouldBe
          s"Не удалось автопродлить ${offer.getMotoInfo.getMarkInfo.getName} ${offer.getMotoInfo.getModelInfo.getName} ${offer.getDocuments.getYear}г. " +
          s"Нажмите на сообщение для решения проблемы."
      }
    }

    "create prolongation placement text with mark, model for trucks offer" in {
      forAll(offerGenWithMarkModel(Gen.const(ApiOfferModel.Category.TRUCKS))) { offer =>
        pushTextCreator
          .prolongationPlacementFailedText(offer)
          .success
          .value shouldBe
          s"Не удалось автопродлить ${offer.getTruckInfo.getMarkInfo.getName} ${offer.getTruckInfo.getModelInfo.getName} ${offer.getDocuments.getYear}г. " +
          s"Нажмите на сообщение для решения проблемы."
      }
    }

  }

}
