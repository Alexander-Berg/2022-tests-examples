package ru.yandex.vertis.billing.banker.sms

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCode, StatusCodes}
import akka.util.ByteString
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.sms.SmsClient.SmsClientResponseStatuses
import ru.yandex.vertis.billing.banker.util.AkkaHttpUtil.HttpResponder
import ru.yandex.vertis.mockito.MockitoSupport
import org.mockito.Mockito.reset

import scala.concurrent.Future
import scala.xml.{Elem, NodeSeq}

class HttpSmsClientSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase
  with BeforeAndAfterEach {

  val responder = mock[HttpResponder]

  override def beforeEach(): Unit = {
    reset(responder)
    super.beforeEach()
  }

  def mockResponder(httpResponse: HttpResponse): Unit = {
    stub(responder.apply(_: HttpRequest)) { case _ =>
      Future.successful(httpResponse)
    }
    ()
  }

  implicit val system = ActorSystem()

  val httpClient = new HttpSmsClient("test", "test", "test", "test", responder)

  val messageSentId = 123456.toString

  val SuccessBody: Elem = <doc><message-sent id={messageSentId} /></doc>

  val SuccessResponse = response(StatusCodes.OK, SuccessBody)

  val StrangeSmsSenderBody = <doc>
    <message-sent/>
    <error>test</error>
    <errorcode>{SmsClientResponseStatuses.ExternalNumberDeliveryDisabled}</errorcode>
  </doc>

  val StrangeSmsSenderResponse = response(StatusCodes.InternalServerError, StrangeSmsSenderBody)

  def errorBody(error: SmsClientResponseStatuses.Value, desc: String): NodeSeq =
    <doc>
      <error>{desc}</error>
      <errorcode>{error}</errorcode>
    </doc>

  def response(status: StatusCode, message: NodeSeq): HttpResponse = {
    val ct = ContentTypes.`text/xml(UTF-8)`
    HttpResponse(status, entity = HttpEntity.Strict(ct, ByteString(message.toString())))
  }

  private val FailSmsClientStatuses =
    Seq(
      SmsClientResponseStatuses.BadPhone,
      SmsClientResponseStatuses.PermanentBlock,
      SmsClientResponseStatuses.PhoneBlocked
    )

  private val RepeatableClientStatuses =
    Seq(
      SmsClientResponseStatuses.DontKnowYou,
      SmsClientResponseStatuses.NoRights,
      SmsClientResponseStatuses.NoPhone,
      SmsClientResponseStatuses.NoCurrent,
      SmsClientResponseStatuses.NoText,
      SmsClientResponseStatuses.NoUid,
      SmsClientResponseStatuses.NoRoute,
      SmsClientResponseStatuses.IntError,
      SmsClientResponseStatuses.LimitExceed
    )

  "HttpSmsClientSpec" should {

    "work with success response" in {
      mockResponder(SuccessResponse)
      val result = httpClient.sendText("123", "test", "1").futureValue
      result.status shouldBe SmsClientResponseStatuses.OK
      result.description shouldBe None
      result.messageSentId shouldBe Some(messageSentId)
    }

    "work with fail responses" in {
      FailSmsClientStatuses.foreach { error =>
        val desc = "test"
        mockResponder(response(StatusCodes.OK, errorBody(error, desc)))
        val result = httpClient.sendText("123", "test", "1").futureValue
        result.status shouldBe error
        result.description shouldBe Some(desc)
        result.messageSentId shouldBe None
      }
    }

    "work with repeatable responses" in {
      RepeatableClientStatuses.foreach { error =>
        val desc = "test"
        mockResponder(response(StatusCodes.OK, errorBody(error, desc)))
        val result = httpClient.sendText("123", "test", "1").futureValue
        result.status shouldBe error
        result.description shouldBe Some(desc)
        result.messageSentId shouldBe None
      }
    }

    "work with strange response from sms sender proxy" in {
      mockResponder(StrangeSmsSenderResponse)
      val result = httpClient.sendText("123", "test", "1").futureValue
      result.status shouldBe SmsClientResponseStatuses.ExternalNumberDeliveryDisabled
      result.description shouldBe Some("test")
      result.messageSentId shouldBe None
    }

    "work with bad request" in {
      mockResponder(HttpResponse(StatusCodes.BadRequest))
      intercept[IllegalArgumentException] {
        httpClient.sendText("123", "test", "1").await
      }
    }

    "work with server error" in {
      mockResponder(HttpResponse(StatusCodes.InternalServerError))
      intercept[SmsServerError] {
        httpClient.sendText("123", "test", "1").await
      }
    }

    "work with other statuses" in {
      mockResponder(HttpResponse(StatusCodes.Forbidden))
      intercept[RuntimeException] {
        httpClient.sendText("123", "test", "1").await
      }
    }

  }

}
