package ru.auto.salesman.service.impl.moisha

import ru.auto.salesman.service.PriceExtractor
import ru.auto.salesman.service.impl.moisha.MoishaPriceExtractorSpec.toBytes
import ru.auto.salesman.service.price.PriceExtractorSpec.{Input, InvalidInput, ValidInput}
import ru.auto.salesman.service.price.PriceExtractorSpec

class MoishaPriceExtractorSpec extends PriceExtractorSpec {

  def newExtractor(input: Input*): PriceExtractor =
    if (input.contains(InvalidInput))
      new MoishaPriceExtractor(new Array[Byte](0))
    else
      new MoishaPriceExtractor(toBytes(input.map(_.asInstanceOf[ValidInput])))
}

object MoishaPriceExtractorSpec extends MoishaJsonProtocol {
  import spray.json._

  /** Writes required data from input as Moisha json response */
  implicit private[MoishaPriceExtractorSpec] object ValidInputWriter
      extends RootJsonWriter[ValidInput] {

    def write(obj: ValidInput): JsValue =
      JsObject(
        "interval" -> obj.interval.toJson,
        "product" -> JsObject(
          "product" -> obj.product.toJson,
          "total" -> obj.price.toJson,
          "goods" -> JsArray(
            JsObject(
              "good" -> "Custom".toJson,
              "cost" -> "PerIndexing".toJson,
              "price" -> obj.price.toJson
            )
          )
        )
      )
  }

  private[MoishaPriceExtractorSpec] def toBytes(
      input: Iterable[ValidInput]
  ): Array[Byte] =
    toPoints(input).compactPrint.getBytes

  private[MoishaPriceExtractorSpec] def toPoints(
      input: Iterable[ValidInput]
  ): JsObject =
    JsObject("points" -> JsArray(input.map(_.toJson).toVector))

}
