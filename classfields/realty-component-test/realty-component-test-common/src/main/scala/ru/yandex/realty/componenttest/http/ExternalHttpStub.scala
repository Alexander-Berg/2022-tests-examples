package ru.yandex.realty.componenttest.http

import java.io.InputStream

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.ValueMatcher
import org.apache.commons.io.IOUtils
import play.api.libs.json.{Json, Writes}

trait ExternalHttpStub {

  def toAbsoluteUrl(relativeUrl: String): String

  def stubGetJsonResponse[T](
    urlRegex: String,
    status: Int,
    response: T
  )(implicit tjs: Writes[T]): Unit = {
    stubGetResponse(urlRegex, status, Json.stringify(Json.toJson(response)))
  }

  def stubGetResponse(
    urlRegex: String,
    status: Int,
    response: String
  ): Unit = {
    stubGetResponse(urlRegex, status, response.getBytes)
  }

  def stubGetResponse(
    urlRegex: String,
    status: Int,
    response: InputStream
  ): Unit = {
    stubGetResponse(urlRegex, status, IOUtils.toByteArray(response))
  }

  def stubGetResponse(
    urlRegex: String,
    status: Int,
    response: Array[Byte]
  ): Unit

  def stubPostJsonResponse[T](
    urlRegex: String,
    status: Int,
    response: T,
    requestMatcher: Option[ValueMatcher[Request]] = None
  )(implicit tjs: Writes[T]): Unit = {
    stubPostResponse(urlRegex, status, Json.stringify(Json.toJson(response)), requestMatcher)
  }

  def stubPostResponse(
    urlRegex: String,
    status: Int,
    response: String,
    requestMatcher: Option[ValueMatcher[Request]]
  ): Unit = {
    stubPostResponse(urlRegex, status, response.getBytes, requestMatcher)
  }

  def stubPostResponse(
    urlRegex: String,
    status: Int,
    response: Array[Byte],
    requestMatcher: Option[ValueMatcher[Request]]
  ): Unit

  def stubDeleteResponse(
    urlRegex: String,
    status: Int = StatusCodes.OK.intValue
  ): Unit

}
