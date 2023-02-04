package ru.yandex.realty.rent.stage.user

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import org.joda.time.format.{DateTimeFormat => JodaDateTimeFormat}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import ru.yandex.realty.rent.clients.spectrumdata.passport.{
  PassportActualCheckPerson,
  PassportActualCheckPersonPassport,
  PassportActualContent,
  PassportVerificationCheckPerson,
  PassportVerificationContent,
  PassportVerificationMatchResult,
  PassportVerificationRawQuery,
  PassportVerificationVerifyPerson
}
import org.scalatestplus.junit.JUnitRunner
import org.scalamock.scalatest.MockFactory
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.JsResultException
import realty.palma.rent_user.{PassportData, RentUser}
import realty.palma.rent_user_black_list.RentUserBlackList
import realty.palma.spectrum_report.SpectrumReport
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.{
  PalmaClient,
  PalmaFilter,
  PalmaListing,
  PalmaPagination,
  PalmaSorting
}
import ru.yandex.realty.events.Event

import scala.collection.JavaConverters._
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.phone.PhoneGenerators
import ru.yandex.realty.rent.clients.spectrumdata.SpectrumDataClient
import ru.yandex.realty.rent.clients.spectrumdata.common.{
  ReportContentState,
  ReportResponseItem,
  ReportState,
  SpectrumDataReportResponse,
  SpectrumDataReportResponseData
}
import ru.yandex.realty.rent.clients.spectrumdata.extremist.{
  ExtremistsActualContent,
  ExtremistsContent,
  ExtremistsContentCheckPerson
}
import ru.yandex.realty.rent.clients.spectrumdata.fssp.{
  ExecutiveContent,
  ProceedingContent,
  ProceedingExecutiveCheckPerson,
  ProceedingExecutiveContent,
  ProceedingExecutiveItem
}
import ru.yandex.realty.rent.clients.spectrumdata.wanted.{
  WantedActualContent,
  WantedContent,
  WantedContentCheckPerson,
  WantedContentItem
}
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.realty.rent.model.User
import ru.yandex.realty.rent.model.enums.PassportVerificationStatus
import ru.yandex.realty.rent.proto.model.user.{PersonalDataTransferAgreement, TenantQuestionnaire, UserData}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.rent.backend.NaturalPersonEventExportManager
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckResolutionNamespace.NaturalPersonCheckResolution
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckResolutionNamespace.NaturalPersonCheckResolution._
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckStatusNamespace.NaturalPersonCheckStatus
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckStatusNamespace.NaturalPersonCheckStatus._
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckTypeNamespace.NaturalPersonCheckType
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckTypeNamespace.NaturalPersonCheckType._
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.{NaturalPersonCheck, NaturalPersonChecks}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.rent.clients.spectrumdata.SpectrumReportUtil._
import ru.yandex.realty.rent.proto.api.common.TenantQuestionnaire.PersonalActivity
import ru.yandex.realty.util.TestDataSettings
import ru.yandex.vertis.application.runtime.RuntimeConfig

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class NaturalPersonCheckStageSpec extends AsyncSpecBase with RealtyGenerators {

  import NaturalPersonCheckStageSpec._
  import NaturalPersonCheck.CheckCase._

  implicit val traced: Traced = Traced.empty
  implicit val pConfig: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(15, Millis))

  private def invokeStage(
    user: User
  ): ProcessingState[User] = {
    val state = ProcessingState(user)
    val stage = new NaturalPersonCheckStage(
      spectrumDataClient,
      palmaRentUserClient,
      blacklistPalmaClient,
      spectrumDataPalmaClient,
      eventExportManager,
      testDataSettings,
      runtimeConfig
    )
    stage.process(state).futureValue
  }

  "NaturalPersonCheckStage" should {
    "create checks in progress for user without DataTransferAgreement" in {
      val userTemplate = getUser
      val user = userTemplate.copy(
        data = userTemplate.data.toBuilder.clearPersonalDataTransferAgreement().build()
      )
      handleMocks(user)
      val newState = invokeStage(user)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 1
      naturalPersonChecks.getStatus shouldBe NaturalPersonCheckStatus.READY
      naturalPersonChecks.getResolution shouldBe VALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
      assert(newState.entry.visitTime.isEmpty)
    }
    "create checks in progress for user with questionnaire" in {
      val user = getUser
      handleMocks(user)
      val newState = invokeStage(user)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe NaturalPersonCheckStatus.IN_PROGRESS
      naturalPersonChecks.getResolution shouldBe CHECK_RESOLUTION_UNKNOWN
      naturalPersonChecks.getOldChecksCount shouldBe 0
      naturalPersonChecks.getChecksList.asScala
        .filter(_.getCheckCase != BLACK_LIST_CHECK)
        .foreach { check =>
          getPalmaReportId(check) should be(empty)
          getReportId(check) shouldNot be(empty)
        }
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
      assert(newState.entry.visitTime.nonEmpty)
    }

    "create checks in progress for user without questionnaire" in {
      val actualUser = getUser.copy(data = getUserDataWithoutTenantQuestionnaire)
      handleMocks(actualUser)
      val newState = invokeStage(actualUser)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 5
      naturalPersonChecks.getStatus shouldBe IN_PROGRESS
      naturalPersonChecks.getResolution shouldBe CHECK_RESOLUTION_UNKNOWN
      naturalPersonChecks.getOldChecksCount shouldBe 0
      naturalPersonChecks.getChecksList.asScala
        .filter(_.getCheckCase != BLACK_LIST_CHECK)
        .foreach(check => {
          getPalmaReportId(check) should be(empty)
          getReportId(check) shouldNot be(empty)
        })
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
      assert(newState.entry.visitTime.nonEmpty)
    }

    "create valid checks for user with questionnaire" in {
      val user = getUser
      handleMocks(user)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe VALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      naturalPersonChecks.getChecksList.asScala
        .filter(_.getCheckCase != BLACK_LIST_CHECK)
        .foreach { check =>
          getPalmaReportId(check) shouldNot be(empty)
          getReportId(check) shouldNot be(empty)
        }
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks valid for user without questionnaire" in {
      val actualUser = getUser.copy(data = getUserDataWithoutTenantQuestionnaire)
      handleMocks(actualUser)
      val state = invokeStage(actualUser)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 5
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe VALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      naturalPersonChecks.getChecksList.asScala
        .filter(_.getCheckCase != BLACK_LIST_CHECK)
        .foreach { check =>
          getPalmaReportId(check) shouldNot be(empty)
          getReportId(check) shouldNot be(empty)
        }
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with error in creation" in {
      val user = getUser
      handleMocks(user = user, hasCreationError = true)
      val newState = invokeStage(user)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe NaturalPersonCheckStatus.ERROR
      naturalPersonChecks.getResolution shouldBe CHECK_RESOLUTION_UNKNOWN
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, ERROR)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
      assert(newState.entry.visitTime.nonEmpty)
    }

    "create checks with error in getting report" in {
      val user = getUser
      handleMocks(user, hasGettingError = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe ERROR
      naturalPersonChecks.getResolution shouldBe CHECK_RESOLUTION_UNKNOWN
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, ERROR)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with invalid passport verification" in {
      val user = getUser
      handleMocks(user = user, isInvalidPassportVerification = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe INVALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, INVALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with invalid wanted" in {
      val user = getUser
      handleMocks(user = user, isInvalidWanted = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe INVALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, INVALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with invalid extremists" in {
      val user = getUser
      handleMocks(user = user, isInvalidExtremist = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe INVALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, INVALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with invalid fssp" in {
      val user = getUser
      handleMocks(user = user, isInvalidFssp = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe VALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, INVALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with fssp in progress" in {
      val user = getUser
      handleMocks(user = user, isFsspInProgress = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe IN_PROGRESS
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, IN_PROGRESS)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with fssp in progress with refreshing" in {
      val user = getUser
      handleMocks(user = user, isFsspInProgress = true, refreshAmount = 1)
      val state = invokeStage(user)
      val fsspCheckOpt = state.entry.data.getNaturalPersonChecks.getChecksList.asScala
        .find(_.getCheckCase == FSSP_DEBT_CHECK)
      fsspCheckOpt.isDefined shouldBe true
      val updatedCheck =
        fsspCheckOpt.get.toBuilder.setUpdateTime(DateTimeFormat.write(DateTimeUtil.now().minusMinutes(45))).build()
      val newState = invokeStage(state.entry.withUpdatedCheck(updatedCheck))
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe IN_PROGRESS
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, IN_PROGRESS, refreshAmount = 1)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with fssp in progress with exausted refresh amount" in {
      val user = getUser
      handleMocks(user = user, isFsspInProgress = true)
      val state = invokeStage(user)
      val fsspCheckOpt = state.entry.data.getNaturalPersonChecks.getChecksList.asScala
        .find(_.getCheckCase == FSSP_DEBT_CHECK)
      fsspCheckOpt.isDefined shouldBe true
      val updatedCheck = fsspCheckOpt.get.toBuilder
        .setCreateTime(DateTimeFormat.write(DateTimeUtil.now().minusMinutes(30)))
        .setRefreshAmount(5)
        .build()
      val newState = invokeStage(state.entry.withUpdatedCheck(updatedCheck))
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe ERROR
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, VALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, ERROR, refreshAmount = 5)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }

    "create checks with invalid passport" in {
      val user = getUser
      handleMocks(user = user, isInvalidPassportActual = true)
      val state = invokeStage(user)
      val newState = invokeStage(state.entry)
      val naturalPersonChecks = newState.entry.data.getNaturalPersonChecks
      naturalPersonChecks.getChecksCount shouldBe 6
      naturalPersonChecks.getStatus shouldBe READY
      naturalPersonChecks.getResolution shouldBe INVALID
      naturalPersonChecks.getOldChecksCount shouldBe 0
      check(naturalPersonChecks, PASSPORT_ACTUAL, PASSPORT_ACTUAL_CHECK, READY, INVALID)
      check(naturalPersonChecks, PASSPORT_VERIFICATION, PASSPORT_VERIFICATION_CHECK, READY, VALID)
      check(naturalPersonChecks, WANTED, WANTED_CHECK, READY, VALID)
      check(naturalPersonChecks, EXTREMIST, EXTREMIST_CHECK, READY, VALID)
      check(naturalPersonChecks, FSSP_DEBT, FSSP_DEBT_CHECK, READY, VALID)
      check(naturalPersonChecks, BLACK_LIST, BLACK_LIST_CHECK, READY, VALID)
    }
  }
}

