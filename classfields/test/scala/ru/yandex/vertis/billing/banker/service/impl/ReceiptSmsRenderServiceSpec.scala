package ru.yandex.vertis.billing.banker.service.impl

import com.google.common.base.Charsets
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.url.UrlShortener
import ru.yandex.vertis.mockito.MockitoSupport
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ReceiptSmsRenderServiceSpec.{receiptSmsText, ReceiptDateTimeFormat}
import org.joda.time.DateTimeZone
import ru.yandex.vertis.billing.banker.model.Receipt
import ru.yandex.vertis.billing.banker.model.Receipt.ReceiptFields
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import spray.json.{enrichString, DeserializationException, JsObject, JsString, JsValue}
import ru.yandex.vertis.billing.banker.model.gens.{receiptGen, Producer}

import scala.concurrent.Future
import scala.io.Source
import scala.reflect.ClassTag

class ReceiptSmsRenderServiceSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfterEach {

  private val ExpectedReceiptLongUrl = "i am long url. trust me"
  private val ExpectedReceiptShortUrl = "i am short url. trust me"

  private val TestReceiptStr = Source
    .fromInputStream(
      this.getClass.getResourceAsStream("/receipt/test.json")
    )
    .mkString

  private val TestReceiptJson = TestReceiptStr.parseJson.asJsObject

  private val TestReceiptBytes = TestReceiptStr.getBytes(Charsets.UTF_8)

  private def wrapToReceipt(content: Array[Byte]): Receipt =
    receiptGen().next.copy(content = content)

  private def extractFields(content: Array[Byte]): ReceiptFields =
    wrapToReceipt(content).receiptFields

  private val TestReceiptUrl =
    extractFields(TestReceiptBytes).checkUrl

  private val urlShortener = {
    val m = mock[UrlShortener]
    stub(m.shorten _) {
      case ExpectedReceiptLongUrl | TestReceiptUrl =>
        Future.successful(ExpectedReceiptShortUrl)
      case url =>
        Future.failed(new IllegalArgumentException(s"Unexpected url: $url"))
    }
    m
  }

  object FieldToRemove extends Enumeration {
    val dt = Value("dt")
    val kkt = Value("kkt")
    val fp = Value("fp")
    val content = Value("receipt_calculated_content")
    val url = Value("check_url")
    val zone = Value("localzone")
  }

  def removeField(json: JsObject, field: FieldToRemove.Value): JsObject =
    JsObject(json.fields.view.filterKeys(_ != field.toString).toMap)

  def replaceField(json: JsObject, field: FieldToRemove.Value, value: JsValue): JsObject =
    JsObject(removeField(json, field).fields + (field.toString -> value))

  private val receiptSmsRenderService = new ReceiptSmsRenderServiceImpl(urlShortener)

  private def checkSmsText(text: String, shortUrl: String, receipt: Array[Byte]): Unit = {
    val fields = extractFields(receipt)
    val expectedText = receiptSmsText(shortUrl, fields)
    text shouldBe expectedText: Unit
  }

  private def check(content: Array[Byte]): Unit = {
    val receipt = wrapToReceipt(content)
    val shortUrl = receiptSmsRenderService.shortUrl(receipt).futureValue
    shortUrl shouldBe ExpectedReceiptShortUrl
    val smsText = receiptSmsRenderService.render(receipt, ExpectedReceiptShortUrl).futureValue
    checkSmsText(smsText, ExpectedReceiptShortUrl, content)
  }

  private def checkFail[T <: AnyRef: ClassTag](content: Array[Byte]): Unit = {
    val receipt = wrapToReceipt(content)
    intercept[T] {
      receiptSmsRenderService.shortUrl(receipt).await
    }
    intercept[T] {
      receiptSmsRenderService.render(receipt, "").await
    }
    ()
  }

  "ReceiptSmsRenderService" should {

    "work correctly for check from testing" in {
      check(TestReceiptBytes)
    }

    "work correctly when one of expected fields not presented" in {
      FieldToRemove.values.foreach { field =>
        val json = removeField(TestReceiptJson, field).toString
        val bytes = json.getBytes(Charsets.UTF_8)
        checkFail[DeserializationException](bytes)
      }
    }

    "fail when the date is in the wrong format" in {
      val json = replaceField(
        TestReceiptJson,
        FieldToRemove.dt,
        JsString("26.04.2019 22:36:00")
      ).toString
      val bytes = json.getBytes(Charsets.UTF_8)
      checkFail[IllegalArgumentException](bytes)
    }

    "work correctly with UTC receipt timezone" in {
      val receiptTime = ReceiptDateTimeFormat
        .withZone(DateTimeUtils.TimeZone)
        .parseDateTime(
          TestReceiptJson.fields(FieldToRemove.dt.toString) match {
            case JsString(v) =>
              v
            case f =>
              throw new IllegalArgumentException(s"Unexpected $FieldToRemove.dt field: $f")
          }
        )

      val convertedReceiptTime =
        receiptTime.toDateTime(DateTimeZone.UTC)

      val withChangedTime = replaceField(
        TestReceiptJson,
        FieldToRemove.dt,
        JsString(ReceiptDateTimeFormat.print(convertedReceiptTime))
      )

      val withChangedTimeAndZone = replaceField(
        withChangedTime,
        FieldToRemove.zone,
        JsString("UTC")
      ).toString

      val bytes = withChangedTimeAndZone.getBytes(Charsets.UTF_8)
      check(bytes)
    }

  }

}

object ReceiptSmsRenderServiceSpec {

  private val ReceiptDateTimeFormat =
    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  private val SmsTextDateTimeFormat =
    new DateTimeFormatterBuilder()
      .appendLiteral('D')
      .appendLiteral(' ')
      .appendDayOfMonth(2)
      .appendLiteral('.')
      .appendMonthOfYear(2)
      .appendLiteral('.')
      .appendYear(4, 4)
      .appendLiteral(' ')
      .appendLiteral('T')
      .appendLiteral(' ')
      .appendHourOfDay(2)
      .appendLiteral(':')
      .appendMinuteOfHour(2)
      .appendLiteral(':')
      .appendSecondOfMinute(2)
      .toFormatter
      .withZone(DateTimeUtils.TimeZone)

  private def receiptSmsText(shortUrl: String, fields: ReceiptFields): String = {
    val I = fields.receiptCalculatedContent.total
    val K = fields.kkt.rn
    val F = fields.fp
    val DT = SmsTextDateTimeFormat.print(fields.dateTime)
    s"CHECK I $I SAIT $shortUrl $DT K $K F $F"
  }

}
