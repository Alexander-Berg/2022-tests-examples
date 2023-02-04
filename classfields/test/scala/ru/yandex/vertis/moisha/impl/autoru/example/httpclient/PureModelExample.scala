package ru.yandex.vertis.moisha.impl.autoru.example.httpclient

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import ru.yandex.vertis.moisha.environment
import ru.yandex.vertis.moisha.impl.autoru.example.PureModelImplementation._
import ru.yandex.vertis.moisha.impl.autoru.example.PureModelJsonProtocol
import spray.json._
import ru.yandex.vertis.moisha.impl.autoru.model.gens._
import ru.yandex.vertis.moisha.model.gens._

/**
  * Uses Moisha-independent model implementation.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
object PureModelExample extends App with PureModelJsonProtocol with SprayJsonSupport {

  /** unmarshals only points from response */
  def unmarshalResponse(httpResponse: HttpResponse): Seq[PointImpl] = {
    val responseBody = EntityUtils.toString(httpResponse.getEntity)
    val jsValue = JsonParser(ParserInput(responseBody))
    val fields = jsValue.asJsObject.fields
    /*
    fields.get("request") ... if someone wants to parse original request
     */
    fields
      .get("points")
      .map {
        case JsArray(ps) => ps.map(pointFormat.read)
        case other =>
          throw new IllegalArgumentException(s"Unexpected $other")
      }
      .getOrElse(throw new IllegalArgumentException(s"No points found in response"))
  }

  // create reference model from AutoRu model implementation
  val offer: OfferImpl = OfferGen.next
  val context: ContextImpl = ContextGen.next

  val from = environment.now().withTimeAtStartOfDay()
  val to = from.plusDays(2).minus(1)

  val request = requestFormat
    .write(RequestImpl(offer, context, "placement", from, to))
    .compactPrint

  /** unmarshal with model format */
  val response = executeWithApache(
    request,
    httpResponse => responseReader.read(JsonParser(ParserInput(EntityUtils.toString(httpResponse.getEntity))))
  )

  println(s"request:\n$request\n\nresponse:\n$response")

  /** simple points unmarshalling */
  val points = executeWithApache(request, unmarshalResponse)

  println(s"request:\n$request\n\npoints:\n${points.mkString("\n")}")

}
