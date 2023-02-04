package ru.yandex.vertis.chat.api

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpRequest, MediaType}
import org.joda.time.DateTime
import ru.auto.api.CommonModel.ClientFeature
import ru.yandex.vertis.chat.PassportId
import ru.yandex.vertis.chat.api.util.directives.RequestDirectives
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.generators.ProducerProvider

class DomainHttpRequest(val request: HttpRequest) extends ProducerProvider {

  def withUser(user: UserId): HttpRequest = {
    request.addHeader(RawHeader(RequestDirectives.UserIdHeader, user))
  }

  def withSomeUser: HttpRequest = withUser(userId.next)

  def withPassportUser(uid: PassportId): HttpRequest = {
    request.addHeader(RawHeader(RequestDirectives.PassportIdHeader, uid.toString))
  }

  def withSomePassportUser: HttpRequest = withPassportUser(uid.next)

  def withRequestId(requestId: String): HttpRequest = {
    request.addHeader(RawHeader(RequestDirectives.RequestIdHeader, requestId))
  }

  def withFeatures(clientFeatures: Set[ClientFeature]): HttpRequest = {
    request.addHeader(
      RawHeader(RequestDirectives.FeaturesHeader, clientFeatures.map(_.name().toLowerCase()).mkString(","))
    )
  }

  def withCacheControlHeader(directives: CacheDirective.RequestDirective*): HttpRequest = {
    if (directives.isEmpty) request
    else if (directives.length == 1) request.addHeader(`Cache-Control`(directives.head))
    else request.addHeader(`Cache-Control`(directives.head, directives.tail: _*))
  }

  def withTill(dateTime: DateTime): HttpRequest = withQueryParam(RequestDirectives.TillParam, dateTime)

  implicit private val dateToStr: DateTime => String = _.toDateTimeISO.toString

  def withTimestampParam(param: String, dateTime: DateTime): HttpRequest = {

    withQueryParam(param, dateTime)
  }

  def withQueryParam[T](param: String, value: T)(implicit conv: T => String): HttpRequest = {
    request.withUri(
      request.uri.withQuery(Query(request.uri.query().toMap + (param -> conv(value))))
    )
  }

  def accepting(mediaType: MediaType): HttpRequest = {
    request.addHeader(Accept(mediaType))
  }
}
