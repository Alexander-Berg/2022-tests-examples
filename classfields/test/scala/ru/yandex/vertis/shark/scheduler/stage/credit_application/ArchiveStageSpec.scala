package ru.yandex.vertis.shark.scheduler.stage.credit_application

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Category, Section}
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.model.CreditApplication.AutoruClaim
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.proto.model.CreditApplication.State
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.scheduler.stage.Stage
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichDuration
import ru.yandex.vertis.zio_baker.zio.features.DurationRange
import zio.ZIO
import zio.test.Assertion.{equalTo, isNone}
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.{Instant, ZoneOffset}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ArchiveStageSpec extends DefaultRunnableSpec with CreditApplicationGen with AutoruOfferGen {

  private val archiveStageLayer = TestClock.any >>> ArchiveStage.live(Domain.DOMAIN_AUTO)

  private val ts = Instant.now()

  private val ExpiredBefore: FiniteDuration = 61.days

  private val actualClaim = AutoruClaim.forTest(
    id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
    bankClaimId = None,
    created = ts,
    updated = ts,
    processAfter = None,
    creditProductId = Tinkoff1CreditApplicationBankSender.CreditProductId,
    state = proto.CreditApplication.Claim.ClaimState.DRAFT,
    bankState = None,
    approvedMaxAmount = None,
    approvedTermMonths = None,
    approvedInterestRate = None,
    approvedMinInitialFeeRate = None,
    offerEntities = Seq(
      AutoruClaim.OfferEntity(
        offer = AutoruCreditApplication.Offer(
          category = Category.CARS,
          section = Section.NEW,
          id = "123-hash".taggedWith[zio_baker.Tag.OfferId],
          sellerType = None,
          userRef = None
        ),
        created = ts,
        updated = ts,
        state = proto.CreditApplication.Claim.ClaimPayload.ObjectState.DRAFT
      )
    )
  )

  private val notActualClaim = AutoruClaim.forTest(
    id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
    bankClaimId = None,
    created = ts.minus(ExpiredBefore.asJava),
    updated = ts.minus(ExpiredBefore.asJava),
    processAfter = None,
    creditProductId = Tinkoff1CreditApplicationBankSender.CreditProductId,
    state = proto.CreditApplication.Claim.ClaimState.DRAFT,
    bankState = None,
    approvedMaxAmount = None,
    approvedTermMonths = None,
    approvedInterestRate = None,
    approvedMinInitialFeeRate = None,
    offerEntities = Seq(
      AutoruClaim.OfferEntity(
        offer = AutoruCreditApplication.Offer(
          category = Category.CARS,
          section = Section.NEW,
          id = "123-hash".taggedWith[zio_baker.Tag.OfferId],
          sellerType = None,
          userRef = None
        ),
        created = ts,
        updated = ts,
        state = proto.CreditApplication.Claim.ClaimPayload.ObjectState.DRAFT
      )
    )
  )

  private val setDateTime = TestClock.setDateTime(ts.atOffset(ZoneOffset.UTC))

  def spec: ZSpec[TestEnvironment, Any] = {
    suite("ArchiveStage")(
      testM("Process already canceled") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.CANCELED,
          claims = Seq(actualClaim)
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result)
        setDateTime *>
          assertM(actual)(isNone).provideLayer(archiveStageLayer)
      },
      testM("Process actual without claims") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.ACTIVE,
          created = ts,
          claims = Seq.empty
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result)
        setDateTime *>
          assertM(actual)(isNone).provideLayer(archiveStageLayer)
      },
      testM("Process actual with claims") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.ACTIVE,
          claims = Seq(actualClaim)
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result)
        setDateTime *>
          assertM(actual)(isNone).provideLayer(archiveStageLayer)
      },
      testM("Process active but not actual without claims") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.ACTIVE,
          created = ts.minus(ExpiredBefore.asJava),
          claims = Seq.empty
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result.flatMap(_.source.flatMap(_.state)))
        setDateTime *>
          assertM(actual)(equalTo(proto.CreditApplication.State.CANCELED.some)).provideLayer(archiveStageLayer)
      },
      testM("Process active but not actual with claims") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.ACTIVE,
          claims = Seq(notActualClaim)
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result.flatMap(_.source.flatMap(_.state)))
        setDateTime *>
          assertM(actual)(equalTo(proto.CreditApplication.State.CANCELED.some)).provideLayer(archiveStageLayer)
      }
    )
  }

  val ScheduleDurationRange: DurationRange = DurationRange(1.minute, 2.minutes)
}
