package ru.yandex.auto.vin.decoder.manager.validation

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.ValidationApi.FeedFormat
import ru.yandex.auto.vin.decoder.manager.validation.data.FeedDataSource
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.FileFormats.FileFormat
import ru.yandex.auto.vin.decoder.utils.EmptyRequestInfo

import java.io.InputStream
import scala.concurrent.ExecutionContext.Implicits.global

class FeedFormatRecognizerTest extends AnyFunSuite {

  implicit val r = EmptyRequestInfo
  case class RecognizeFeedTest(path: String, fileFormat: FileFormat, feedFormat: FeedFormat)

  val testData = Seq(
    RecognizeFeedTest(
      "/autoteka/sales/AlfaGarant_yandex_1_20200615-153212.csv",
      FileFormats.Csv,
      FeedFormat.AUTOTEKA_SALES
    ),
    RecognizeFeedTest(
      "/autoteka/sales/BRIGHTPARK_1_1_20200602-174502.json",
      FileFormats.Json,
      FeedFormat.AUTOTEKA_SALES
    ),
    RecognizeFeedTest(
      "/autoteka/services/AXSEL_000001_2_20200203-111959.csv",
      FileFormats.Csv,
      FeedFormat.AUTOTEKA_SERVICES
    ),
    RecognizeFeedTest(
      "/autoteka/products/AXSEL_000001_3_20200208-023002.csv",
      FileFormats.Csv,
      FeedFormat.AUTOTEKA_PRODUCTS
    ),
    RecognizeFeedTest(
      "/autoteka/insurance/LUIDOR_603152_5_20200603-142114.csv",
      FileFormats.Csv,
      FeedFormat.AUTOTEKA_INSURANCE
    ),
    RecognizeFeedTest(
      "/autoru/sales/ROLF_1_20191230-135025.json",
      FileFormats.Json,
      FeedFormat.AUTORU_SALES
    ),
    RecognizeFeedTest(
      "/autoru/sales/ROLF_1_20191230-135025.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_SALES
    ),
    RecognizeFeedTest(
      "/autoru/services/ROLF_2_20200525-102513.json",
      FileFormats.Json,
      FeedFormat.AUTORU_SERVICES
    ),
    RecognizeFeedTest(
      "/autoru/services/ROLF_2_20200525-102513.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_SERVICES
    ),
    RecognizeFeedTest(
      "/autoru/insurances/ROLF_5_20200420-181759.json",
      FileFormats.Json,
      FeedFormat.AUTORU_INSURANCE
    ),
    RecognizeFeedTest(
      "/autoru/insurances/ROLF_5_20200420-181759.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_INSURANCE
    ),
    RecognizeFeedTest(
      "/autoru/insurances/insurance_with_errors.json",
      FileFormats.Json,
      FeedFormat.AUTORU_INSURANCE
    ),
    RecognizeFeedTest(
      "/autoru/insurances/insurance_with_errors.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_INSURANCE
    ),
    RecognizeFeedTest(
      "/autoru/estimate/estimate.json",
      FileFormats.Json,
      FeedFormat.AUTORU_ESTIMATE
    ),
    RecognizeFeedTest(
      "/autoru/estimate/estimate_test.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_ESTIMATE
    ),
    RecognizeFeedTest(
      "/autoru/estimate/estimate.csv",
      FileFormats.Csv,
      FeedFormat.AUTORU_ESTIMATE
    ),
    RecognizeFeedTest(
      "/autoru/programs/programs.json",
      FileFormats.Json,
      FeedFormat.AUTORU_PROGRAMS
    ),
    RecognizeFeedTest(
      "/autoru/programs/programs.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_PROGRAMS
    ),
    RecognizeFeedTest(
      "/autoru/programs/programs.csv",
      FileFormats.Csv,
      FeedFormat.AUTORU_PROGRAMS
    ),
    RecognizeFeedTest(
      "/autoru/credit/credit-application.csv",
      FileFormats.Csv,
      FeedFormat.AUTORU_CREDIT_APPLICATION
    ),
    RecognizeFeedTest(
      "/autoru/credit/credit-application.xml",
      FileFormats.Xml,
      FeedFormat.AUTORU_CREDIT_APPLICATION
    ),
    RecognizeFeedTest(
      "/autoru/credit/credit-application.json",
      FileFormats.Json,
      FeedFormat.AUTORU_CREDIT_APPLICATION
    )
  )

  val recognizer = new FeedFormatRecognizer

  test("recognize formats") {
    testData.foreach(data => {
      val res = recognizer.recognize(getFeedData(data.path))
      assert(res.get.manager.feedFormat === data.feedFormat)
      assert(res.get.manager.fileFormat === data.fileFormat)
    })

  }

  private def getFeedData(path: String): FeedDataSource = {
    new FeedDataSource {
      override def getInputStream: InputStream = {
        getClass.getResourceAsStream(path)
      }
    }
  }

}
