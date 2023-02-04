package ru.yandex.vertis.passport.api.v2

import akka.http.scaladsl.model.MediaType.{Binary, WithFixedCharset}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentType, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalacheck.Gen
import org.scalatest.Matchers._
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.passport.api.ServiceBackendProvider
import ru.yandex.vertis.passport.proto.ApiProtoFormats
import ru.yandex.vertis.passport.model.SessionResult
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.util.akka.http.protobuf.{Protobuf, ProtobufSupport}

/**
  *
  * @author zvez
  */
trait V2Spec extends ProtobufSupport {
  this: ServiceBackendProvider with ScalatestRouteTest =>

  val medialType = Gen.oneOf(MediaTypes.`application/json`, Protobuf.mediaType).next

  val expectedContentType: ContentType = medialType match {
    case b: Binary => b.toContentType
    case nb: WithFixedCharset => nb.toContentType
    case _ => ???
  }

  val commonHeaders = addHeader(Accept(medialType))

  def yandexHeaders(yaSessionId: String, yaSslSessionId: String) =
    addHeader("X-Yandex-Session-ID", yaSessionId) ~>
      addHeader("X-Yandex-Ssl-Session-ID", yaSslSessionId)

  def checkSessionResult(expected: SessionResult, response: ApiModel.SessionResult): Unit = {
    response shouldBe apiCtx.SessionResultWriter.write(expected)
    response.getSession.getId shouldBe expected.session.id.asString
    response.getSession.getDeviceUid shouldBe expected.session.deviceUid
    response.getSession.getTtlSec shouldBe expected.session.ttl.getStandardSeconds
    response.getSession.getUserId shouldBe expected.session.userId.getOrElse("")

    expected.user.foreach { expectedUser =>
      response.hasUser shouldBe true
      val user = response.getUser
      user.getId shouldBe expectedUser.id
    }
  }
}
