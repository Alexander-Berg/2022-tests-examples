package ru.yandex.vertis.moisha.impl.autoru_auction

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}

import ru.yandex.vertis.moisha.ProductPolicy
import ru.yandex.vertis.moisha.ProductPolicy.{FromDateTime, RangeDateTime}
import ru.yandex.vertis.moisha.environment._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer
import ru.yandex.vertis.moisha.util.GeoIds.{RegMoscow, RegSPb}

import ru.yandex.vertis.moisha.impl.autoru_auction.AutoRuAuctionPolicy.{AutoRuAuctionRequest, AutoRuAuctionResponse}
import ru.yandex.vertis.moisha.impl.autoru_auction.gens.AutoRuAuctionRequestGen
import ru.yandex.vertis.moisha.impl.autoru_auction.model._

import scala.util.{Failure, Success}

trait SingleProductDailyPolicySpec extends Matchers with WordSpecLike {

  def inMoscow(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(context = request.context.copy(clientRegionId = RegMoscow))

  def inSPb(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(context = request.context.copy(clientRegionId = RegSPb))

  def inRegion(region: RegionId)(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(context = request.context.copy(clientRegionId = region))

  def inCity(city: Option[RegionId])(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(context = request.context.copy(clientCityId = city))

  def withCategory(category: Categories.Value)(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(offer = request.offer.copy(category = category))

  def withSection(section: Sections.Value)(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(offer = request.offer.copy(section = section))

  def withMarks(marks: List[MarkId])(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(context = request.context.copy(marks = marks))

  def withInterval(interval: DateTimeInterval)(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(interval = interval)

  def dailyRequest(request: AutoRuAuctionRequest): AutoRuAuctionRequest =
    request.copy(interval = wholeDay(now()))

  def withProduct(request: AutoRuAuctionRequest, product: Products.Value): AutoRuAuctionRequest =
    request.copy(product = product)

  def policy: ProductPolicy

  def testIterations: Int = 50

  type RequestSpec = AutoRuAuctionRequest => AutoRuAuctionRequest

  def correctInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from)
      case l: RangeDateTime => wholeDay(l.to.minusDays(1))
    }

  def incorrectInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from.minusDays(1))
      case l: RangeDateTime => wholeDay(l.to.plusDays(1))
    }

  def createRequest(
      interval: DateTimeInterval,
      product: Products.Value,
      specs: Iterable[RequestSpec]): AutoRuAuctionRequest = {
    val autoruRequest = withInterval(interval)(
      withProduct(
        AutoRuAuctionRequestGen.next,
        product
      )
    )
    specs.foldLeft(autoruRequest)((r, s) => s(r))
  }

  def checkPolicy(
      requestInterval: DateTimeInterval,
      expectedProduct: AutoRuAuctionProduct,
      requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(requestInterval, expectedProduct.p, requestSpecs)
    policy.estimate(request) match {
      case Success(AutoRuAuctionResponse(`request`, points)) =>
        points.size should be(1)
        points.head.product should be(expectedProduct)
      case other => fail(s"Unexpected $other")
    }
  }

  def checkPolicyEmpty(interval: DateTimeInterval, product: Products.Value, requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(interval, product, requestSpecs)
    policy.estimate(request) match {
      case Success(AutoRuAuctionResponse(`request`, points)) =>
        points.size should be(0)
      case other => fail(s"Unexpected $other")
    }
  }

  def checkPolicyFailure(interval: DateTimeInterval, product: Products.Value, requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(interval, product, requestSpecs)
    policy.estimate(request) match {
      case Failure(e: IllegalArgumentException) =>
      case other => fail(s"Unexpected $other")
    }
  }

  val categoriesWithoutCars: Set[Categories.Value] =
    categoriesWithout(Set(Categories.Cars))

  def categoriesWithout(filter: Set[Categories.Value]): Set[Categories.Value] =
    Categories.values.filterNot(t => filter.contains(t) || filter.contains(Categories.parent(t)))
}
