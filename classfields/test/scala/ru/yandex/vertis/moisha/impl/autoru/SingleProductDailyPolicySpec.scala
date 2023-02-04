package ru.yandex.vertis.moisha.impl.autoru

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.moisha.ProductPolicy
import ru.yandex.vertis.moisha.ProductPolicy.{FromDateTime, RangeDateTime}
import ru.yandex.vertis.moisha.environment._
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.{AutoRuRequest, AutoRuResponse}
import ru.yandex.vertis.moisha.impl.autoru.gens.RequestGen
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer

import scala.util.{Failure, Success}

/**
  * Utilities for AutoRu policies tests
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait SingleProductDailyPolicySpec extends Matchers with WordSpecLike {

  def withPrice(request: AutoRuRequest, offerPrice: Funds): AutoRuRequest =
    request.copy(offer = request.offer.copy(price = offerPrice))

  def inMoscow(request: AutoRuRequest): AutoRuRequest =
    request.copy(context = request.context.copy(clientRegionId = RegMoscow))

  def inSPb(request: AutoRuRequest): AutoRuRequest =
    request.copy(context = request.context.copy(clientRegionId = RegSPb))

  def inRegion(region: RegionId)(request: AutoRuRequest): AutoRuRequest =
    request.copy(context = request.context.copy(clientRegionId = region))

  def inCity(city: Option[RegionId])(request: AutoRuRequest): AutoRuRequest =
    request.copy(context = request.context.copy(clientCityId = city))

  def withOfferCreationTs(request: AutoRuRequest, offerCreationTs: DateTime): AutoRuRequest =
    request.copy(offer = request.offer.copy(creationTs = offerCreationTs))

  def todaysOffer(request: AutoRuRequest): AutoRuRequest =
    request.copy(offer = request.offer.copy(creationTs = request.interval.from.plus(1)))

  def oldOffer(request: AutoRuRequest): AutoRuRequest =
    request.copy(offer = request.offer.copy(creationTs = request.interval.from.minusDays(Gen.oneOf(1, 2, 3).next)))

  def withTransport(transport: Transports.Value)(request: AutoRuRequest): AutoRuRequest =
    request.copy(offer = request.offer.copy(transport = transport))

  def withCategory(category: Categories.Value)(request: AutoRuRequest): AutoRuRequest =
    request.copy(offer = request.offer.copy(category = category))

  def withInterval(interval: DateTimeInterval)(request: AutoRuRequest): AutoRuRequest =
    request.copy(interval = interval)

  def dailyRequest(request: AutoRuRequest): AutoRuRequest =
    request.copy(interval = wholeDay(now()))

  def withProduct(request: AutoRuRequest, product: Products.Value): AutoRuRequest =
    request.copy(product = product)

  def policy: ProductPolicy

  def testIterations: Int = 50

  type RequestSpec = AutoRuRequest => AutoRuRequest

  def correctInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from)
      case l: RangeDateTime if l.to.minusDays(1).isAfter(l.from) => wholeDay(l.to.minusDays(1))
      case l: RangeDateTime => wholeDay(l.from)
    }

  def incorrectInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from.minusDays(1))
      case l: RangeDateTime => wholeDay(l.to.plusDays(1))
    }

  def createRequest(
      interval: DateTimeInterval,
      product: Products.Value,
      offerPrice: Funds,
      specs: Iterable[RequestSpec]): AutoRuRequest = {
    val autoruRequest = withInterval(interval)(withPrice(withProduct(RequestGen.next, product), offerPrice))
    specs.foldLeft(autoruRequest)((r, s) => s(r))
  }

  def priceIn(min: Funds, max: Funds): Gen[Funds] =
    Gen.chooseNum(if (min == 0) 0 else min + 1, max)

  def checkPolicy(
      requestInterval: DateTimeInterval,
      expectedProduct: AutoRuProduct,
      offerPriceGen: Gen[Funds],
      requestSpecs: RequestSpec*): Unit = {
    for (offerPrice <- offerPriceGen.next(testIterations)) {
      val request = createRequest(requestInterval, expectedProduct.p, offerPrice, requestSpecs)
      policy.estimate(request) match {
        case Success(AutoRuResponse(`request`, points)) =>
          points.size should be(1)
          points.head.product should be(expectedProduct)
        case other => fail(s"Unexpected $other")
      }
    }
  }

  def checkPolicyEmpty(
      interval: DateTimeInterval,
      product: Products.Value,
      offerPriceGen: Gen[Funds],
      requestSpecs: RequestSpec*): Unit = {
    for (offerPrice <- offerPriceGen.next(testIterations)) {
      val request = createRequest(interval, product, offerPrice, requestSpecs)
      policy.estimate(request) match {
        case Success(AutoRuResponse(`request`, points)) =>
          points.size should be(0)
        case other => fail(s"Unexpected $other")
      }
    }
  }

  def checkPolicyFailure(
      interval: DateTimeInterval,
      product: Products.Value,
      offerPriceGen: Gen[Funds],
      requestSpecs: RequestSpec*): Unit = {
    for (offerPrice <- offerPriceGen.next(testIterations)) {
      val request = createRequest(interval, product, offerPrice, requestSpecs)
      policy.estimate(request) match {
        case Failure(e: IllegalArgumentException) =>
        case other => fail(s"Unexpected $other")
      }
    }
  }

  def transportsWithoutCars: Set[Transports.Value] =
    transportsWithout(Set(Transports.Cars))

  def transportsWithout(filter: Set[Transports.Value]): Set[Transports.Value] =
    Transports.values.filterNot(t => filter.contains(t) || filter.contains(Transports.parent(t)))
}
