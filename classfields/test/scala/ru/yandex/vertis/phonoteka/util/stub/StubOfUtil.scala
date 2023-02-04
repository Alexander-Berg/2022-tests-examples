package ru.yandex.vertis.phonoteka.util.stub

import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.quality.http_client_utils.config.{HttpClientConfig, ProxyConfig, SslContexConfig}
import sttp.client.{RequestBody, Response, StringBody}

object StubOfUtil {

  val testConfig =
    HttpClientConfig(
      url = "https://example.com:12332",
      proxyConfig =
        Some(
          ProxyConfig(
            "infra-proxy.slb.vertis.yandex.net",
            3128
          )
        ),
      sslContextConfig =
        Some(
          SslContexConfig(
            "/etc/yandex/vertis-datasources-secrets/of-cert.p12",
            "super_secret_password"
          )
        )
    )

  val phone1 = Phone("79171111111")
  val phone2 = Phone("79172222222")
  val phone3 = Phone("79173333333")
  val phone4 = Phone("79174444444")
  val phone5 = Phone("79175555555")
  val phone6 = Phone("79176666666")

  def getResponse(requestBody: RequestBody[Nothing]): Response[String] =
    requestBody match {
      case StringBody(s, _, _) if s contains phone1.normalized => Response.ok(stubbedResponse1)
      case StringBody(s, _, _) if s contains phone2.normalized => Response.ok(stubbedResponse2)
      case StringBody(s, _, _) if s contains phone3.normalized => Response.ok(stubbedResponse3)
      case StringBody(s, _, _) if s contains phone4.normalized => Response.ok(stubbedResponse4)
      case StringBody(s, _, _) if s contains phone5.normalized => Response.ok(stubbedResponse5)
      case StringBody(s, _, _) if s contains phone6.normalized => Response.ok(stubbedResponse6)
    }

  private val stubbedResponse1: String =
    s"""{"id":"some-random-id-1","data":{"msisdn":"${phone1.normalized}","variables":[{"name":"SCORE_V12","status":"SUCCESS","value":"2.0","type":"DOUBLE"},{"name":"SCORE_V11","status":"SUCCESS","value":"5.0","type":"DOUBLE"}]}}"""

  private val stubbedResponse2: String =
    s"""{"id":"some-random-id-2","data":{"msisdn":"${phone2.normalized}","variables":[{"name":"SCORE_V12","status":"SUCCESS","value":"4.0","type":"DOUBLE"},{"name":"SCORE_V11","status":"SUCCESS","value":"1.0","type":"DOUBLE"}]}}"""

  private val stubbedResponse3: String =
    s"""{"id":"some-random-id-3","data":{"msisdn":"${phone3.normalized}","variables":[{"name":"SCORE_V12","status":"SUCCESS","value":"3.2","type":"DOUBLE"},{"name":"SCORE_V11","status":"SUCCESS","value":"2.0","type":"DOUBLE"}]}}"""

  private val stubbedResponse4: String =
    s"""{"id":"some-random-id-3","data":{"msisdn":"${phone4.normalized}","variables":[{"name":"SCORE_V12","status":"NOT_FOUND"},{"name":"SCORE_V11","status":"SUCCESS","value":"1.0","type":"DOUBLE"}]}}"""

  private val stubbedResponse5: String =
    s"""{"id":"some-random-id-3","data":{"msisdn":"${phone5.normalized}","variables":[{"name":"SCORE_V12","status":"NOT_FOUND"},{"name":"SCORE_V11","status":"AWKWARD_STATUS","value":"over 9000"}]}}"""
  private val stubbedResponse6: String = """{"not":{"good":"json"}}"""
}
