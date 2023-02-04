package ru.yandex.realty.componenttest.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, get, post, urlMatching}
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.ValueMatcher
import ru.yandex.realty.componenttest.http.ExternalHttpStub
import ru.yandex.realty.logging.Logging

trait WireMockExternalHttpStub extends WireMockProvider with ExternalHttpStub with Logging {

  override def toAbsoluteUrl(relativeUrl: String): String =
    wireMock.url(relativeUrl)

  override def stubGetResponse(urlRegex: String, status: Int, response: Array[Byte]): Unit = {
    wireMock.stubFor(
      get(urlMatching(urlRegex))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
        )
    )
    log.info(
      "GET resource is stubbed: url={}, status={}, responseBytes={}",
      urlRegex.asInstanceOf[AnyRef],
      status.asInstanceOf[AnyRef],
      response.length.asInstanceOf[AnyRef]
    )
  }

  override def stubPostResponse(
    urlRegex: String,
    status: Int,
    response: Array[Byte],
    requestMatcher: Option[ValueMatcher[Request]]
  ): Unit = {
    val request = post(urlMatching(urlRegex))
    requestMatcher.foreach(request.andMatching)
    wireMock.stubFor(
      request
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
        )
    )
    log.info(
      "GET resource is stubbed: url={}, status={}, responseBytes={}",
      urlRegex.asInstanceOf[AnyRef],
      status.asInstanceOf[AnyRef],
      response.length.asInstanceOf[AnyRef]
    )
  }

  override def stubDeleteResponse(urlRegex: String, status: Int): Unit = {
    wireMock.stubFor(
      delete(urlMatching(urlRegex))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
    log.info(
      "DELETE resource is stubbed: url={}, status={}",
      urlRegex.asInstanceOf[Any],
      status.asInstanceOf[Any]
    )
  }

}
