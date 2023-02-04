package ru.yandex.vertis.shark.scheduler.stage.credit_application.sender.impl

import ru.yandex.vertis.shark.{TmsMockDadataClient, TmsStaticSamples}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import common.id.IdGenerator
import ru.yandex.vertis.shark.client.bank.{BankClientContext, RosgosstrahBankClient}
import ru.yandex.vertis.shark.client.bank.converter.RosgosstrahBankConverter
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Entities.RosgosstrahStepClaim
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Responses.ClaimConditionResponse.{
  ConditionData,
  Conditions,
  SuccessConditionResponse
}
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Responses.ClaimStatusResponse.{
  Status,
  StatusData,
  SuccessStatusResponse
}
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Responses._
import ru.yandex.vertis.shark.model.CreditApplication.AutoruClaim
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.proto.model.CreditApplication.Claim.ClaimState
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.client.bank.RosgosstrahBankClient.RosgosstrahBankClient
import zio.clock.Clock
import zio.test._
import zio.{Task, ULayer, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext

object Rosgosstrah1CreditApplicationBankSenderSpec extends DefaultRunnableSpec with TmsStaticSamples {

  private val httpMeta = HttpMeta(200.some, "OK".some)
  private val formId = 123
  private val bankClaimId = formId.toString.taggedWith[Tag.CreditApplicationBankClaimId]

  private val clientSuccessResponse = SendResponse(
    errors = None,
    meta = Meta(httpMeta.some),
    data = Seq(FormData(id = 123, meta = Meta(httpMeta.some)))
  )

  private val signInResponse = AuthResponseWithToken(
    token = "token".taggedWith[Tag.Token],
    authResponse = AuthResponse.SuccessAuthResponse("id", "eee@gmail.com", LocalDateTime.now, LocalDateTime.now).some
  )

  private val bankConverterLayer =
    ZLayer.requires[Clock] ++ TmsMockDadataClient.live ++ IdGenerator.random >>>
      RosgosstrahBankConverter.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Rosgosstrah1CreditApplicationBankSender")(
      testM("when send all at once save step as 3 and status NEW") {
        val res = for {
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(step = Rosgosstrah1CreditApplicationBankSender.ZeroStep)
          regAddress = creditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
          resAddress = creditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            registrationAddress = regAddress,
            residenceAddress = resAddress
          )
          context = SenderConverterContext.forTest(converterContext)
          source <- sender.send(claimId)(context)
        } yield source
        val expectation = CreditApplicationClaimSource(
          step = Rosgosstrah1CreditApplicationBankSender.ThirdStep.some,
          state = ClaimState.NEW.some,
          bankClaimId = bankClaimId.some
        ).some
        assertM(res)(Assertion.equalTo(expectation)).provideLayer(bankSenderLayer())
      },
      testM("when rgs-step 2 fails save as step 1") {
        val res = for {
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(step = Rosgosstrah1CreditApplicationBankSender.ZeroStep)
          regAddress = creditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
          resAddress = creditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            registrationAddress = regAddress,
            residenceAddress = resAddress
          )
          context = SenderConverterContext.forTest(converterContext)
          source <- sender.send(claimId)(context)
        } yield source
        val expectation = CreditApplicationClaimSource(
          step = Rosgosstrah1CreditApplicationBankSender.FirstStep.some,
          state = ClaimState.SENDING.some,
          bankClaimId = bankClaimId.some
        ).some
        assertM(res)(Assertion.equalTo(expectation))
          .provideLayer(bankSenderLayer(mockRosgosstrahBankClient(failOnStep = 2.some)))
      },
      testM("when starts on step 1 should finish") {
        val res = for {
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(step = Rosgosstrah1CreditApplicationBankSender.FirstStep)
          regAddress = creditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
          resAddress = creditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
          converterContext = AutoConverterContext.forTest(
            timestamp = timestamp,
            creditApplication = creditApplication,
            registrationAddress = regAddress,
            residenceAddress = resAddress
          )
          context = SenderConverterContext.forTest(converterContext)
          source <- sender.send(claimId)(context)
        } yield source
        val expectation = CreditApplicationClaimSource(
          step = Rosgosstrah1CreditApplicationBankSender.ThirdStep.some,
          state = ClaimState.NEW.some
        ).some
        assertM(res)(Assertion.equalTo(expectation)).provideLayer(bankSenderLayer(mockRosgosstrahBankClient()))
      },
      testM("when client responds with status client_approved update status") {
        val res = for {
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(
            step = Rosgosstrah1CreditApplicationBankSender.FirstStep,
            bankClaimId = "123".taggedWith[Tag.CreditApplicationBankClaimId].some
          )
          source <- sender.check(creditApplication, claimId)
        } yield source
        val expectation = CreditApplicationClaimSource(
          state = ClaimState.APPROVED.some,
          bankState = "client_approved".some,
          approvedMaxAmount = Some(2L.taggedWith[Tag.MoneyRub]),
          approvedTermMonths = Some(2.taggedWith[Tag.MonthAmount]),
          approvedInterestRate = Some(2.2f.taggedWith[Tag.Rate])
        ).some
        assertM(res)(Assertion.equalTo(expectation)).provideLayer(bankSenderLayer(mockRosgosstrahBankClient()))
      },
      testM("when client responds with status loan_issued update status") {
        val res = for {
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(
            step = Rosgosstrah1CreditApplicationBankSender.FirstStep,
            bankClaimId = "123".taggedWith[Tag.CreditApplicationBankClaimId].some
          )
          source <- sender.check(creditApplication, claimId)
        } yield source
        val expectation = CreditApplicationClaimSource(
          state = ClaimState.ISSUE.some,
          bankState = "loan_issued".some,
          approvedMaxAmount = Some(2L.taggedWith[Tag.MoneyRub]),
          approvedTermMonths = Some(2.taggedWith[Tag.MonthAmount]),
          approvedInterestRate = Some(2.2f.taggedWith[Tag.Rate])
        ).some
        assertM(res)(Assertion.equalTo(expectation))
          .provideLayer(bankSenderLayer(mockRosgosstrahBankClient(respondStatus = Status.LoanIssued)))
      },
      testM("save status when condition fails") {
        val res = for {
          sender <- ZIO.service[Rosgosstrah1CreditApplicationBankSender]
          (creditApplication, claimId) = testCreditApplication(
            step = Rosgosstrah1CreditApplicationBankSender.FirstStep,
            bankClaimId = "123".taggedWith[Tag.CreditApplicationBankClaimId].some
          )
          source <- sender.check(creditApplication, claimId)
        } yield source
        val expectation = CreditApplicationClaimSource(
          state = ClaimState.ISSUE.some,
          bankState = "loan_issued".some
        ).some
        assertM(res)(Assertion.equalTo(expectation))
          .provideLayer(
            bankSenderLayer(mockRosgosstrahBankClient(failOnCoditions = true, respondStatus = Status.LoanIssued))
          )
      }
    )

  private def testCreditApplication(
      step: Step,
      bankClaimId: Option[CreditApplicationBankClaimId] = None): (CreditApplication, CreditApplicationClaimId) = {
    val claims = sampleCreditApplication.claims.filter(_.creditProductId != "rosgosstrah-1")
    val rgsbClaim = AutoruClaim.forTest(
      id = "84745b5f-26bf-43e4-8585-098108ec4223".taggedWith[Tag.CreditApplicationClaimId],
      created = Instant.now,
      updated = Instant.now,
      step = step,
      bankClaimId = bankClaimId,
      creditProductId = "rosgosstrah-1".taggedWith[Tag.CreditProductId],
      state = ClaimState.DRAFT,
      bankState = "".some,
      processAfter = Instant.now.some
    )
    val updatedClaims = rgsbClaim +: claims
    (sampleCreditApplication.copy(claims = updatedClaims), rgsbClaim.id)
  }

  private def bankSenderLayer(mockLayer: ULayer[RosgosstrahBankClient] = mockRosgosstrahBankClient()) =
    Clock.live ++ mockLayer ++ bankConverterLayer >+>
      Rosgosstrah1CreditApplicationBankSender.live

  private def mockRosgosstrahBankClient(
      failOnStep: Option[Int] = None,
      failOnCoditions: Boolean = false,
      respondStatus: Status = Status.ClientApproved) =
    ZLayer.succeed(new RosgosstrahBankClient.Service {

      override def signIn()(implicit context: BankClientContext): Task[AuthResponseWithToken] =
        Task.succeed(signInResponse)

      override def sendClaim(
          claim: RosgosstrahStepClaim
        )(implicit token: Token,
          context: BankClientContext): Task[SendResponse] =
        failOnStep match {
          case Some(step) if claim.data.step == step => Task.fail(new Exception)
          case _ => Task.succeed(clientSuccessResponse)
        }

      override def getStatus(
          bankClaimId: CreditApplicationBankClaimId
        )(implicit token: Token,
          context: BankClientContext): Task[ClaimStatusResponse] = Task.succeed(
        SuccessStatusResponse(StatusData(123, respondStatus, "status"), Meta(Some(HttpMeta(Some(200), Some("OK")))))
      )

      override def getConditions(
          bankClaimId: CreditApplicationBankClaimId
        )(implicit token: Token,
          context: BankClientContext): Task[ClaimConditionResponse.SuccessConditionResponse] =
        if (failOnCoditions) {
          Task.fail(new Exception)
        } else {
          Task.succeed(
            SuccessConditionResponse(
              ConditionData(123, Conditions(Some(2.2), Some(2), Some(2.2), Some(2L))),
              Meta(Some(HttpMeta(Some(200), Some("OK"))))
            )
          )
        }
    })
}
