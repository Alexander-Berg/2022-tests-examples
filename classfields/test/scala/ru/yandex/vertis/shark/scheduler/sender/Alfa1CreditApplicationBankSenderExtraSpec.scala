package ru.yandex.vertis.shark.scheduler.sender

import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.bank.AlfaBankClient
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.AutoruCreditApplication
import ru.yandex.vertis.shark.model.CreditApplication
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.CreditApplicationBankSender.ShouldProcessResult._
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.{Has, ZIO, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec}
import zio.test.mock.mockable

import java.time.Instant
import scala.concurrent.duration._

/**
  * Extra spec for this specific sender that doesn't rely on external services,
  * doesn't exert design pressure on other specs, and doesn't need an off
  * switch.
  */
object Alfa1CreditApplicationBankSenderExtraSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val sampleClaim = generate[CreditApplication.AutoruClaim].sample.get.copy(
    creditProductId = "alfabank-1".taggedWith[Tag.CreditProductId],
    processAfter = None,
    sentSnapshot = None
  )

  private val sampleApplication = generate[AutoruCreditApplication].sample.get.copy(
    claims = Seq.empty
  )

  private val sampleSentSnapshot = generate[CreditApplication.Claim.SentSnapshot].sample.get

  private val now = Instant.now()

  @mockable[AlfaBankClient.Service]
  private object AlfaBankClientMock

  @mockable[IdGenerator.Service]
  private object IdGeneratorMock

  private val converterLayer = ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) >>>
    AlfaBankConverter.livePil

  private val bankSenderLayer =
    AlfaBankClientMock.empty ++ IdGeneratorMock.empty ++
      // There are some macro issues with mocking this, which are not worth investigating at the moment.
      converterLayer >>> Alfa1CreditApplicationBankSender.live

  private val shouldProcessTestCases: Seq[ShouldProcessTestCase] = Seq(
    ShouldProcessTestCase(
      "no restrictions",
      sampleClaim,
      now,
      sampleApplication.copy(claims = Seq(sampleClaim)),
      expected = ProcessNow
    ),
    ShouldProcessTestCase(
      "processAfter",
      sampleClaim.copy(processAfter = now.plusDuration(3.hours).some),
      now,
      sampleApplication,
      expected = ProcessLater(now.plusDuration(3.hours))
    ),
    ShouldProcessTestCase(
      "processAfter (expired)",
      sampleClaim.copy(processAfter = now.some),
      now,
      sampleApplication,
      expected = ProcessNow
    ),
    ShouldProcessTestCase(
      "this claim with sent date",
      sampleClaim
        .copy(
          id = "this-id".taggedWith[Tag.CreditApplicationClaimId],
          sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
        ),
      now,
      sampleApplication,
      // The claim doesn't block itself
      expected = ProcessNow
    ),
    ShouldProcessTestCase(
      "other claim with sent date (alfabank-1)",
      sampleClaim.copy(id = "this-id".taggedWith[Tag.CreditApplicationClaimId]),
      now,
      sampleApplication.copy(claims =
        Seq(
          sampleClaim.copy(
            id = "other-id".taggedWith[Tag.CreditApplicationClaimId],
            creditProductId = "alfabank-1".taggedWith[Tag.CreditProductId],
            sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
          )
        )
      ),
      expected = ProcessLater(now.plusDuration(19.hours))
    ),
    ShouldProcessTestCase(
      "other claim with sent date (alfabank-2)",
      sampleClaim.copy(id = "this-id".taggedWith[Tag.CreditApplicationClaimId]),
      now,
      sampleApplication.copy(claims =
        Seq(
          sampleClaim.copy(
            id = "other-id".taggedWith[Tag.CreditApplicationClaimId],
            creditProductId = "alfabank-2".taggedWith[Tag.CreditProductId],
            sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
          )
        )
      ),
      expected = ProcessLater(now.plusDuration(19.hours))
    ),
    ShouldProcessTestCase(
      "other claim with sent date (tinkoff-1)",
      sampleClaim.copy(id = "this-id".taggedWith[Tag.CreditApplicationClaimId]),
      now,
      sampleApplication.copy(claims =
        Seq(
          sampleClaim.copy(
            id = "other-id".taggedWith[Tag.CreditApplicationClaimId],
            creditProductId = "tinkoff-1".taggedWith[Tag.CreditProductId],
            sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
          )
        )
      ),
      // Tinkoff claim is ignored.
      expected = ProcessNow
    ),
    ShouldProcessTestCase(
      "processAfter + sent date (processAfter wins)",
      sampleClaim
        .copy(id = "this-id".taggedWith[Tag.CreditApplicationClaimId], processAfter = now.plusDuration(20.hours).some),
      now,
      sampleApplication.copy(claims =
        Seq(
          sampleClaim.copy(
            id = "other-id".taggedWith[Tag.CreditApplicationClaimId],
            sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
          )
        )
      ),
      // (-5 + 24) = 19 < 20
      expected = ProcessLater(now.plusDuration(20.hours))
    ),
    ShouldProcessTestCase(
      "processAfter + sent date (sent date wins)",
      sampleClaim
        .copy(id = "this-id".taggedWith[Tag.CreditApplicationClaimId], processAfter = now.plusDuration(18.hours).some),
      now,
      sampleApplication.copy(claims =
        Seq(
          sampleClaim.copy(
            id = "other-id".taggedWith[Tag.CreditApplicationClaimId],
            sentSnapshot = sampleSentSnapshot.copy(created = now.minusDuration(5.hours)).some
          )
        )
      ),
      // (-5 + 24) = 19 > 18
      expected = ProcessLater(now.plusDuration(19.hours))
    )
  )

  private def testShouldProcess(
      x: ShouldProcessTestCase) = testM(x.description)(
    assertM(
      ZIO
        .access[Has[Alfa1CreditApplicationBankSender]](
          _.get.shouldProcess(
            x.claim,
            x.ts,
            x.application.copy(
              // The provided claim should always be present in the application
              claims = x.application.claims :+ x.claim
            )
          )
        )
        .provideLayer(bankSenderLayer)
    )(equalTo(x.expected))
  )

  override def spec =
    suite("Alfa1CreditApplicationBankSenderExtra")(
      suite("shouldProcess")(
        shouldProcessTestCases.map(testShouldProcess): _*
      )
    )

  private case class ShouldProcessTestCase(
      description: String,
      claim: CreditApplication.Claim,
      ts: Instant,
      application: AutoruCreditApplication,
      expected: CreditApplicationBankSender.ShouldProcessResult)
}
