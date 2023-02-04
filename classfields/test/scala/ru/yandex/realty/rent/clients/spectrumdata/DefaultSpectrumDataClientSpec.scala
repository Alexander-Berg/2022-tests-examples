package ru.yandex.realty.rent.clients.spectrumdata

import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.rent.clients.spectrumdata.SpectrumDataJsonGen.{score, _}
import ru.yandex.realty.rent.clients.spectrumdata.common.{
  ReportContentSourceState,
  ReportContentState,
  ReportResponseItem,
  ReportState,
  SpectrumDataReportResponse,
  SpectrumDataReportResponseData
}
import ru.yandex.realty.rent.clients.spectrumdata.extremist.{
  ExtremistsActualContent,
  ExtremistsContent,
  ExtremistsContentCheckPerson,
  ExtremistsContentItem,
  ExtremistsContentPerson,
  ExtremistsEvidence
}
import ru.yandex.realty.rent.clients.spectrumdata.fssp.{
  ExecutiveContent,
  ProceedingContent,
  ProceedingExecutiveCheckPerson,
  ProceedingExecutiveContent,
  ProceedingExecutiveItem,
  RecordStatus
}
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
import ru.yandex.realty.rent.clients.spectrumdata.wanted.{
  WantedActualContent,
  WantedBaseInvestigation,
  WantedBirth,
  WantedContactInformation,
  WantedContent,
  WantedContentCheckPerson,
  WantedContentItem,
  WantedImage,
  WantedPerson
}
import DefaultSpectrumDataClient._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class DefaultSpectrumDataClientSpec
  extends SpecBase
  with AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock {

  private val tokenMaker = new SpectrumDataTokenMaker("test@test", "123")
  private val client = new DefaultSpectrumDataClient(httpService, tokenMaker)

  def createReport(reportTypeUid: String, request: String, clientResponse: String)(
    responseProvider: () => SpectrumDataReportResponse[ReportResponseItem]
  ): Assertion = {
    httpClient.expect(POST, s"/b2b/api/v1/user/reports/$reportTypeUid/_make")
    httpClient.expectJson(request)
    httpClient.respondWithJson(clientResponse)
    val response = responseProvider()
    response.state shouldBe ReportState.Ok
    response.size shouldEqual (Some(1))
    response.event shouldBe (None)
    response.stamp shouldEqual stamp
    response.data shouldEqual Seq(
      ReportResponseItem(
        uid = reportUid,
        isnew = isNew,
        processRequestUid = Some(processRequestUid),
        suggestGet = suggestedGet
      )
    )
  }

  def getReport[T](reportId: String, report: String, content: T)(
    reportProvider: String => SpectrumDataReportResponse[SpectrumDataReportResponseData[T]]
  ): Assertion = {
    httpClient.expect(GET, s"/b2b/api/v1/user/reports/$reportId?_content=true")
    httpClient.respondWithJson(report)
    val response = reportProvider(reportId)
    response.state shouldBe ReportState.Ok
    response.size shouldEqual Some(1)
    response.event shouldBe None
    response.stamp shouldEqual stamp
    response.data shouldEqual Seq(
      SpectrumDataReportResponseData(
        domainUid = domainUid,
        reportTypeUid = reportTypeUid,
        progressOk = 1,
        progressWait = 0,
        progressError = 0,
        state = ReportContentState(
          Seq(ReportContentSourceState("some_source", "OK", Some(Json.obj()))),
          Some(Json.obj())
        ),
        content = Some(content)
      )
    )
  }

  "DefaultSpectrumDataClient" should {
    "successfully perform create passport active report request" in {
      createReport(passportActualCheck, createPassportReportRequest, createReportResponse) { () =>
        client.createPassportActualReport(passportSeries, passportNumber).futureValue
      }
    }

    "should successfully get passport actual report" in {
      getReport(
        "425679",
        passportReport,
        PassportActualContent(
          Some(
            PassportActualCheckPerson(
              PassportActualCheckPersonPassport(
                number = Some(passportNumber),
                series = Some(passportSeries),
                expired = Some(expired),
                details = Some(passportDetails)
              )
            )
          )
        )
      ) { reportId =>
        client
          .getPassportActualReport(reportId)
          .futureValue
      }
    }

    "should successfully get in process passport actual report" in {
      getReport("425679", emptyReport, PassportActualContent(None)) { reportId =>
        client
          .getPassportActualReport(reportId)
          .futureValue
      }
    }

    "successfully create verify person report" in {
      createReport(passportVerificationCheck, verifyPassportRequest, createReportResponse) { () =>
        client
          .createPassportVerificationReport(
            lastName = lastName,
            firstName = firstName,
            patronymic = Some(patronymic),
            birth = Some(birthDay),
            passport = Some(passportSeries + passportNumber),
            phone = Some(phone)
          )
          .futureValue
      }
    }

    "should successfully get verify person report" in {
      getReport(
        "425680",
        verifyPassportReport,
        PassportVerificationContent(
          Some(
            PassportVerificationCheckPerson(
              PassportVerificationVerifyPerson(
                matchResult = Some(PassportVerificationMatchResult.MatchFound),
                description = Some(verifyPassportDescription),
                rawQuery = Some(
                  PassportVerificationRawQuery(
                    lastName = lastName,
                    firstName = firstName,
                    middleName = Some(patronymic),
                    phoneNumber = Some(phone),
                    passportNumber = Some(passportSeries + passportNumber),
                    birthDate = Some(birthDate)
                  )
                )
              )
            )
          )
        )
      ) { reportId =>
        client
          .getPassportVerificationReport(reportId)
          .futureValue
      }
    }

    "should successfully get in process verify report" in {
      getReport("425680", emptyReport, PassportVerificationContent(None)) { reportId =>
        client
          .getPassportVerificationReport(reportId)
          .futureValue
      }
    }

    "successfully perform create wanted report request" in {
      createReport(wantedCheck, createPersonRequest, createReportResponse) { () =>
        client
          .createWantedReport(firstName, lastName, Some(patronymic), Some(birthDay))
          .futureValue
      }
    }

    "should successfully get wanted report" in {
      getReport(
        "425681",
        wantedReport,
        WantedActualContent(
          Some(
            WantedContentCheckPerson(
              WantedContent(
                Some(1),
                Some(
                  Seq(
                    WantedContentItem(
                      Some(
                        WantedPerson(
                          firstName,
                          lastName,
                          Some(patronymic),
                          Some(gender),
                          Some(nationality),
                          Some(WantedBirth(Some(birthDay), Some(place))),
                          Some(WantedImage(Some("")))
                        )
                      ),
                      Some(WantedBaseInvestigation(investigation)),
                      Some(WantedContactInformation(phone)),
                      Some(region)
                    )
                  )
                )
              )
            )
          )
        )
      ) { reportId =>
        client
          .getWantedReport(reportId)
          .futureValue
      }
    }

    "should successfully get in progress wanted report" in {
      getReport("425681", emptyReport, WantedActualContent(None)) { reportId =>
        client
          .getWantedReport(reportId)
          .futureValue
      }
    }

    "successfully perform create extremists report request" in {
      createReport(extremistsCheck, createPersonRequest, createReportResponse) { () =>
        client
          .createExtremistReport(firstName, lastName, Some(patronymic), Some(birthDay))
          .futureValue
      }
    }

    "should successfully get extremists report" in {
      getReport(
        "425682",
        extremistsReport,
        ExtremistsActualContent(
          Some(
            ExtremistsContentCheckPerson(
              ExtremistsContent(
                found = Some(true),
                isActive = Some(false),
                Some(version),
                Some(score),
                Some(
                  Seq(
                    ExtremistsContentItem(
                      matched_middle_name = Some(true),
                      matched_birth_date = Some(true),
                      matched_region = Some(false),
                      score = Some(score),
                      isActive = Some(false),
                      isAdded = Some(false),
                      isDeleted = Some(true),
                      Some(
                        ExtremistsContentPerson(
                          Some(lastName),
                          Some(firstName),
                          Some(patronymic),
                          Some(birthDate),
                          Some(place),
                          Some(ExtremistsEvidence(Some(version), Some(url)))
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      ) { reportId =>
        client
          .getExtremistReport(reportId)
          .futureValue
      }
    }

    "should successfully get in progress extremists report" in {
      getReport("425682", emptyReport, ExtremistsActualContent(None)) { reportId =>
        client
          .getExtremistReport(reportId)
          .futureValue
      }
    }

    "successfully perform create proceeding executive report request" in {
      createReport(executiveProceedingCheck, createPersonRequest, createReportResponse) { () =>
        client
          .createProceedingExecutiveReport(firstName, lastName, Some(patronymic), Some(birthDay))
          .futureValue
      }
    }

    "should successfully get proceeding executive report" in {
      getReport(
        "425683",
        proceedingExecutiveReport,
        ProceedingExecutiveContent(
          Some(
            ProceedingExecutiveCheckPerson(
              ProceedingContent(
                ExecutiveContent(
                  Some(
                    Seq(
                      ProceedingExecutiveItem(
                        Some(date),
                        Some(number),
                        Some(name),
                        Some(numberComp),
                        Some(place),
                        Some(typeCode),
                        Some(typeName),
                        Some(number),
                        Some(date),
                        Some(riseDate),
                        Some(subjectType),
                        Some(subjectTypeName),
                        Some(issuer),
                        Some(bailiffOfficeCode),
                        Some(bailiffOfficeAddress),
                        Some(bailiffName),
                        Some(bailiffPhone),
                        Some(balance),
                        Some(balanceFine),
                        Some(balanceDuty),
                        Some(balanceBudget),
                        Some(balanceOther),
                        Some(RecordStatus.Closed),
                        Some(endDate)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      ) { reportId =>
        client
          .getProceedingExecutiveReport(reportId)
          .futureValue
      }
    }

    "should successfully get in progress proceeding executive report" in {
      getReport("425683", emptyReport, ProceedingExecutiveContent(None)) { reportId =>
        client
          .getProceedingExecutiveReport(reportId)
          .futureValue
      }
    }
  }
}
