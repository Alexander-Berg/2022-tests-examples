package ru.yandex.vertis.picapica.http

import org.scalatest.{WordSpecLike, Matchers}
import spray.http.HttpResponse
import spray.httpx.unmarshalling._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

/**
  * @author evans
  */
trait RouteTest
  extends Matchers
  with WordSpecLike
  with ScalatestRouteTest
  with HttpService {
  implicit def unmarshallerAdapter[T](implicit um: Unmarshaller[T]): FromResponseUnmarshaller[T] = {
    new Deserializer[HttpResponse, T] {
      override def apply(response: HttpResponse): Deserialized[T] = um(response.entity)
    }
  }
}