object NaturalPersonCheckStageSpec
  extends MockFactory
  with BasicProtoFormats
  with RealtyGenerators
  with PhoneGenerators {

  private val palmaRentUserClient = mock[PalmaClient[RentUser]]
  private var spectrumDataPalmaClient = mock[PalmaClient[SpectrumReport]]
  private val brokerClient = mock[BrokerClient]
  private val eventExportManager = new NaturalPersonEventExportManager(brokerClient)
  private val blacklistPalmaClient = mock[PalmaClient[RentUserBlackList]]
  private var spectrumDataClient = mock[SpectrumDataClient]
  private val runtimeConfig = mock[RuntimeConfig]
  private val testDataSettings = mock[TestDataSettings]

  private def getUser = generateUser(
    Some(readableString.next),
    UserData
      .newBuilder()
      .setTenantQuestionnaire(
        TenantQuestionnaire
          .newBuilder()
          .setPersonalActivity(
            PersonalActivity
              .newBuilder()
              .setActivity(PersonalActivity.Activity.WORK)
              .setAboutWorkAndPosition(readableString.next)
          )
          .build()
      )
      .setPersonalDataTransferAgreement(
        PersonalDataTransferAgreement
          .newBuilder()
          .setAgreementDate(DateTimeFormat.write(DateTimeUtil.now()))
      )
      .build()
  )

  def getUserDataWithoutTenantQuestionnaire: UserData =
    UserData.newBuilder
      .setPersonalDataTransferAgreement(PersonalDataTransferAgreement.getDefaultInstance)
      .build

  private val birthDate = Some(
    Gen.oneOf(Seq("11.01.1999", "01.11.1998", "28.08.1991", "12.03.1996", "31.12.2999", "11.05.1900")).next
  )
  private val passportSeries: String = readableString.next
  private val passportNumber: String = readableString.next

  private def birthDay =
    birthDate
      .map(d => Timestamps.fromMillis(JodaDateTimeFormat.forPattern("dd.MM.yyyy").parseDateTime(d).getMillis))
      .map(t => Timestamp(t.getSeconds, t.getNanos))

  private val validInProgressFsspResponse = getResponse(ProceedingExecutiveContent(None), isProgress = true)

  private val validFsspResponse = getResponse(
    ProceedingExecutiveContent(
      Some(ProceedingExecutiveCheckPerson(ProceedingContent(ExecutiveContent(Some(Seq.empty)))))
    )
  )

  private val invalidFsspResponse = getResponse(
    ProceedingExecutiveContent(
      Some(
        ProceedingExecutiveCheckPerson(
          ProceedingContent(
            ExecutiveContent(
              Some(
                Seq(
                  ProceedingExecutiveItem(
                    debtBalanceEp = Some(2000.0d),
                    debtBalanceBudg = Some(2000.0d),
                    debtBalanceOther = Some(2000.0d),
                    debtBalanceDuty = Some(2000.0d),
                    debtBalanceFine = Some(2001.0d)
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  private val validWantedResponse =
    getResponse(WantedActualContent(Some(WantedContentCheckPerson(WantedContent(Some(0), Some(Seq.empty))))))

  private val invalidWantedResponse =
    getResponse(
      WantedActualContent(
        Some(WantedContentCheckPerson(WantedContent(Some(0), Some(Seq(WantedContentItem(None, None, None, None))))))
      )
    )

  private val validExtremistsResponse = getResponse(
    ExtremistsActualContent(
      Some(
        ExtremistsContentCheckPerson(
          ExtremistsContent(
            found = Some(false),
            isActive = Some(false),
            version = None,
            score = Some(0.5),
            items = Some(Seq.empty)
          )
        )
      )
    )
  )

  private val invalidExtremistsResponse = getResponse(
    ExtremistsActualContent(
      Some(
        ExtremistsContentCheckPerson(
          ExtremistsContent(
            found = Some(false),
            isActive = Some(true),
            version = None,
            score = Some(1.0),
            items = Some(Seq.empty)
          )
        )
      )
    )
  )

  private def validPassportActualResponse(number: String, series: String) = getResponse(
    PassportActualContent(
      Some(
        PassportActualCheckPerson(
          PassportActualCheckPersonPassport(
            number = Some(number),
            series = Some(series),
            expired = Some(false),
            details = None
          )
        )
      )
    )
  )

  private def invalidPassportActualResponse(number: String, series: String) = getResponse(
    PassportActualContent(
      Some(
        PassportActualCheckPerson(
          PassportActualCheckPersonPassport(
            number = Some(number),
            series = Some(series),
            expired = Some(true),
            details = None
          )
        )
      )
    )
  )

  private def validVerificationResponse(lastName: String, firstName: String) = getResponse(
    PassportVerificationContent(
      Some(
        PassportVerificationCheckPerson(
          PassportVerificationVerifyPerson(
            matchResult = Some(PassportVerificationMatchResult.MatchFound),
            description = Some(""),
            rawQuery = Some(PassportVerificationRawQuery(lastName, firstName, None, None, None, None))
          )
        )
      )
    )
  )

  private def invalidVerificationResponse(lastName: String, firstName: String) = getResponse(
    PassportVerificationContent(
      Some(
        PassportVerificationCheckPerson(
          PassportVerificationVerifyPerson(
            matchResult = Some(PassportVerificationMatchResult.MatchNotFound),
            description = Some(""),
            rawQuery = Some(PassportVerificationRawQuery(lastName, firstName, None, None, None, None))
          )
        )
      )
    )
  )

  private def generateUser(phone: Option[String], data: UserData = UserData.getDefaultInstance) = User(
    uid = Gen.posNum[Long].next,
    userId = uidGen().next,
    phone = phone,
    name = Some(readableString.next),
    surname = Some(readableString.next),
    patronymic = Some(readableString.next),
    fullName = Gen.option(readableString).next,
    email = Some(uidGen().next),
    passportVerificationStatus = PassportVerificationStatus.Saved,
    roommateLinkId = None,
    roommateLinkExpirationTime = None,
    assignedFlats = Map.empty,
    data = data,
    createTime = DateTime.now(),
    updateTime = DateTime.now(),
    visitTime = None
  )

  private def creationResponse(reportId: String): SpectrumDataReportResponse[ReportResponseItem] =
    SpectrumDataReportResponse(
      ReportState.Ok,
      "",
      Some(1),
      Seq(ReportResponseItem(reportId, isnew = true, None, "")),
      None
    )

  private def getResponse[T](
    data: T,
    isProgress: Boolean = false
  ): SpectrumDataReportResponse[SpectrumDataReportResponseData[T]] =
    SpectrumDataReportResponse(
      ReportState.Ok,
      "",
      Some(1),
      Seq(
        SpectrumDataReportResponseData(
          readableString.next,
          readableString.next,
          if (isProgress) 0 else 1,
          if (isProgress) 1 else 0,
          0,
          ReportContentState(Seq.empty, None),
          Some(data)
        )
      ),
      None
    )

  private def handleMocks(
    user: User,
    hasCreationError: Boolean = false,
    hasGettingError: Boolean = false,
    isInvalidPassportActual: Boolean = false,
    isInvalidPassportVerification: Boolean = false,
    isInvalidWanted: Boolean = false,
    isInvalidExtremist: Boolean = false,
    isInvalidFssp: Boolean = false,
    isFsspInProgress: Boolean = false,
    refreshAmount: Int = 0
  ) = {
    val firstName = user.name.get
    val lastName = user.surname.get
    val middleName = user.patronymic
    spectrumDataPalmaClient = mock[PalmaClient[SpectrumReport]]
    spectrumDataClient = mock[SpectrumDataClient]
    (palmaRentUserClient
      .get(_: String)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(
        Future.successful(Some(RentUser(passportData = Some(PassportData(birthDay, passportSeries, passportNumber)))))
      )
    (blacklistPalmaClient
      .list(_: Seq[PalmaFilter], _: Option[PalmaPagination], _: Option[PalmaSorting])(_: Traced))
      .expects(where { (filters, _, _, _) =>
        filters.exists(_.fieldName == "passport_hash")
      })
      .anyNumberOfTimes()
      .returning(Future.successful(PalmaListing(Nil, "")))
    (spectrumDataPalmaClient
      .create(_: SpectrumReport)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(Future.successful(SpectrumReport()))
    if (refreshAmount > 0) {
      (spectrumDataClient
        .refreshReport(_: String)(_: Traced))
        .expects(*, *)
        .repeat(refreshAmount)
        .returning(Future.successful(creationResponse(readableString.next)))
    }
    (brokerClient
      .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
      .expects(*, *, *, *)
      .anyNumberOfTimes()
      .returning(Future.unit)
    (runtimeConfig.isEnvironmentStable _)
      .expects()
      .anyNumberOfTimes()
      .returning(true)
    (testDataSettings
      .canDoExtremelyExpensiveThing(_: String))
      .expects(*)
      .anyNumberOfTimes()
      .returning(true)

    handleCreationMocks(firstName, lastName, middleName, hasCreationError)
    handleGetReportMocks(
      firstName,
      lastName,
      hasGettingError,
      isInvalidPassportActual,
      isInvalidPassportVerification,
      isInvalidWanted,
      isInvalidExtremist,
      isInvalidFssp,
      isFsspInProgress
    )
  }

  private def handleCreationMocks(
    firstName: String,
    lastName: String,
    middleName: Option[String],
    hasVerificationError: Boolean
  ) = {
    (spectrumDataClient
      .createProceedingExecutiveReport(_: String, _: String, _: Option[String], _: Option[String])(_: Traced))
      .expects(firstName, lastName, middleName, birthDate, *)
      .repeat(1)
      .returning(Future.successful(creationResponse(readableString.next)))
    (spectrumDataClient
      .createWantedReport(_: String, _: String, _: Option[String], _: Option[String])(_: Traced))
      .expects(firstName, lastName, middleName, birthDate, *)
      .repeat(1)
      .returning(Future.successful(creationResponse(readableString.next)))
    (spectrumDataClient
      .createExtremistReport(_: String, _: String, _: Option[String], _: Option[String])(_: Traced))
      .expects(firstName, lastName, middleName, birthDate, *)
      .repeat(1)
      .returning(Future.successful(creationResponse(readableString.next)))
    (spectrumDataClient
      .createPassportActualReport(_: String, _: String)(_: Traced))
      .expects(passportSeries, passportNumber, *)
      .repeat(1)
      .returning(Future.successful(creationResponse(readableString.next)))
    (spectrumDataClient
      .createPassportVerificationReport(
        _: String,
        _: String,
        _: Option[String],
        _: Option[String],
        _: Option[String],
        _: Option[String]
      )(_: Traced))
      .expects(
        lastName,
        firstName,
        middleName,
        birthDate,
        Some(passportSeries + passportNumber),
        None,
        *
      )
      .repeat(1)
      .returning(
        if (hasVerificationError) Future.failed(JsResultException(Seq.empty))
        else Future.successful(creationResponse(readableString.next))
      )
  }

  private def handleGetReportMocks(
    firstName: String,
    lastName: String,
    hasWantedError: Boolean,
    isInvalidPassportActual: Boolean,
    isInvalidPassportVerification: Boolean,
    isInvalidWanted: Boolean,
    isInvalidExtremist: Boolean,
    isInvalidFssp: Boolean,
    isFsspInProgress: Boolean
  ) = {
    (spectrumDataClient
      .getProceedingExecutiveReport(_: String)(_: Traced))
      .expects(*, *)
      .repeat(1)
      .returning(
        Future.successful(
          if (isInvalidFssp) invalidFsspResponse
          else if (isFsspInProgress) validInProgressFsspResponse
          else validFsspResponse
        )
      )
    (spectrumDataClient
      .getWantedReport(_: String)(_: Traced))
      .expects(*, *)
      .repeat(1)
      .returning(
        if (hasWantedError) Future.failed(JsResultException(Seq.empty))
        else Future.successful(if (isInvalidWanted) invalidWantedResponse else validWantedResponse)
      )
    (spectrumDataClient
      .getExtremistReport(_: String)(_: Traced))
      .expects(*, *)
      .repeat(1)
      .returning(Future.successful(if (isInvalidExtremist) invalidExtremistsResponse else validExtremistsResponse))
    (spectrumDataClient
      .getPassportActualReport(_: String)(_: Traced))
      .expects(*, *)
      .repeat(1)
      .returning(
        Future.successful(
          if (isInvalidPassportActual) invalidPassportActualResponse(passportNumber, passportSeries)
          else validPassportActualResponse(passportNumber, passportSeries)
        )
      )
    (spectrumDataClient
      .getPassportVerificationReport(_: String)(_: Traced))
      .expects(*, *)
      .repeat(1)
      .returning(
        Future.successful(
          if (isInvalidPassportVerification) invalidVerificationResponse(lastName, firstName)
          else validVerificationResponse(lastName, firstName)
        )
      )
  }

  private def check(
    naturalPersonChecks: NaturalPersonChecks,
    checkType: NaturalPersonCheckType,
    checkCase: NaturalPersonCheck.CheckCase,
    status: NaturalPersonCheckStatus,
    resolution: NaturalPersonCheckResolution = CHECK_RESOLUTION_UNKNOWN,
    refreshAmount: Int = 0
  ) = {
    val checkOpt = naturalPersonChecks.getChecksList.asScala
      .find { c =>
        c.getCheckCase == checkCase &&
        c.getStatus == status &&
        c.getResolution == resolution
      }
    assert(checkOpt.isDefined)
    checkOpt.foreach { check =>
      assert(check.getCheckCase == checkCase)
    }
    if (refreshAmount > 0) {
      assert(checkOpt.get.getRefreshAmount == refreshAmount)
    }
  }
}
