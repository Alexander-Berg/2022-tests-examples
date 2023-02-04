package ru.yandex.vertis.shark.controller.updater

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging._
import common.zio.features.testkit.FeaturesTest
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.controller.updater.CreditApplicationUpdater.CreditApplicationUpdater
import ru.yandex.vertis.shark.model.CreditApplication.{AutoruClaim, Claim, Info, Requirements, UserSettings}
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.UserRef.AutoruDealer
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.notification.NotificationUpdater
import ru.yandex.vertis.shark.proto.model.CreditApplication.Claim.ClaimPayload.ObjectState
import ru.yandex.vertis.shark.proto.model.CreditApplication.Claim.ClaimState
import ru.yandex.vertis.shark.proto.model.CreditApplication.Communication.AutoruExternal
import ru.yandex.vertis.shark.proto.model.CreditApplication.State
import ru.yandex.vertis.shark.proto.model.CreditProduct.IdempotencyType
import ru.yandex.vertis.shark.util.RichModel._
import ru.yandex.vertis.shark.util.{sampleAutoruCreditApplciation, sampleClaim, sampleOffer}
import ru.yandex.vertis.zio_baker.util.DateTimeUtil._
import zio.test._
import zio.ULayer

import java.time.Instant
import scala.concurrent.duration._
import zio.test.Assertion._
import zio.test.mock.mockable
import zio.test.mock.Expectation.valueF

object CreditApplicationUpdaterSpec extends DefaultRunnableSpec {

  private val ts = Instant.now()

  private case class ApplyUpdateRequestTestCase(
      description: String,
      source: CreditApplication,
      updateRequest: CreditApplication.UpdateRequest,
      expected: CreditApplication)

  private case class ApplyCreditProductsTestCase(
      description: String,
      creditApplication: CreditApplication,
      creditProducts: Seq[CreditProduct],
      sendDelay: Option[FiniteDuration],
      timestamp: Instant,
      check: CreditApplication => TestResult)

