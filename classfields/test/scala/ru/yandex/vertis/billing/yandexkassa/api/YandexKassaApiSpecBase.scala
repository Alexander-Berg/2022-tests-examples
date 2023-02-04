package ru.yandex.vertis.billing.yandexkassa.api

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.nio.file.{Files, Paths}
import ru.yandex.vertis.billing.yandexkassa.api.impl.ApiV2Settings
import spray.json.{JsValue, JsonParser}

/**
  * Base stuff for api specs
  *
  * @author alex-kovalenko
  */
trait YandexKassaApiSpecBase extends Matchers with AnyWordSpecLike {

  private val privateFile =
    this.getClass.getResourceAsStream("/openssl/apiv2/private.pem")

  private val publicFile =
    this.getClass.getResourceAsStream("/openssl/apiv2/public.pem")

  val settings =
    ApiV2Settings.forKeys(
      shopId = 12345,
      shopArticleId = 6789,
      privateFile,
      publicFile,
      isTestEnvironment = true
    )

  def getContent(name: String): String =
    new String(
      this.getClass.getResourceAsStream(name).readAllBytes()
    )

  def getJson(name: String): JsValue =
    JsonParser(getContent(name))

}
