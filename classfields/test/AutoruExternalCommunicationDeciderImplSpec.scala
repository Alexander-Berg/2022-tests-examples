package ru.yandex.vertis.shark.communication.impl

import java.net.URL
import cats.syntax.option._
import com.softwaremill.tagging._
import ru.auto.api.api_offer_model.{Category, Offer, OfferStatus, Section, State}
import ru.auto.api.cars_model.CarInfo
import ru.auto.api.common_model.{Photo, PhotoClass, PriceInfo}
import ru.yandex.vertis.shark.Mock._
import ru.yandex.vertis.shark.communication.autoru.AutoruExternalCommunicationDecider
import ru.yandex.vertis.shark.controller.credit_product_calculator.testkit.CreditProductCalculatorMock
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.AutoruCreditApplication.ExternalCommunication.ClaimEntity
import ru.yandex.vertis.shark.model.AutoruCreditApplication.{ExternalCommunication, ExternalCommunicationSource}
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.event.CreditApplicationAutoruExternalCommunicationEvent
import ru.yandex.vertis.shark.model.event.CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload
import ru.yandex.vertis.shark.model.generators._
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.OfferId
import ru.yandex.vertis.zio_baker.util.EmptyString
import zio.ULayer
import zio.test.Assertion.{anything, equalTo}
import zio.test.mock._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AutoruExternalCommunicationDeciderImplSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen {

  private case class DeciderTestCase(
      description: String,
      creditApplication: AutoruCreditApplication,
      layer: ULayer[AutoruExternalCommunicationDecider],
      expected: Option[ExternalCommunicationSource])

  private val creditProductId: CreditProductId = "tinkoff-1".taggedWith[Tag.CreditProductId]

  private val creditProduct: AutoCreditProduct = AutoCreditProduct.forTest(
    id = creditProductId,
    bankType = proto.Bank.BankType.TINKOFF.some,
    bankId = "tinkoff".taggedWith[Tag.BankId],
    amountRange = CreditProduct.AmountRange(None, None),
    interestRateRange = CreditProduct.InterestRateRange(None, None),
    termMonthsRange = CreditProduct.TermMonthsRange(None, None),
    minInitialFeeRate = 0f.taggedWith[Tag.Rate],
    geobaseIds = Seq.empty,
    creditApplicationInfoBlockDependencies = Seq.empty,
    borrowerPersonProfileBlockDependencies = Seq.empty,
    creditProposalEntities = Seq.empty,
    isActive = true,
    borrowerConditions = None
  )

  private val offerId: OfferId = "some-offer-id".taggedWith[zio_baker.Tag.OfferId]

  private val photoUrl: String = "//some.domain.com/photo1.jpg"

  private val state: State = State.defaultInstance
    .withImageUrls(
      Seq(
        Photo.defaultInstance
          .withIsDeleted(false)
          .withIsInternal(false)
          .withPhotoClass(PhotoClass.AUTO_VIEW_3_4_FRONT_LEFT)
          .withSizes(Map(ObjectPayload.Size320x240 -> photoUrl))
      )
    )

  private val offer: Offer = Offer.defaultInstance
    .withStatus(OfferStatus.ACTIVE)
    .withId(offerId)
    .withCategory(Category.CARS)
    .withState(state)
    .withCarInfo(CarInfo.defaultInstance.withMark("MERCEDES").withModel("S-Klasse AMG"))
    .withPriceInfo(PriceInfo.defaultInstance.withRurPrice(15000000L))

  private def creditProductDictionary(creditProducts: Seq[CreditProduct]) =
    CreditProductDictionaryMock
      .List(
        anything,
        Expectation.value(creditProducts)
      )
      .optional

  private def creditProductCalculator =
    CreditProductCalculatorMock
      .Suitable(
        anything,
        Expectation.value(
          Suitable(
            creditProductId = creditProductId,
            passed = true,
            checkRequirements = CheckRequirements.Passed,
            checkBorrower = CheckBorrower.Passed,
            info = MissingBlocks(Seq.empty, Seq.empty),
            borrowerPersonProfile = MissingBlocks(Seq.empty, Seq.empty),
            checkRateLimit = CheckRateLimit.Passed,
            checkObject = Seq.empty
          )
        )
      )
      .optional

  private def layer(creditProducts: Seq[CreditProduct] = Seq.empty): ULayer[AutoruExternalCommunicationDecider] =
    (creditProductDictionary(creditProducts) && creditProductCalculator) >>>
      AutoruExternalCommunicationDecider.live

  import proto.CreditApplication.Communication.AutoruExternal

  private val deciderTestCases: Seq[DeciderTestCase] = Seq(
    DeciderTestCase(
      description = "Completeness BLANK",
      creditApplication = sampleAutoruCreditApplication()
        .copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None,
          borrowerPersonProfile = None
        ),
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.BLANK.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "Completeness MINIMUM",
      creditApplication = {
        val c = sampleAutoruCreditApplication()
        c.copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              name = c.borrowerPersonProfile.flatMap(_.name),
              phones = c.borrowerPersonProfile.flatMap(_.phones)
            )
            .some
        )
      },
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.MINIMUM.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "Completeness PARTIALLY",
      creditApplication = {
        val c = sampleAutoruCreditApplication()
        c.copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              name = c.borrowerPersonProfile.flatMap(_.name),
              phones = c.borrowerPersonProfile.flatMap(_.phones),
              oldName = c.borrowerPersonProfile.flatMap(_.oldName),
              gender = c.borrowerPersonProfile.flatMap(_.gender),
              passportRf = c.borrowerPersonProfile.flatMap(_.passportRf),
              oldPassportRf = c.borrowerPersonProfile.flatMap(_.oldPassportRf),
              foreignPassport = c.borrowerPersonProfile.flatMap(_.foreignPassport),
              insuranceNumber = c.borrowerPersonProfile.flatMap(_.insuranceNumber),
              driverLicense = c.borrowerPersonProfile.flatMap(_.driverLicense)
            )
            .some
        )
      },
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.PARTIALLY.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "Completeness ALMOST_COMPLETLY",
      creditApplication = {
        val c = sampleAutoruCreditApplication()
        c.copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              name = c.borrowerPersonProfile.flatMap(_.name),
              phones = c.borrowerPersonProfile.flatMap(_.phones),
              oldName = c.borrowerPersonProfile.flatMap(_.oldName),
              gender = c.borrowerPersonProfile.flatMap(_.gender),
              passportRf = c.borrowerPersonProfile.flatMap(_.passportRf),
              oldPassportRf = c.borrowerPersonProfile.flatMap(_.oldPassportRf),
              foreignPassport = c.borrowerPersonProfile.flatMap(_.foreignPassport),
              insuranceNumber = c.borrowerPersonProfile.flatMap(_.insuranceNumber),
              birthDate = c.borrowerPersonProfile.flatMap(_.birthDate),
              birthPlace = c.borrowerPersonProfile.flatMap(_.birthPlace),
              residenceAddress = c.borrowerPersonProfile.flatMap(_.residenceAddress),
              registrationAddress = c.borrowerPersonProfile.flatMap(_.registrationAddress)
            )
            .some
        )
      },
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.ALMOST_COMPLETLY.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "Completeness COMPLETE",
      creditApplication = sampleAutoruCreditApplication()
        .copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None
        ),
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.COMPLETE.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "ObjectCommunicationState SELECTED",
      creditApplication = sampleAutoruCreditApplication()
        .copy(
          claims = Seq.empty,
          offers = Seq(AutoruCreditApplication.Offer(Category.CARS, Section.USED, offerId, None, None)),
          externalCommunication = None
        ),
      layer = layer(),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.COMPLETE.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.SELECTED.some,
        claimEntities = Seq.empty
      ).some
    ),
    DeciderTestCase(
      description = "With recommended ClaimEntity",
      creditApplication = sampleAutoruCreditApplication()
        .copy(
          claims = Seq.empty,
          offers = Seq.empty,
          externalCommunication = None
        ),
      layer = layer(Seq(creditProduct)),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.COMPLETE.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq(
          ClaimEntity.forTest(creditProductId, AutoruExternal.ClaimCommunicationState.RECOMMENDED)
        )
      ).some
    ),
    DeciderTestCase(
      description = "With sent ClaimEntity",
      creditApplication = sampleAutoruCreditApplication().copy(
        claims = Seq(
          CreditApplication.AutoruClaim.forTest(
            id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
            bankClaimId = None,
            created = InstantArb.arbitrary.sample.get,
            updated = InstantArb.arbitrary.sample.get,
            processAfter = None,
            creditProductId = creditProductId,
            state = proto.CreditApplication.Claim.ClaimState.NEW
          )
        ),
        offers = Seq.empty,
        externalCommunication = None
      ),
      layer = layer(Seq(creditProduct)),
      expected = ExternalCommunicationSource(
        creditApplicationState = proto.CreditApplication.State.DRAFT.some,
        completenessState = AutoruExternal.CompletenessState.COMPLETE.some,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED.some,
        claimEntities = Seq(
          ClaimEntity.forTest(
            creditProductId,
            AutoruExternal.ClaimCommunicationState.SENT,
            proto.CreditApplication.Claim.ClaimState.NEW.some
          )
        )
      ).some
    ),
    DeciderTestCase(
      description = "No changes",
      creditApplication = sampleAutoruCreditApplication().copy(
        claims = Seq(
          CreditApplication.AutoruClaim.forTest(
            id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
            bankClaimId = None,
            created = InstantArb.arbitrary.sample.get,
            updated = InstantArb.arbitrary.sample.get,
            processAfter = None,
            creditProductId = creditProductId,
            state = proto.CreditApplication.Claim.ClaimState.NEW
          )
        ),
        offers = Seq.empty,
        externalCommunication = ExternalCommunication
          .forTest(
            updated = InstantArb.arbitrary.sample.get,
            lastEvent = None,
            eventScheduledAt = None,
            creditApplicationState = proto.CreditApplication.State.DRAFT,
            completenessState = AutoruExternal.CompletenessState.COMPLETE,
            objectCommunicationState = AutoruExternal.ObjectCommunicationState.NOT_SELECTED,
            claimEntities = Seq(
              ClaimEntity.forTest(
                creditProductId,
                AutoruExternal.ClaimCommunicationState.SENT,
                proto.CreditApplication.Claim.ClaimState.NEW.some
              )
            )
          )
          .some
      ),
      layer = layer(Seq(creditProduct)),
      expected = None
    )
  )

  private val deciderTests = deciderTestCases.map {
    case DeciderTestCase(description, creditApplication, layer, expected) =>
      val result = AutoruExternalCommunicationDecider.decide(creditApplication, offer.some)
      testM(description)(assertM(result)(equalTo(expected))).provideLayer(layer)
  }

  private val toEventTests = Seq {
    val ts = InstantArb.arbitrary.sample.get
    val externalCommunication = ExternalCommunication.forTest(
      updated = ts,
      lastEvent = None,
      eventScheduledAt = None,
      creditApplicationState = proto.CreditApplication.State.DRAFT,
      completenessState = AutoruExternal.CompletenessState.COMPLETE,
      objectCommunicationState = AutoruExternal.ObjectCommunicationState.SELECTED,
      claimEntities = Seq(
        ClaimEntity.forTest(
          creditProductId = creditProductId,
          state = AutoruExternal.ClaimCommunicationState.SENT
        )
      )
    )
    val creditApplication = sampleAutoruCreditApplication().copy(externalCommunication = externalCommunication.some)
    val requestId = "some-request-id"
    val result = AutoruExternalCommunicationDecider.toEvent(
      creditApplication = creditApplication,
      offer = offer.some,
      requestId = requestId.some
    )
    val expected = CreditApplicationAutoruExternalCommunicationEvent(
      timestamp = ts,
      requestId = requestId.some,
      idempotencyKey = result.map(_.idempotencyKey).getOrElse(EmptyString),
      externalCommunication = externalCommunication,
      creditApplicationId = creditApplication.id,
      creditApplicationRequirements = creditApplication.requirements,
      creditApplicationState = creditApplication.state,
      objectPayload = CreditApplicationAutoruExternalCommunicationEvent
        .ObjectPayload(
          offerUrl = new URL("https://auto.ru/rcard/some-offer-id"),
          priceRub = 15000000L.taggedWith[Tag.MoneyRub],
          mark = "MERCEDES",
          model = "S-Klasse AMG",
          photos = Seq(
            CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload.Photo(
              sizes = Map(ObjectPayload.Size320x240 -> new URL(s"http:$photoUrl")),
              photoClass = PhotoClass.AUTO_VIEW_3_4_FRONT_LEFT
            )
          )
        )
        .some,
      userPayload = CreditApplicationAutoruExternalCommunicationEvent.UserPayload(
        name = creditApplication.borrowerPersonProfile.flatMap(_.name.map(_.nameEntity)),
        phone = creditApplication.borrowerPersonProfile
          .flatMap(_.phones)
          .map(_.phoneEntities)
          .orEmpty
          .headOption
          .map(_.phone),
        email = creditApplication.borrowerPersonProfile
          .flatMap(_.emails)
          .map(_.emailEntities)
          .orEmpty
          .headOption
          .map(_.email),
        user = None
      )
    ).some
    test("event")(zio.test.assert(result)(equalTo(expected)))
  }

  override def spec: ZSpec[Environment, Failure] =
    suite("AutoruExternalCommunicationDecider")(
      suite("decide")(deciderTests: _*),
      suite("toEvent")(toEventTests: _*)
    )
}
