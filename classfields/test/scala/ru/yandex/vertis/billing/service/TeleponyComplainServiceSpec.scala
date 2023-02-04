package ru.yandex.vertis.billing.service

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.service.TeleponyComplainServiceImpl.{
  DuplicateTeleponyCallComplainException,
  TeleponyRedirectNotFoundException
}
import ru.yandex.vertis.billing.settings.TeleponySettings
import ru.yandex.vertis.billing.util.DateTimeUtils
import ru.yandex.vertis.telepony.model.proto.ComplaintsModel.{
  Complaint,
  ComplaintCallInfo,
  ComplaintCause,
  ComplaintCauseType
}
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.monad._

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * @author tolmach
  */
class TeleponyComplainServiceSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  implicit val monad = new FutureMonad()

  private val domain = "autoru_billing"
  private val baseUrl = "http://telepony-api-int.vrts-slb.test.vertis.yandex.net"

  private val configMap = Map(
    "url" -> baseUrl,
    "domain" -> domain,
    "connection-timeout" -> "5s",
    "request-timeout" -> "60s",
    "num-retries" -> "3",
    "max-connections" -> "5"
  )
  private val config = ConfigFactory.parseMap(configMap.asJava)
  private val settings = TeleponySettings(config)

  private val redirectId = "123456789"
  private val incoming = "+88005553535"
  private val callTime = DateTimeUtils.now()
  private val causeDescription = "test"

  private val expectedComplain = {
    val cause = ComplaintCause
      .newBuilder()
      .setCauseType(ComplaintCauseType.TOO_MANY_NON_TARGET_CALLS)
      .setCauseDescription("test")

    val timestamp = Conversions.protoTimestamp(callTime.getMillis)
    val callInfo = ComplaintCallInfo
      .newBuilder()
      .setCallerNumber(incoming)
      .setCallTime(timestamp)

    Complaint
      .newBuilder()
      .setId(1)
      .setRedirectId(redirectId)
      .setComplaintCause(cause)
      .setCallInfo(callInfo)
      .build()
  }

  private def prepareBackend = {
    SttpBackendStub.asynchronousFuture
      .whenRequestMatches(_.uri.toString.startsWith(baseUrl))
  }

  private def okBackend(response: Array[Byte]): SttpBackend[Future, Any] = {
    prepareBackend.thenRespond(response)
  }

  private def backendWithCode(code: Int): SttpBackend[Future, Any] = {
    prepareBackend.thenRespondWithCode(StatusCode(code))
  }

  private def client(backend: => SttpBackend[Future, Any]): TeleponyComplainService[Future] = {
    new TeleponyComplainServiceImpl(settings)(backend, monad)
  }

  "TeleponyComplainService" should {
    "crate complain" in {
      val response = expectedComplain.toByteArray
      val backend = okBackend(response)
      val c = client(backend)

      val complain = c.complain(redirectId, incoming, callTime, causeDescription).futureValue
      complain shouldBe expectedComplain
    }
    "throw DuplicateTeleponyCallComplainException when got 400 from telepony" in {
      val backend = backendWithCode(400)
      val c = client(backend)

      val response = c.complain(redirectId, incoming, callTime, causeDescription)
      response.failed.futureValue match {
        case DuplicateTeleponyCallComplainException(`redirectId`) =>
          ()
        case other =>
          fail(s"Unexpected $other")
      }
    }
    "throw TeleponyRedirectNotFoundException when got 400 from telepony" in {
      val backend = backendWithCode(404)
      val c = client(backend)

      val response = c.complain(redirectId, incoming, callTime, causeDescription)
      response.failed.futureValue match {
        case TeleponyRedirectNotFoundException(`redirectId`) =>
          ()
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

}
