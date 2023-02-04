package ru.yandex.realty.canonical.base

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.canonical.base.params.{Parameter, RequestParameter}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.model.offer.OfferType

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class RequestParameterExtractorSpec extends WordSpec with Matchers {

  "RequestParameterExtractor" should {
    "correctly extract types" in {
      RequestType.values
        .foreach { rt =>
          val r = new Request {
            override def `type`: RequestType.Value = rt

            override def params: Seq[Parameter] = Seq(
              RequestParameter.Rgid(101),
              RequestParameter.Type(OfferType.SELL)
            )
          }
          RequestParameterExtractor.getRequestType(r.key) shouldBe Success(r.`type`)
        }
    }

    "correctly extract rooms" in {
      RequestParameter.RoomsValue.values.toSet
        .subsets()
        .filter(_.nonEmpty)
        .foreach { params =>
          val roomP = RequestParameter.RoomsTotal(params.toSeq: _*)

          val r = new Request {
            override def `type`: RequestType.Value = RequestType.Search

            override def params: Seq[Parameter] = Seq(
              RequestParameter.Rgid(101),
              RequestParameter.Type(OfferType.SELL),
              roomP
            )
          }
          RequestParameterExtractor.extractInKeyRooms(r.key) should contain theSameElementsAs params
        }

    }
  }

}
