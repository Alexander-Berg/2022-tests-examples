package ru.yandex.vertis.moisha.impl.autoru.example

import org.joda.time.DateTime
import ru.yandex.vertis.moisha.environment._
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy
import ru.yandex.vertis.moisha.impl.autoru.example.PureModelImplementation._
import spray.json.{JsObject, JsString, RootJsonFormat, RootJsonReader, _}

/**
  * Json formats descriptions for model objects
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait PureModelJsonProtocol extends DefaultJsonProtocol {

  /** [[DateTime]] <-> Json converter */
  implicit object DateTimeJsonFormat extends JsonFormat[DateTime] {

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) =>
        IsoDateTimeFormatter.parseDateTime(s)
      case x =>
        deserializationError(s"Expected JsString, but got $x")
    }

    override def write(dt: DateTime): JsValue =
      JsString(IsoDateTimeFormatter.print(dt))
  }

  /** [[scala.Product]]-based formats for model case classes */
  implicit val goodFormat: RootJsonFormat[GoodImpl] = jsonFormat3(GoodImpl.apply)
  implicit val productFormat: RootJsonFormat[ProductImpl] = jsonFormat3(ProductImpl.apply)
  implicit val offerFormat: RootJsonFormat[OfferImpl] = jsonFormat5(OfferImpl.apply)
  implicit val contextFormat: RootJsonFormat[ContextImpl] = jsonFormat3(ContextImpl.apply)

  /** Parses Moisha's [[ru.yandex.vertis.moisha.model.DateTimeInterval]] json representation in pair of [[DateTime]] */
  def parseInterval(jsInterval: JsValue): (DateTime, DateTime) = {
    val interval = jsInterval.asJsObject
    (DateTimeJsonFormat.read(interval.fields("from")), DateTimeJsonFormat.read(interval.fields("to")))
  }

  /** Custom json reader for [[PointImpl]] because its structure does not matches
    *    to Moisha's [[AutoRuPolicy.AutoRuPoint]] */
  val pointReader: RootJsonReader[PointImpl] = new RootJsonReader[PointImpl] {

    override def read(json: JsValue): PointImpl = json match {
      case JsObject(fields) =>
        val policy: String = StringJsonFormat.read(fields("policy"))
        val (from, to) = parseInterval(fields("interval"))
        val product = productFormat.read(fields("product"))
        PointImpl(policy, from, to, product)
      case other => deserializationError(s"Expected JsObject, but got $other")
    }
  }

  implicit val pointFormat: RootJsonFormat[PointImpl] = lift(pointReader)

  implicit val requestFormat: RootJsonFormat[RequestImpl] = new RootJsonFormat[RequestImpl] {

    override def write(obj: RequestImpl): JsValue =
      JsObject(
        "offer" -> offerFormat.write(obj.offer),
        "context" -> contextFormat.write(obj.context),
        "product" -> JsString(obj.product),
        "interval" -> JsObject(
          "from" -> DateTimeJsonFormat.write(obj.from),
          "to" -> DateTimeJsonFormat.write(obj.to)
        )
      )

    override def read(json: JsValue): RequestImpl = json match {
      case JsObject(fields) =>
        val (from, to) = parseInterval(fields("interval"))
        RequestImpl(
          offerFormat.read(fields("offer")),
          contextFormat.read(fields("context")),
          StringJsonFormat.read(fields("product")),
          from,
          to
        )
      case other =>
        deserializationError("Expected JsObject, but was $other")
    }
  }

  implicit val responseReader: RootJsonReader[ResponseImpl] = new RootJsonReader[ResponseImpl] {

    override def read(json: JsValue): ResponseImpl = json match {
      case JsObject(fields) =>
        val request = requestFormat.read(fields("request"))
        val points = setFormat[PointImpl].read(fields("points")).toSet
        ResponseImpl(request, points)
      case other =>
        deserializationError("Expected JsObject, but was $other")
    }
  }
}
