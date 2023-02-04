package ru.yandex.vertis.moderation.httpclient.geobase

import org.asynchttpclient.{AsyncHttpClientConfig, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.scalatest.Ignore
import ru.yandex.vertis.moderation.geobase.{GeobaseClient, GeobaseClientSpecBase}
import ru.yandex.vertis.moderation.httpclient.geobase.impl.http.HttpGeobaseClient

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class HttpGeobaseClientSpec extends GeobaseClientSpecBase {

  private val asyncHttpClientConfig: AsyncHttpClientConfig = new DefaultAsyncHttpClientConfig.Builder().build()
  private val httpClient = new DefaultAsyncHttpClient()
  override protected val geobaseClient: GeobaseClient =
    new HttpGeobaseClient("http://geobase-test.qloud.yandex.ru/v1/", httpClient)
}
