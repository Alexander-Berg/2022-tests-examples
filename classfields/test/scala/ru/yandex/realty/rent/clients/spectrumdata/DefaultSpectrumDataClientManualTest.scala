package ru.yandex.realty.rent.clients.spectrumdata

import org.apache.http.impl.nio.client.{CloseableHttpAsyncClient, HttpAsyncClientBuilder}
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.http.{ApacheHttpClient, HttpEndpoint, LoggedHttpClient, RemoteHttpService}
import ru.yandex.realty.rent.clients.spectrumdata.passport.PassportVerificationRequestData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

/**
  * @author azakharov
  */
class DefaultSpectrumDataClientManualTest extends SpecBase with AsyncSpecBase with PropertyChecks {

  val realAsyncClient: CloseableHttpAsyncClient = {
    val client = HttpAsyncClientBuilder
      .create()
      .setMaxConnPerRoute(1024)
      .setMaxConnTotal(40 * 1024)
      .build()
    client.start()
    sys.addShutdownHook(client.close())
    client
  }

  val remoteHttpService = new RemoteHttpService(
    "spectrumdata",
    HttpEndpoint("b2b-api.spectrumdata.ru", 443, "https"),
    new ApacheHttpClient(realAsyncClient) with LoggedHttpClient
  )

  //https://yav.yandex-team.ru/secret/sec-01f87tnyhqfqzbg7t1ejr95vey/explore/versions
  val tokenMaker = new SpectrumDataTokenMaker("", "")
  val spectrumDataClient = new DefaultSpectrumDataClient(remoteHttpService, tokenMaker)
  implicit val trace: Traced = Traced.empty

  "DefaultSpectrumDataClient" should {
    "should create passport report and get passport report successfully" in {
      val response = spectrumDataClient
        .createPassportActualReport("3206", "193634")
        .futureValue
      println(s"response=$response")
      assert(response.data.nonEmpty)
      val reportResponseItem = response.data.head
      val passportReport = spectrumDataClient.getPassportActualReport(reportResponseItem.uid).futureValue
      println(s"report=$passportReport")
    }

    "should create wanted report and get wanted report successfully" in {
      implicit val trace: Traced = Traced.empty
      val response = spectrumDataClient
        .createWantedReport("Иван", "Иванов", Some("Иванович"), Some("01.01.2000"))
        .futureValue
      println(s"response=$response")
      assert(response.data.nonEmpty)
      val reportResponseItem = response.data.head
      val report = spectrumDataClient.getWantedReport(reportResponseItem.uid).futureValue
      println(s"report=$report")
    }

    "should create extremists report and get extremists report successfully" in {
      implicit val trace: Traced = Traced.empty
      val response = spectrumDataClient
        .createExtremistReport("Иван", "Иванов", Some("Иванович"), Some("01.01.2000"))
        .futureValue
      println(s"response=$response")
      assert(response.data.nonEmpty)
      val reportResponseItem = response.data.head
      val report = spectrumDataClient.getExtremistReport(reportResponseItem.uid).futureValue
      println(s"report=$report")
    }

    "should get passport actual report successfully" in {
      val response = spectrumDataClient
        .getPassportActualReport(
          "report_passport_check_eyJwYXNzcG9ydCI6IjMyMDYgMTkzNjM0In0=@yandex_team_check_person"
        )
        .futureValue
      println(response)
    }

    "should create passport verification report" in {
      val response = spectrumDataClient
        .createPassportVerificationReport(
          "Иванов",
          "Иван",
          Some("Иванович"),
          Some("19.12.1983"),
          Some("1234567890"),
          None
        )
        .futureValue
      println(response)
    }

    "should get passport verification report" in {
      val response = spectrumDataClient
        .getPassportVerificationReport(
          "report_verify_person_eyJsYXN0X25hbWUiOiLQmNCy0LDQvdC-0LIiLCJmaXJzdF9uYW1lIjoi0JjQstCw0L0iLC" +
            "JwYXRyb255bWljIjoi0JjQstCw0L3QvtCy0LjRhyIsImJpcnRoIjoiMTkuMTIuMTk4MyIsInBhc3Nwb3J0IjoiMTIzNDU2Nzg5MCJ9" +
            "@yandex_team_check_person"
        )
        .futureValue
      println(response)
    }

    "should create and get proceeding executive report successfully" in {
      implicit val trace: Traced = Traced.empty
      val response = spectrumDataClient
        .createProceedingExecutiveReport("Иван", "Иванов", Some("Иванович"), Some("01.01.2000"))
        .futureValue
      println(s"response=$response")
      assert(response.data.nonEmpty)
      val reportResponseItem = response.data.head
      val report = spectrumDataClient.getProceedingExecutiveReport(reportResponseItem.uid).futureValue
      println(s"report=$report")
    }

  }
}
