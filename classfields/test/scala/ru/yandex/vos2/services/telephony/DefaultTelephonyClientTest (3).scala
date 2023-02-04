package ru.yandex.vos2.services.telephony

import org.apache.http.HttpResponse
import org.apache.http.client.methods.{HttpPost, HttpRequestBase}
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.services.telephony.DefaultTelephonyClient.CreateRedirectRequest
import ru.yandex.vos2.services.telephony.TelephonyClient.Domains
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration._

/**
  * Created by andrey on 2/6/18.
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DefaultTelephonyClientTest extends AnyFunSuite with MockHttpClientHelper {

  implicit val trace = Traced.empty
  for {
    doNotDisturb <- Seq(true, false)
  } test(s"getOrCreatePhoneRedirect(doNotDisturb = $doNotDisturb)") {
    val telephonyClient = getClient(doNotDisturb)
    telephonyClient.getOrCreatePhoneRedirect(
      Domains.TestDomain,
      "testObjectId",
      "79991113344",
      5.days,
      doNotDisturb,
      Some("test_tag"),
      Some(213)
    )
  }

  private def getClient(trustedDealerCallsAccepted: Boolean): DefaultTelephonyClient = {
    new DefaultTelephonyClient("telepony-api-01-iva.vertis.tst.yandex.net", 35506) {
      override protected def doRequest[T <: HttpRequestBase, R](name: String, request: T)(
          f: (HttpResponse) => R
      )(implicit traced: Traced): R = {
        val objectId: String = "testObjectId"
        val antifraud = if (trustedDealerCallsAccepted) "enable" else "restricted"
        assert(request.getURI.toString == "/api/2.x/test_domain/redirect/getOrCreate/" + objectId + "/")
        val entity = EntityUtils.toString(request.asInstanceOf[HttpPost].getEntity)
        val jsonEntity = Json.parse(entity).as[CreateRedirectRequest]

        val needEntity = CreateRedirectRequest(
          target = "+79991113344",
          ttl = 432000,
          geoId = Some(213),
          antifraud = s"$antifraud",
          tag = Some("test_tag"),
          phoneType = None
        )
        assert(jsonEntity === needEntity)
        val response = mockResponse(
          200,
          Json
            .obj(
              "id" -> "testId",
              "objectId" -> objectId,
              "createTime" -> DateTime.now().toString,
              "deadline" -> DateTime.now().plusDays(1).toString,
              "source" -> "testSource",
              "target" -> "testTarget"
            )
            .toString()
        )
        f(response)
      }
    }
  }
}
