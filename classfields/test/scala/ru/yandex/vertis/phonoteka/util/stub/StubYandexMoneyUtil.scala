package ru.yandex.vertis.phonoteka.util.stub

import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.quality.http_client_utils.config.{HttpClientConfig, ProxyConfig}
import sttp.client.Response
import sttp.model.{StatusCode, Uri}

object StubYandexMoneyUtil {

  val testConfig: HttpClientConfig =
    HttpClientConfig(
      url = "https://uri.uri.uri.yandex.ru",
      proxyConfig =
        Some(
          ProxyConfig(
            "infra-proxy.slb.vertis.yandex.net",
            3128
          )
        )
    )

  val phone1 = Phone("79171111111")
  val phone2 = Phone("79172222222")
  val phone3 = Phone("79173333333")
  val phone4 = Phone("79174444444")
  val phone5 = Phone("79175555555")

  def getResponseByUri(uri: Uri): Response[String] =
    uri.toString match {
      case s if s contains phone1.normalized => Response.ok(stubbedResponse1)
      case s if s contains phone2.normalized => Response.ok(stubbedResponse2)
      case s if s contains phone3.normalized => Response.ok(stubbedResponse3)
      case s if s contains phone4.normalized => Response.apply(stubbedResponse4, StatusCode(400))
      case s if s contains phone5.normalized => Response.apply(stubbedResponse4, StatusCode(500))
    }

  private val stubbedResponse1: String =
    s"""{"hasWallet":true,"lastWalletCreated":"2020-02-06","lastTransaction":"2020-02-06","phoneNumberInBlackList":false,"hasTransactions":true}"""

  private val stubbedResponse2: String =
    s"""{"hasWallet":true,"lastWalletCreated":"2020-02-03","lastTransaction":"2020-02-03","phoneNumberInBlackList":false,"hasTransactions":true}"""

  private val stubbedResponse3: String = s"""{"hasWallet":false,"phoneNumberInBlackList":false}"""

  private val stubbedResponse4: String = s"""{"error":{"type":"IllegalParameters","parameterNames":["phone"]}}"""
}