  private val applyUpdateRequestTestCases = Seq(
    {
      val creditApplication = sampleAutoruCreditApplciation().copy(
        schedulerLastUpdate = ts.minusDuration(1.minute).some,
        userSettings = UserSettings(
          affiliateUserId = "SomeAffiliateUserId".taggedWith[Tag.AffiliateUserId].some,
          tags = Seq("a", "b").map(_.taggedWith[Tag.UserTag])
        ).some
      )
      val state = State.ACTIVE
      val requirements = Requirements(
        maxAmount = 1000000L.taggedWith[Tag.MoneyRub],
        initialFee = 100000L.taggedWith[Tag.MoneyRub],
        termMonths = 36.taggedWith[Tag.MonthAmount],
        geobaseIds = Seq.empty
      )
      val info = Info.forTest(controlWord = Block.ControlWordBlock("СекретноеСлово").some)
      val borrowerPersonProfile =
        PersonProfileImpl.forTest(expenses = Block.ExpensesBlock(250000L.taggedWith[Tag.MoneyRub]).some)
      val offers = Seq(sampleOffer())
      val garageId = None
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with fields",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(offers, garageId).some,
              state = state.some,
              requirements = requirements.some,
              info = info.some,
              borrowerPersonProfile = borrowerPersonProfile.some,
              userSettings = CreditApplication
                .UserSettings(
                  affiliateUserId = "NewAffiliateUserId".taggedWith[Tag.AffiliateUserId].some,
                  tags = Seq("b", "c").map(_.taggedWith[Tag.UserTag])
                )
                .some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          state = state,
          requirements = requirements.some,
          info = info.some,
          borrowerPersonProfile = borrowerPersonProfile.some,
          offers = offers,
          userSettings = CreditApplication
            .UserSettings(
              affiliateUserId = "SomeAffiliateUserId".taggedWith[Tag.AffiliateUserId].some,
              tags = Seq("a", "b", "c").map(_.taggedWith[Tag.UserTag])
            )
            .some
        )
      )
    }, {
      val creditApplication = sampleAutoruCreditApplciation().copy(
        state = State.DRAFT,
        schedulerLastUpdate = ts.minusDuration(1.minute).some
      )
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with empty source",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = None,
              state = None,
              requirements = None,
              info = None,
              borrowerPersonProfile = None,
              userSettings = None
            )
            .some
        ),
        expected = creditApplication.copy(updated = ts)
      )
    }, {
      import CreditApplicationClaimSource.AutoruPayload
      import CreditApplicationClaimSource.AutoruPayload.OfferEntity
      val offer = sampleOffer()
      val sourceOfferEntity = OfferEntity(
        offer = offer,
        state = ObjectState.APPROVED
      )
      val claimSource = CreditApplicationClaimSource(
        payload = AutoruPayload(Seq(sourceOfferEntity)).some,
        state = ClaimState.APPROVED.some,
        bankState = None,
        approvedMaxAmount = 2000000L.taggedWith[Tag.MoneyRub].some,
        approvedTermMonths = 24.taggedWith[Tag.MonthAmount].some,
        approvedInterestRate = 10.5f.taggedWith[Tag.Rate].some,
        approvedMinInitialFeeRate = 5.5f.taggedWith[Tag.Rate].some
      )
      val offerEntity = AutoruClaim.OfferEntity(
        offer = offer,
        created = ts.minusSeconds(9999L),
        updated = ts.minusSeconds(9999L),
        state = ObjectState.NEW
      )
      val claim = sampleClaim().copy(offerEntities = Seq(offerEntity))
      val creditApplication = sampleAutoruCreditApplciation().copy(state = State.DRAFT, claims = Seq(claim))
      val expectedOfferEntity = AutoruClaim.OfferEntity(
        offer = offer,
        created = offerEntity.created,
        updated = ts,
        state = ObjectState.APPROVED
      )
      val expectedClaim = claim.copy(
        updated = ts,
        state = ClaimState.APPROVED,
        approvedMaxAmount = claimSource.approvedMaxAmount,
        approvedTermMonths = claimSource.approvedTermMonths,
        approvedInterestRate = claimSource.approvedInterestRate,
        approvedMinInitialFeeRate = claimSource.approvedMinInitialFeeRate,
        offerEntities = Seq(expectedOfferEntity)
      )
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationClaimSource with fields",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          claims = Map(
            claim.id -> claimSource
          )
        ),
        expected = creditApplication
          .copy(
            updated = ts,
            claims = Seq(expectedClaim)
          )
      )
    }, {
      val claim = sampleClaim()
      val creditApplication = sampleAutoruCreditApplciation().copy(
        schedulerLastUpdate = None,
        state = State.DRAFT,
        claims = Seq(claim)
      )
      val claimSource = CreditApplicationClaimSource(
        payload = None,
        state = None,
        bankState = None,
        approvedMaxAmount = None,
        approvedTermMonths = None,
        approvedInterestRate = None,
        approvedMinInitialFeeRate = None
      )
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationClaimSource with empty source",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          claims = Map(
            claim.id -> claimSource
          )
        ),
        expected = creditApplication
          .copy(
            updated = ts,
            claims = Seq(claim.copy(updated = ts))
          )
      )
    }, {
      val creditApplication = sampleAutoruCreditApplciation().copy(
        schedulerLastUpdate = ts.minusDuration(1.minute).some,
        externalCommunication = None
      )
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with AutoPayload",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          payload = CreditApplication.UpdateRequest
            .AutoPayload(
              externalCommunicationSource = AutoruCreditApplication
                .ExternalCommunicationSource(
                  creditApplicationState = State.ACTIVE.some,
                  completenessState = AutoruExternal.CompletenessState.MINIMUM.some,
                  objectCommunicationState = AutoruExternal.ObjectCommunicationState.SELECTED.some,
                  claimEntities = Seq.empty,
                  lastEvent = None
                )
                .some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          externalCommunication = AutoruCreditApplication.ExternalCommunication
            .forTest(
              updated = ts,
              lastEvent = None,
              eventScheduledAt = ts.plusDuration(10.minutes).some,
              creditApplicationState = State.ACTIVE,
              completenessState = AutoruExternal.CompletenessState.MINIMUM,
              objectCommunicationState = AutoruExternal.ObjectCommunicationState.SELECTED,
              claimEntities = Seq.empty
            )
            .some
        )
      )
    }, {
      val creditApplication = sampleAutoruCreditApplciation().copy(
        schedulerLastUpdate = ts.minusDuration(1.minute).some,
        scores = Seq(
          YandexScore(1.some, 1.some, None, None, None, None, None, None, ts, None),
          YandexScore(2.some, 2.some, None, None, None, None, None, None, ts, None),
          OkbScore(1, ts, None),
          OkbScore(2, ts, None)
        )
      )
      ApplyUpdateRequestTestCase(
        description = "Scores update",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          scores = Seq(YandexScore(3.some, 3.some, None, None, None, None, None, None, ts, None), OkbScore(3, ts, None))
        ),
        expected = creditApplication.copy(
          updated = ts,
          scores = Seq(YandexScore(3.some, 3.some, None, None, None, None, None, None, ts, None), OkbScore(3, ts, None))
        )
      )
    }, {
      val creditApplication = sampleAutoruCreditApplciation().copy(forcedSending = None)
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with forcedSending",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource.forTest(forcedSending = true.some).some
        ),
        expected = creditApplication.copy(
          updated = ts,
          forcedSending = true.some
        )
      )
    }, {
      val creditApplication = sampleAutoruCreditApplciation().copy(offers = Seq.empty, garageId = None)
      val garageId = "garageId".some
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with garageId payload",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(Seq.empty, garageId).some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          garageId = garageId
        )
      )
    }, {
      val offers = Seq(sampleOffer())
      val creditApplication = sampleAutoruCreditApplciation().copy(offers = offers, garageId = None)
      val garageId = "garageId".some
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with garageId payload when offers non empty",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(offers, garageId).some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          garageId = garageId,
          offers = offers
        )
      )
    }, {
      val offers = Seq(sampleOffer())
      val garageId1 = "garageId1".some
      val garageId2 = "garageId1".some
      val creditApplication = sampleAutoruCreditApplciation().copy(garageId = garageId1)
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource with new garageId",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(offers = offers, garageId = garageId2).some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          garageId = garageId2,
          offers = offers
        )
      )
    }, {
      val offers = Seq(sampleOffer())
      val garageId1 = "garageId1".some
      val garageId2 = "".some
      val creditApplication = sampleAutoruCreditApplciation().copy(garageId = garageId1)
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource when garageId is EmptyString, clears garageId",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(garageId = garageId2, offers = offers).some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          garageId = None,
          offers = offers
        )
      )
    }, {
      val offers = Seq(sampleOffer())
      val garageId = "garageId".some
      val creditApplication = sampleAutoruCreditApplciation().copy(garageId = garageId)
      ApplyUpdateRequestTestCase(
        description = "CreditApplicationSource when None garageId, garageId stays same",
        source = creditApplication,
        updateRequest = CreditApplication.UpdateRequest(
          timestamp = ts,
          source = CreditApplicationSource
            .forTest(
              payload = CreditApplicationSource.AutoruPayload(garageId = None, offers = offers).some
            )
            .some
        ),
        expected = creditApplication.copy(
          updated = ts,
          garageId = garageId,
          offers = offers
        )
      )
    }
  )

  private val addCreditProductsTestCases = {
    val creditApplication = sampleAutoruCreditApplciation().copy(
      claims = Seq.empty,
      offers = Seq.empty,
      schedulerLastUpdate = None,
      requirements = None,
      scores = Seq.empty,
      notifications = Seq.empty,
      info = None,
      borrowerPersonProfile = None
    )
    val creditProductId = "some-credit-product-id".taggedWith[Tag.CreditProductId]
    val creditProduct = sampleCreditProduct().copy(
      id = creditProductId,
      sendDelay = None
    )
    val dealerOffer = sampleOffer().copy(userRef = Some(AutoruDealer(123L)))
    Seq(
      ApplyCreditProductsTestCase(
        description = "Add credit product without delay",
        creditApplication = creditApplication,
        creditProducts = Seq(creditProduct),
        sendDelay = None,
        timestamp = ts,
        check = result => {
          val processAfter = ts.plusDuration(10.minutes)
          val claimOpt = result.claims.find(_.creditProductId == creditProductId)
          assertTrue(
            result.updated == ts,
            claimOpt.get.created == ts,
            claimOpt.get.updated == ts,
            claimOpt.get.state.isDraft,
            result.scheduledAt.contains(processAfter),
            claimOpt.get.processAfter.contains(processAfter)
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit product with explicit delay",
        creditApplication = creditApplication,
        creditProducts = Seq(creditProduct),
        sendDelay = 1.hour.some,
        timestamp = ts,
        check = result => {
          val processAfter = ts.plusDuration(1.hour)
          val claimOpt = result.claims.find(_.creditProductId == creditProductId)
          assertTrue(
            result.updated == ts,
            claimOpt.get.created == ts,
            claimOpt.get.updated == ts,
            claimOpt.get.state.isDraft,
            result.scheduledAt.contains(processAfter),
            claimOpt.get.processAfter.contains(processAfter)
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit product with delay in the product",
        creditApplication = creditApplication,
        creditProducts = Seq(creditProduct.copy(sendDelay = 1.hour.some)),
        sendDelay = None,
        timestamp = ts,
        check = result => {
          val processAfter = ts.plusDuration(1.hour)
          val claimOpt = result.claims.find(_.creditProductId == creditProductId)
          assertTrue(
            result.scheduledAt.contains(processAfter),
            claimOpt.get.processAfter.contains(processAfter)
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit product with both explicit delay and delay in the product",
        creditApplication = creditApplication,
        creditProducts = Seq(creditProduct.copy(sendDelay = 5.minutes.some)),
        sendDelay = 1.hour.some,
        timestamp = ts,
        check = result => {
          val processAfter = ts.plusDuration(1.hour)
          val claimOpt = result.claims.find(_.creditProductId == creditProductId)
          assertTrue(
            result.scheduledAt.contains(processAfter),
            claimOpt.get.processAfter.contains(processAfter)
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit product with different delays in the products",
        creditApplication = creditApplication,
        creditProducts = Seq(
          creditProduct.copy(id = "foo".taggedWith[Tag.CreditProductId], sendDelay = 1.hour.some),
          creditProduct.copy(id = "bar".taggedWith[Tag.CreditProductId], sendDelay = 5.minutes.some),
          creditProduct.copy(id = "baz".taggedWith[Tag.CreditProductId], sendDelay = 20.minutes.some)
        ),
        sendDelay = None,
        timestamp = ts,
        check = result => {
          def claim(id: String) = result.claims.find(_.creditProductId == id)
          assertTrue(
            claim("foo").get.processAfter.contains(ts.plusDuration(1.hour)),
            claim("bar").get.processAfter.contains(ts.plusDuration(5.minutes)),
            claim("baz").get.processAfter.contains(ts.plusDuration(20.minutes)),
            // Shortest delay wins
            result.scheduledAt.contains(ts.plusDuration(5.minutes))
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit product with different idempotency types",
        creditApplication = creditApplication.copy(offers = Seq(dealerOffer)),
        creditProducts = Seq(
          creditProduct.copy(
            id = "foo".taggedWith[Tag.CreditProductId],
            idempotencyType = IdempotencyType.UNKNOWN_IDEMPOTENCY_TYPE
          ),
          creditProduct
            .copy(id = "bar".taggedWith[Tag.CreditProductId], idempotencyType = IdempotencyType.BY_PRODUCT_N_DEALER),
          creditProduct.copy(
            id = "baz".taggedWith[Tag.CreditProductId],
            idempotencyType = IdempotencyType.UNKNOWN_IDEMPOTENCY_TYPE
          )
        ),
        sendDelay = None,
        timestamp = ts,
        check = result => {
          def claim(id: String) = result.claims.find(_.creditProductId == id)
          assertTrue(
            claim("foo").get.idempotencyKey.contains("0:foo".taggedWith[Tag.IdempotencyKey]),
            claim("bar").get.idempotencyKey.contains("1:bar:dealer:123".taggedWith[Tag.IdempotencyKey]),
            claim("baz").get.idempotencyKey.contains("0:baz".taggedWith[Tag.IdempotencyKey])
          )
        }
      ),
      ApplyCreditProductsTestCase(
        description = "Add credit products with existing credit product",
        creditApplication = creditApplication.copy(
          offers = Seq(dealerOffer),
          claims = Seq(
            sampleClaim().copy(
              creditProductId = "bar".taggedWith[Tag.CreditProductId],
              idempotencyKey = "1:bar:dealer:122".taggedWith[Tag.IdempotencyKey].some
            ),
            sampleClaim().copy(
              creditProductId = "foo".taggedWith[Tag.CreditProductId],
              idempotencyKey = None
            )
          )
        ),
        creditProducts = Seq(
          creditProduct.copy(
            id = "foo".taggedWith[Tag.CreditProductId],
            idempotencyType = IdempotencyType.UNKNOWN_IDEMPOTENCY_TYPE
          ),
          creditProduct
            .copy(id = "bar".taggedWith[Tag.CreditProductId], idempotencyType = IdempotencyType.BY_PRODUCT_N_DEALER),
          creditProduct.copy(
            id = "baz".taggedWith[Tag.CreditProductId],
            idempotencyType = IdempotencyType.UNKNOWN_IDEMPOTENCY_TYPE
          )
        ),
        sendDelay = None,
        timestamp = ts,
        check = result => {
          def claimExist(p: Claim => Boolean) = result.claims.exists(p)
          assertTrue(
            claimExist(_.creditProductId == "foo".taggedWith[Tag.CreditProductId]),
            claimExist(_.idempotencyKey.contains("1:bar:dealer:122".taggedWith[Tag.IdempotencyKey])),
            claimExist(_.idempotencyKey.contains("1:bar:dealer:123".taggedWith[Tag.IdempotencyKey])),
            claimExist(_.idempotencyKey.contains("0:baz".taggedWith[Tag.IdempotencyKey]))
          )
        }
      )
    )
  }

  private val applyUpdateRequestTests = applyUpdateRequestTestCases.map {
    case ApplyUpdateRequestTestCase(description, source, updateRequest, expected) =>
      testM(description) {
        val testZ = for {
          updatedCreditApplication <- CreditApplicationUpdater.applyUpdateRequest(source)(updateRequest)
          r = assert(updatedCreditApplication)(equalTo(expected))
        } yield r
        testZ.provideLayer(creditApplicationUpdaterLayer)
      }
  }

  private val addCreditProductsTests = addCreditProductsTestCases.map {
    case ApplyCreditProductsTestCase(description, creditApplication, creditProducts, sendDelay, timestamp, check) =>
      val result = creditApplication.addCreditProducts(creditProducts, timestamp, sendDelay)
      test(description)(check(result))
  }

  override def spec = suite("CreditApplicationUpdater")(
    suite("applyUpdateRequest")(applyUpdateRequestTests: _*),
    suite("addCreditProducts")(addCreditProductsTests: _*)
  )

  private def sampleCreditProduct(): ConsumerCreditProduct = {
    import org.scalacheck.magnolia._
    Arbitraries
      .generate[ConsumerCreditProduct]
      .sample
      .get
      .copy(rateLimit = None)
  }

  // A hack to support `mockable` for a polymorphic service. The type name matches the type parameter in the service
  // definition. This hack won't be necessary with ZIO 2.0.
  // @see zio/zio#3237
  private type T = CreditApplication

  @mockable[NotificationUpdater.Service[CreditApplication]]
  object NotificationUpdaterMock

  private val notificationUpdaterMockLayer: ULayer[NotificationUpdater[CreditApplication]] =
    NotificationUpdaterMock.UpdateState(anything, valueF(_._1)).optional

  private val creditApplicationUpdaterLayer: ULayer[CreditApplicationUpdater] =
    FeaturesTest.test ++ notificationUpdaterMockLayer >>> CreditApplicationUpdater.live(Domain.DOMAIN_AUTO)
}
