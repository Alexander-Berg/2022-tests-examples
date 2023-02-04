package ru.yandex.vertis.shark.client.bank.converter.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import org.scalacheck.magnolia
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.Source
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.model.Inn
import ru.yandex.vertis.zio_baker.model.WithValidate.ValidationErrors
import zio.test.Assertion.{equalTo, fails, isSubtype, isTrue}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Task, ZIO}

import java.time.Instant

object TinkoffBankAutoCreditConverterSpec extends DefaultRunnableSpec with CreditApplicationGen {

  import magnolia._
  import ru.yandex.vertis.shark.model.Block._

  private lazy val converter = new TinkoffBankAutoCreditConverter

  private val timestamp = Instant.now()

  override def spec: ZSpec[TestEnvironment, Any] = suite("TinkoffBankAutoCreditConverter")(
    testM("convert full data to tinkoff format") {
      val res = for {
        seq <- ZIO.foreach((1 to 10).toList) { _ =>
          val autoruCreditApplication = sampleAutoruCreditApplication()
          val claimId = "test-1111".taggedWith[Tag.CreditApplicationClaimId]
          val converterContext =
            AutoConverterContext.forTest(timestamp = timestamp, creditApplication = autoruCreditApplication)
          val context = SenderConverterContext.forTest(converterContext)
          val source = Source(context, claimId)
          converter.convert(source)
        }
      } yield seq.forall(_.nonEmpty)
      assertM(res)(isTrue)
    },
    testM("check car needed") {
      val autoruCreditApplication = sampleAutoruCreditApplication()
      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      val profile =
        autoruCreditApplication.borrowerPersonProfile.get
          .asInstanceOf[PersonProfileImpl]
          .copy(driverLicense = NoDriverLicenseBlock.some)
      val autoruCreditApplicationWithErrors = autoruCreditApplication.copy(borrowerPersonProfile = profile.some)
      val claimId = "test-1111".taggedWith[Tag.CreditApplicationClaimId]
      val converterContext =
        AutoConverterContext
          .forTest(timestamp = timestamp, creditApplication = autoruCreditApplicationWithErrors)
      val context = SenderConverterContext.forTest(converterContext)
      val source = Source(context, claimId)
      val res = for {
        fields <- converter.convert(source)
      } yield fields

      assertM(res.run)(fails(isSubtype[ValidationErrors](Assertion.anything)))
    },
    testM("check work other needed") {

      def prepare = Task.effect {
        val sample = sampleAutoruCreditApplication()
        val employmentBlock =
          NotEmployedEmploymentBlock(proto.Block.EmploymentBlock.NotEmployed.Reason.UNKNOWN_REASON, None)
        @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
        val profile =
          sample.borrowerPersonProfile.get.asInstanceOf[PersonProfileImpl].copy(employment = employmentBlock.some)
        sample.copy(borrowerPersonProfile = profile.some)
      }

      val res = for {
        autoruCreditApplicationWithErrors <- prepare
        claimId = "test-1111".taggedWith[Tag.CreditApplicationClaimId]
        converterContext = AutoConverterContext
          .forTest(timestamp = timestamp, creditApplication = autoruCreditApplicationWithErrors)
        context = SenderConverterContext.forTest(converterContext)
        source = Source(context, claimId)
        fields <- converter.convert(source)
      } yield fields
      assertM(res.run)(fails(isSubtype[ValidationErrors](Assertion.anything)))
    },
    testM("check work phone needed if working") {
      val autoruCreditApplication = sampleAutoruCreditApplication()
      val employmentBlock =
        EmployeeEmploymentBlock(
          "ddd",
          Arbitraries.generate[Inn].sample.get,
          2,
          Arbitraries.generate[Seq[Okved]].sample.get,
          Arbitraries.generate[Entity.AddressEntity].sample.get,
          Seq(),
          proto.Block.EmploymentBlock.Employed.Employee.PositionType.IT_SPECIALIST,
          10.taggedWith[Tag.MonthAmount]
        )
      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      val profile = autoruCreditApplication.borrowerPersonProfile.get
        .asInstanceOf[PersonProfileImpl]
        .copy(employment = employmentBlock.some)
      val autoruCreditApplicationWithErrors = autoruCreditApplication.copy(borrowerPersonProfile = profile.some)
      val claimId = "test-1111".taggedWith[Tag.CreditApplicationClaimId]
      val converterContext =
        AutoConverterContext
          .forTest(timestamp = timestamp, creditApplication = autoruCreditApplicationWithErrors)
      val context = SenderConverterContext.forTest(converterContext)
      val source = Source(context, claimId)
      val res = for {
        fields <- converter.convert(source)
      } yield fields

      assertM(res.run)(fails(isSubtype[ValidationErrors](Assertion.anything)))
    },
    test("check latin to normal digits conversion") {
      val str = "MCMXCIV DLV"
      val res = TinkoffBankConverterBase.convertToDecimalIfNeeded(str)
      assert(res)(equalTo("1994 555"))
    },
    test("check reason conversion") {
      val str = "russ сбежал из дурррки !!!11111"
      val res = TinkoffBankConverterBase.cleanupNotEmploymentReason(str.some)
      assert(res)(equalTo("сбежал из дурррки".some))
    },
    test("check reason default") {
      val str = "russ sbezhal iz durrki   !!!11111"
      val res = TinkoffBankConverterBase.cleanupNotEmploymentReason(str.some)
      assert(res)(equalTo("Другая причина".some))
    }
  )
}
