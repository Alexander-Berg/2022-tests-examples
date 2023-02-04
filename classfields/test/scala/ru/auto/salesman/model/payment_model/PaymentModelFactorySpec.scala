package ru.auto.salesman.model.payment_model

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category.{CARS, MOTO, TRUCKS}
import ru.auto.api.ApiOfferModel.Section.{NEW, USED}
import ru.auto.salesman.model.AdsRequestTypes.{CarsUsed, Commercial}
import ru.auto.salesman.model.ProductId.{Fresh, Placement, Premium, Reset, Special, Top}
import ru.auto.salesman.model.{AdsRequestType, AdsRequestTypes}
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._

class PaymentModelFactorySpec extends BaseSpec {

  private val noSingleWithCallsFactory =
    TestPaymentModelFactory.withoutSingleWithCalls()

  private val singleWithCallsFactory =
    TestPaymentModelFactory.withSingleWithCalls()

  "PaymentModel factory" should {

    "return calls for cars:new placement with calls auction available" in {
      forAll(clientRecordGen(paidCallsAvailableGen = true)) { client =>
        noSingleWithCallsFactory
          .paymentModel(
            Placement,
            CARS,
            NEW,
            client
          )
          .success
          .value shouldBe PaymentModel.Calls
      }
    }

    "return single for cars:used placement with ads request type available" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(CarsUsed), AdsRequestTypes.values)
      forAll(clientRecordGen(singlePaymentsGen = singlePaymentsGen)) { client =>
        noSingleWithCallsFactory
          .paymentModel(
            Placement,
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PaymentModel.Single
      }
    }

    "return singleWithCalls for cars:used placement even with ads request type available if enabled" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(CarsUsed), AdsRequestTypes.values)
      forAll(clientRecordGen(singlePaymentsGen = singlePaymentsGen)) { client =>
        singleWithCallsFactory
          .paymentModel(
            Placement,
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PaymentModel.SingleWithCalls
      }
    }

    "return single for commercial placement with ads request type available" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(Commercial), AdsRequestTypes.values)
      forAll(
        clientRecordGen(singlePaymentsGen = singlePaymentsGen),
        OfferSectionGen
      ) { (client, section) =>
        noSingleWithCallsFactory
          .paymentModel(
            Placement,
            TRUCKS,
            section,
            client
          )
          .success
          .value shouldBe PaymentModel.Single
      }
    }

    "return quota for placement if no ads request types available and " +
    "no calls auction available and no SingleWithCalls available" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false
      )
      forAll(clientGen, ProtoCategoryGen, OfferSectionGen) {
        (client, category, section) =>
          noSingleWithCallsFactory
            .paymentModel(
              Placement,
              category,
              section,
              client
            )
            .success
            .value shouldBe PaymentModel.Quota
      }
    }

    "return SingleWithCalls for placement in cars:used if no ads request types available and " +
    "no calls auction available but SingleWithCalls available" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false
      )
      forAll(clientGen) { client =>
        singleWithCallsFactory
          .paymentModel(
            Placement,
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PaymentModel.SingleWithCalls
      }
    }

    "return quota for moto placement" in {
      forAll(ClientRecordGen, OfferSectionGen) { (client, section) =>
        noSingleWithCallsFactory
          .paymentModel(
            Placement,
            MOTO,
            section,
            client
          )
          .success
          .value shouldBe PaymentModel.Quota
      }
    }

    "return quota for quota product" in {
      forAll(ClientRecordGen, ProtoCategoryGen, OfferSectionGen, quotaTypeGen) {
        (client, category, section, quota) =>
          noSingleWithCallsFactory
            .paymentModel(
              quota,
              category,
              section,
              client
            )
            .success
            .value shouldBe PaymentModel.Quota
      }
    }

    "return single for single product if no SingleWithCalls available" in {
      val productGen = Gen.oneOf(Special, Premium, Top, Fresh, Reset)
      forAll(ClientRecordGen, ProtoCategoryGen, OfferSectionGen, productGen) {
        (client, category, section, product) =>
          noSingleWithCallsFactory
            .paymentModel(
              product,
              category,
              section,
              client
            )
            .success
            .value shouldBe PaymentModel.Single
      }
    }

    "return SingleWithCalls for single product if  SingleWithCalls available" in {
      val productGen = Gen.oneOf(Special, Premium, Top, Fresh, Reset)
      forAll(ClientRecordGen, ProtoCategoryGen, OfferSectionGen, productGen) {
        (client, category, section, product) =>
          singleWithCallsFactory
            .paymentModel(
              product,
              category,
              section,
              client
            )
            .success
            .value shouldBe PaymentModel.SingleWithCalls
      }
    }
  }

  "PlacementPaymentModel factory" should {

    "return calls for cars:new placement with calls auction available" in {
      forAll(clientRecordGen(paidCallsAvailableGen = true)) { client =>
        noSingleWithCallsFactory
          .placementPaymentModel(
            CARS,
            NEW,
            client
          )
          .success
          .value shouldBe PlacementPaymentModel.Calls
      }
    }

    "return single for cars:used placement with ads request type available if SingleWithCalls disabled" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(CarsUsed), AdsRequestTypes.values)
      forAll(clientRecordGen(singlePaymentsGen = singlePaymentsGen)) { client =>
        noSingleWithCallsFactory
          .placementPaymentModel(
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PlacementPaymentModel.Single(CarsUsed)
      }
    }

    "return singleWithCalls for cars:used placement even with ads request type available if SingleWithCalls enabled" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(CarsUsed), AdsRequestTypes.values)
      forAll(clientRecordGen(singlePaymentsGen = singlePaymentsGen)) { client =>
        singleWithCallsFactory
          .placementPaymentModel(
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PlacementPaymentModel.SingleWithCalls
      }
    }

    "return single for commercial placement with ads request type available" in {
      val singlePaymentsGen =
        Gen.oneOf(Set(Commercial), AdsRequestTypes.values)
      forAll(
        clientRecordGen(singlePaymentsGen = singlePaymentsGen),
        OfferSectionGen
      ) { (client, section) =>
        val commercial = PlacementPaymentModel.Single(Commercial)
        singleWithCallsFactory
          .placementPaymentModel(
            TRUCKS,
            section,
            client
          )
          .success
          .value shouldBe commercial
      }
    }

    "return quota for placement if no ads request types available and no calls auction available" +
    "and SingleWithCalls disabled" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false
      )
      forAll(clientGen, ProtoCategoryGen, OfferSectionGen) {
        (client, category, section) =>
          noSingleWithCallsFactory
            .placementPaymentModel(
              category,
              section,
              client
            )
            .success
            .value shouldBe PlacementPaymentModel.Quota
      }
    }

    "return singleWithCalls for placement in cars:used if no ads request types available and no calls auction available" +
    "but SingleWithCalls enabled" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false
      )
      forAll(clientGen) { client =>
        singleWithCallsFactory
          .placementPaymentModel(
            CARS,
            USED,
            client
          )
          .success
          .value shouldBe PlacementPaymentModel.SingleWithCalls
      }
    }

    "return quota for moto placement" in {
      forAll(ClientRecordGen, OfferSectionGen) { (client, section) =>
        noSingleWithCallsFactory
          .placementPaymentModel(
            MOTO,
            section,
            client
          )
          .success
          .value shouldBe PlacementPaymentModel.Quota
      }
    }
  }
}
